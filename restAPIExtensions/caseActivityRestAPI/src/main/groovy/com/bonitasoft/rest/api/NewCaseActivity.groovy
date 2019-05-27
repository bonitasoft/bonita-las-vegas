package com.bonitasoft.rest.api;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.FlowNodeType
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.bpm.flownode.ManualTaskCreator
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class NewCaseActivity implements RestApiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(NewCaseActivity.class)
	private static final String ACTIVITY_CONTAINER = "Dymanic Activity Container"
	private static final String CREATE_ACTIVITY = "Create Activity"
	
	@Override
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		def jsonBody = new JsonSlurper().parse(request.getReader())
		def processAPI = context.apiClient.getProcessAPI()

		def res = processAPI.searchActivities(new SearchOptionsBuilder(0, 1).with {
			filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, jsonBody.caseId)
			filter(ActivityInstanceSearchDescriptor.NAME, ACTIVITY_CONTAINER)
			done()
		}).getResult()
	
		if(res.isEmpty()) {
			return responseBuilder.with {
				withResponseStatus(HttpServletResponse.SC_NOT_FOUND)
				withResponse("No Dymanic Activity Container found")
				build()
			}
		}
		def containerId = res.get(0).id;
		processAPI.assignUserTask(containerId, context.apiSession.userId)
		def task = processAPI
				.addManualUserTask(new ManualTaskCreator(containerId, jsonBody.name).with{
					setDisplayName( "&#x2795 $jsonBody.name" )
					setDescription(jsonBody.description)
					setAssignTo(context.apiSession.userId)
					setDueDate(null)
				})
		
				
	    res = processAPI.searchActivities(new SearchOptionsBuilder(0, 1).with {
					filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, jsonBody.caseId)
					filter(ActivityInstanceSearchDescriptor.NAME, CREATE_ACTIVITY)
					filter(ActivityInstanceSearchDescriptor.ACTIVITY_TYPE, FlowNodeType.USER_TASK)
					done()
				}).getResult()
		if(res.isEmpty()) {
			return responseBuilder.with {
				withResponseStatus(HttpServletResponse.SC_NOT_FOUND)
				withResponse("No Create Activity found")
				build()
			}
		}
		def createActivityTaskId = res[0].id;
		processAPI.assignUserTask(createActivityTaskId, context.apiSession.userId)
		processAPI.executeUserTask(createActivityTaskId, [name:jsonBody.name])
		
		return responseBuilder.with {
			withResponse(new JsonBuilder([name:jsonBody.name]).toString())
			withResponseStatus(HttpServletResponse.SC_CREATED)
			build()
		}
	}
	
}
