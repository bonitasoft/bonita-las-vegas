package com.bonitasoft.rest.api;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskDefinition
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ManualTaskInstance
import org.bonitasoft.engine.bpm.flownode.StandardLoopCharacteristics
import org.bonitasoft.engine.bpm.flownode.UserTaskInstance
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class CaseActivity implements RestApiController,CaseActivityHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseActivity.class)
    private static final String ACTIVITY_CONTAINER = "Dymanic Activity Container"
    private static final String CREATE_ACTIVITY = "Create Activity"
    private static final String PREFIX = '$'

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def contextPath = "http://$request.serverName:$request.localPort$request.contextPath"
        def caseId = request.getParameter "caseId"
        if (!caseId) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter caseId is missing"}""")
        }

        def ProcessAPI processAPI = context.apiClient.getProcessAPI()
        def pDef = -1;
        try {
            pDef = processAPI.getProcessDefinition(processAPI.getProcessDefinitionIdFromProcessInstanceId(caseId.toLong()))
        }catch(ProcessDefinitionNotFoundException e) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_NOT_FOUND,"""{"error" : "no process definition found for instance $caseId" }""")
        }

        def designProcessDefinition = processAPI.getDesignProcessDefinition(pDef.id)
        def loopTasks = designProcessDefinition
                .getFlowElementContainer()
                .getActivities()
                .findAll{
                    it instanceof HumanTaskDefinition && it.getLoopCharacteristics() instanceof StandardLoopCharacteristics
                }.collect{ it.name }

        //Retrieve pending activities
        def result = processAPI.getPendingHumanTaskInstances(context.apiSession.userId,0, Integer.MAX_VALUE, ActivityInstanceCriterion.EXPECTED_END_DATE_ASC)
                .findAll{ it.name != ACTIVITY_CONTAINER && it.name != CREATE_ACTIVITY && it.parentProcessInstanceId ==  caseId.toLong()}
                .collect{ HumanTaskInstance task ->
                    def metadata = getMetadata(task,processAPI)
                    [
                        id:task.name,
                        name:task.displayName ?: task.name,
                        url:forge(pDef.name,pDef.version,task,contextPath,metadata),
                        description:task.description,
                        target:linkTarget(task),
                        state:task.state.capitalize(),
                        metadata:metadata
                    ]
                }

        def containerInstance = processAPI.searchHumanTaskInstances(new SearchOptionsBuilder(0, 1)
                .filter(HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID, caseId.toLong())
                .filter(HumanTaskInstanceSearchDescriptor.NAME, ACTIVITY_CONTAINER)
                .done()).result[0]

        //Retrieve adhoc activities
        result.addAll(processAPI.searchHumanTaskInstances(new SearchOptionsBuilder(0, Integer.MAX_VALUE)
                .filter(HumanTaskInstanceSearchDescriptor.PARENT_CONTAINER_ID, containerInstance.id)
                .done())
                .result
                .collect{ HumanTaskInstance task ->
                    def metadata = getMetadata(task,processAPI)
                    [
                        id:task.name,
                        name:task.displayName ?: task.name,
                        url:forge(pDef.name,pDef.version,task,contextPath, metadata),
                        description:task.description,
                        target:linkTarget(task),
                        state:task.state.capitalize(),
                        metadata:metadata
                    ]
                })

        result = result.sort{ t1,t2 -> valueOf(t1.metadata.$activityState) <=> valueOf(t2.metadata.$activityState) }

        //Retrieve finished activities
        result.addAll(processAPI.searchArchivedHumanTasks(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, caseId)
            differentFrom(ArchivedActivityInstanceSearchDescriptor.NAME, ACTIVITY_CONTAINER)
            and()
            differentFrom(ArchivedActivityInstanceSearchDescriptor.NAME, CREATE_ACTIVITY)
            done()
        }).getResult()
        .findAll{
            !loopTasks.contains(it.name) }
        .collect{ ArchivedHumanTaskInstance task ->
            [
                id:task.sourceObjectId,
                name:task.displayName ?: task.name,
                description:task.description,
                state:task.state.capitalize()
            ]
        })

        buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
    }

    def valueOf(state) {
        switch(state) {
            case 'Required' : return 0
            case 'Optional' : return 1
            case 'Discretionary' : return 2
            case 'N/A' : return 3
            default : return 4
        }
    }

    def getMetadata(HumanTaskInstance task, ProcessAPI processAPI) {
        def res = [:]
        if(task instanceof ManualTaskInstance) {
            res.put('$activityState', 'Optional')
        }else {
            processAPI.getActivityTransientDataInstances(task.id, 0, Integer.MAX_VALUE)
                    .findAll{ it.name.startsWith(PREFIX) }
                    .collect{ res.put(it.name,it.value) }
        }
        return res
    }


    def String forge(String processName, String processVersion, ActivityInstance instance, contextPath, metadata) {
        if(instance instanceof UserTaskInstance) {
            canExecute(metadata.$activityState)
                    ? "$contextPath/portal/resource/taskInstance/$processName/$processVersion/$instance.name/content/?id=$instance.id&displayConfirmation=false"
                    : ""
        }else if(instance instanceof ManualTaskInstance) {
            "$contextPath/apps/cases/do?id=$instance.id"
        }
    }

    def String linkTarget(ActivityInstance instance) {
        if(instance instanceof UserTaskInstance) {
            "_self"
        }else if(instance instanceof ManualTaskInstance) {
            "_parent"
        }
    }

    /**
     * Build an HTTP response.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  httpStatus the status of the response
     * @param  body the response body
     * @return a RestAPIResponse
     */
    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }


}
