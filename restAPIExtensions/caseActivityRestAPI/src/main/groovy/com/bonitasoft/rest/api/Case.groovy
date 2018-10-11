package com.bonitasoft.rest.api;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.data.DataNotFoundException
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class Case implements RestApiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(Case.class)

	@Override
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		def contextPath = "http://localhost:$request.localPort$request.contextPath"
		def processAPI = context.apiClient.getProcessAPI()
		def result = processAPI.searchProcessInstances(new SearchOptionsBuilder(0, 9999).with {
			done()
		}).getResult()
		.collect{
			[id:it.id,name:processName(it.processDefinitionId,processAPI),state:asLabel(it.state.toUpperCase(),"info"),viewAction:viewActionLink(it.id,processAPI,contextPath)]
		}
	
		processAPI.searchArchivedProcessInstances(new SearchOptionsBuilder(0, 9999).with {
			done()
		}).getResult()
		.collect{
			result << [id:it.sourceObjectId,name:processName(it.processDefinitionId,processAPI),state:asLabel(it.state.toUpperCase(),"default"),viewAction:viewActionLink(it.sourceObjectId,processAPI,contextPath)]
		}
		
		return responseBuilder.with {
			withResponseStatus(HttpServletResponse.SC_OK)
			withResponse(new JsonBuilder(result).toString())
			build()
		}
	}
	def asLabel(state,style) {
		"""<span class="label label-$style">$state</span>"""
	}
	
	def String processName(long processDefId,ProcessAPI processAPI) {
		def definition = processAPI.getProcessDefinition(processDefId)
		return "$definition.name ($definition.version)"
	}

	def String viewActionLink(long caseId,ProcessAPI processAPI,contextPath) {
		def openTasks = CaseActivityHelper.searchOpenedTasks(caseId,processAPI).result
		.findAll{CaseActivityHelper.canExecute(CaseActivityHelper.getState(it,processAPI))}
		if(openTasks.size() > 0) {
			return """<a class="btn btn-primary btn-sm" href="$contextPath/apps/cases/case?id=$caseId" target="_target">Open <span class="badge"> $openTasks.size</span></a>"""
		}else {
			return ""
		}
	}
	

}
