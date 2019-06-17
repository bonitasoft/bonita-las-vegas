package com.bonitasoft.rest.api.cases;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskDefinition
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.StandardLoopCharacteristics
import org.bonitasoft.engine.bpm.flownode.UserTaskInstance
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.rest.api.helper.Helper
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

/**
 * Return all the tasks available, with their status, for a given case (url parameter)
 */
class CaseTask implements RestApiController, Helper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseTask.class)

    private static final String ADD_EXPENSE_TASK_NAME = "Add Expense"

    //Tasks we do not want to display in the `available actions` section -> used elsewhere
    private static final String[] UNWANTED_TASKS = [
        "Edit Expense",
        "Delete Expense",
        "Edit expense summary"
    ]

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def contextPath = "http://$request.serverName:$request.localPort$request.contextPath"
        def caseId = request.getParameter "caseId"
        def appToken = request.getParameter "appToken"
        if (!caseId) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter caseId is missing"}""")
        }
        if(!appToken) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `appToken` is mandatory")
        }
        try {
            validateCaseAccess(caseId, context);
        } catch (IllegalAccessException e) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_FORBIDDEN,"""{"error" : "You don't have access to this case"}""")
        }

        def ProcessAPI processAPI = context.apiClient.getProcessAPI()
        def pDef = -1;
        try {
            pDef = processAPI.getProcessDefinition(processAPI.getProcessDefinitionIdFromProcessInstanceId(caseId.toLong()))
        } catch (ProcessDefinitionNotFoundException e) {
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
                .findAll{ it.parentProcessInstanceId ==  caseId.toLong() }
                .findAll { !UNWANTED_TASKS.contains(it.name) }
                .collect{ HumanTaskInstance task ->
                    def metadata = getMetadata(task,processAPI)
                    [
                        id:task.name,
                        name:task.displayName ?: task.name,
                        url:forge(pDef.name,pDef.version,task,contextPath,metadata),
                        description:task.description,
                        target:"_self",
                        state:task.state.capitalize(),
                        metadata:metadata
                    ]
                }
        // Not a BPM task but we want to display the add document action as if it was one, with an availability state linked to the Add expense task
        def addExpenseTask = result.find { it.id == ADD_EXPENSE_TASK_NAME }
        def documentTaskState = "Not available"
        if (addExpenseTask) {
            documentTaskState = addExpenseTask.metadata.$activityState == "Required"
                    ? "Available"
                    : addExpenseTask.metadata.$activityState
        }
        result.add(addDocumentTasks(contextPath, appToken, caseId, documentTaskState))

        result = result.sort{ t1,t2 -> idOfState(t1.metadata.$activityState) <=> idOfState(t2.metadata.$activityState) }

        //Retrieve finished activities
        processAPI.searchArchivedHumanTasks(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ArchivedActivityInstanceSearchDescriptor.ASSIGNEE_ID, context.apiSession.userId)
            filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, caseId)
            done()
        }).getResult()
        .findAll { !UNWANTED_TASKS.contains(it.name) }
        .findAll { !loopTasks.contains(it.name) && !result.collect{ task -> task["id"] }.contains(it.name) }
        .collect{ ArchivedHumanTaskInstance task ->
            [
                id:task.sourceObjectId,
                name:task.displayName ?: task.name,
                description:task.description,
                state:task.state.capitalize()
            ]
        }
        .unique { taskA, taskB -> taskA.name <=> taskB.name } // If a given task has been done a couple of time, we only display one archive instance -> we do not need more in this use case
        .each { result.add(it) }

        buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
    }

    def String forge(String processName, String processVersion, ActivityInstance instance, contextPath, metadata) {
        if(instance instanceof UserTaskInstance) {
            canExecute(metadata.$activityState)
                    ? "$contextPath/portal/resource/taskInstance/$processName/$processVersion/$instance.name/content/?id=$instance.id&displayConfirmation=false"
                    : ""
        }
    }

    def addDocumentTasks(contextPath, appToken, caseId, state) {
        [
            id:"addDocument",
            name:"📜 Add document",
            url: canExecute(state) ? "$contextPath/apps/$appToken/document?id=$caseId" : "",
            description:"Add a document related to an expense",
            target:"_top",
            state:state,
            metadata:['$activityState':state]
        ]
    }

}
