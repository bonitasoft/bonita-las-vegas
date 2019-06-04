package com.bonitasoft.rest.api

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

/**
 * This API returns all the instances (active or archived) of the expense report process started by the calling user.
 */
class Case implements RestApiController, CaseActivityHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(Case.class)
	public static final String EXPENSE_REPORT_PROCESS_NAME = "Expense Report"
	public static final String EXPENSE_REPORT_PROCESS_VERSION = "1.0"

	@Override 
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		def contextPath = request.contextPath
		def processAPI = context.apiClient.getProcessAPI()
		def userId = context.apiSession.userId
		def expenseReportProcessDef = retrieveProcess(processAPI, EXPENSE_REPORT_PROCESS_NAME, EXPENSE_REPORT_PROCESS_VERSION)
		def instances = retrieveProcessInstance(processAPI, expenseReportProcessDef, contextPath, userId)
		
		
		return responseBuilder.with {
			withResponseStatus(HttpServletResponse.SC_OK)
			withResponse(new JsonBuilder(instances).toString())
			build()
		}
	}

	/**
	 * @param processAPI
	 * @param process
	 * @param contextPath
	 * @return All instances (active or archived) of the process in input
	 */
	def retrieveProcessInstance(ProcessAPI processAPI, ProcessDeploymentInfo process, String contextPath, Long userId) {
		def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
			filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, process.processId)
			filter(ProcessInstanceSearchDescriptor.STARTED_BY, userId)
			done()
		}
		def result = processAPI.searchProcessInstances(searchOptions).getResult()
				.collect {
					[id: it.id, name: process.name, state: asLabel(it.state.toUpperCase(), "info"), viewAction: viewActionLink(it.id, processAPI, contextPath)]
				}
		searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
			filter(ArchivedProcessInstancesSearchDescriptor.PROCESS_DEFINITION_ID, process.processId)
			filter(ArchivedProcessInstancesSearchDescriptor.STARTED_BY, userId)
			done()
		}
		processAPI.searchArchivedProcessInstances(searchOptions).getResult()
				.collect {
					result << [id: it.sourceObjectId, name: process.name, state: asLabel(it.state.toUpperCase(), "default"), viewAction: viewActionLink(it.sourceObjectId, processAPI, contextPath)]
				}
		result
	}
	
	def asLabel(state, style) {
		"""<span class="label label-$style">$state</span>"""
	}
	
	def String viewActionLink(long caseId, ProcessAPI processAPI, contextPath) {
		def openTasks = searchOpenedTasks(caseId, processAPI).result
				.findAll { canExecute(getState(it, processAPI).name) }
		if (openTasks.size() > 0) {
			return """<a class="btn btn-primary btn-sm" href="$contextPath/apps/expenseReportEmployee/case?id=$caseId" target="_target">Open <span class="badge"> $openTasks.size</span></a>"""
		} else {
			return ""
		}
	}

}
