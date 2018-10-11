package com.bonitasoft.rest.api;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.data.DataNotFoundException
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.HumanTaskDefinition
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

class CaseActivity implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseActivity.class)
	private static final String ACTIVITY_CONTAINER = "Dymanic Activity Container"
	private static final String CREATE_ACTIVITY = "Create Activity"

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
		def loopTasks = designProcessDefinition.getFlowElementContainer().getActivities().findAll{
			it instanceof HumanTaskDefinition && it.getLoopCharacteristics() instanceof StandardLoopCharacteristics
		}.collect{
			it.name
		}
		def result = []
		//Retrieve pending activities
		CaseActivityHelper.searchOpenedTasks(caseId, processAPI).getResult().collect{
			def state = CaseActivityHelper.getState(it,processAPI)
			if(CaseActivityHelper.canExecute(state)) {
				result << [id:it.name,name:it.displayName,state:state,url:forge(pDef.name,pDef.version,it,contextPath),description:it.description,target:linkTarget(it)]
			}else {
				result << [id:it.name,name:it.displayName,state:state,description:it.description]
			}
		}
		//Retrieve finished activities
		processAPI.searchArchivedHumanTasks(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
			filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, caseId)
			differentFrom(ArchivedActivityInstanceSearchDescriptor.NAME, ACTIVITY_CONTAINER)
			and()
			differentFrom(ArchivedActivityInstanceSearchDescriptor.NAME, CREATE_ACTIVITY)
			done()
		}).getResult().collect{
			if(!loopTasks.contains(it.name)) {
				result << [id:it.name,name:it.displayName,state:[name:it.state,id:CaseActivityHelper.idOfState(it.state)],description:it.description]
			}
		}
		
		//Retrieve N/A activities
		designProcessDefinition.getFlowElementContainer().getActivities().findAll{
			it instanceof HumanTaskDefinition && !result.id.contains(it.name) && !ACTIVITY_CONTAINER.equals(it.name) && !CREATE_ACTIVITY.equals(it.name)
		}.collect{
			result << [id:it.name,name:it.name,state:[name:"N/A",id:CaseActivityHelper.idOfState("N/A")],description:it.description]
		}
		
        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result.sort {a1,a2 ->
			 a1.state.id <=> a2.state.id
		}).toString())
    }
	
	
	def String forge(String processName,String processVersion,ActivityInstance instance,contextPath) {
		if(instance instanceof UserTaskInstance) {
			"$contextPath/portal/resource/taskInstance/$processName/$processVersion/$instance.name/content/?id=$instance.id&displayConfirmation=false"
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
