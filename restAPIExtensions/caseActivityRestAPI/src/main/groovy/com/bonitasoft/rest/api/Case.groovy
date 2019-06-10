package com.bonitasoft.rest.api

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

/**
 * This API returns all the active instances of the expense report process started by the calling user.
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

        def appToken = request.getParameter("appToken")
        if(!appToken) {
            return responseBuilder.with {
                withResponseStatus(HttpServletResponse.SC_BAD_REQUEST)
                withResponse("Parameter `appToken` is mandatory")
                build()
            }
        }

        def expenseReportProcessDef = retrieveProcess(processAPI, EXPENSE_REPORT_PROCESS_NAME, EXPENSE_REPORT_PROCESS_VERSION)
        def instances = retrieveProcessInstance(context, processAPI, expenseReportProcessDef, contextPath, userId, appToken)

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
     * @return All active instances of the process in input
     */
    def retrieveProcessInstance(RestAPIContext context, ProcessAPI processAPI, ProcessDeploymentInfo process, String contextPath, Long userId, String appToken) {
        def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, process.processId)
            filter(ProcessInstanceSearchDescriptor.STARTED_BY, userId)
            filter(ProcessInstanceSearchDescriptor.NAME, EXPENSE_REPORT_PROCESS_NAME)
            done()
        }
        def result = processAPI.searchProcessInstances(searchOptions).getResult()
                .collect {
                    SimpleBusinessDataReference businessDataRef = processAPI.getProcessInstanceExecutionContext(it.id)['expenseReport_ref']
                    def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)
                    [
                        id: it.id,
                        name: expenseReport.expenseHeader.description ?: "New expense report",
                        state: computeState(it, processAPI),
                        viewAction: viewActionLink(it.id, processAPI, contextPath, appToken)
                    ]
                }
        result
    }

    def asLabel(state, style) {
        """<span class="label label-$style">$state</span>"""
    }

    def String openTasksCount(long caseId, ProcessAPI processAPI, contextPath) {
        searchOpenedTasks(caseId, processAPI).result
                .findAll { canExecute(getState(it, processAPI).name) }
                .size()
    }

    def String viewActionLink(long caseId, ProcessAPI processAPI, contextPath, String appToken) {
        def tasksToExclude = ["Manager validation"]
        def openTasks = searchOpenedTasks(caseId, processAPI).getResult()
                .findAll { canExecute(getState(it, processAPI).name) }
                .findAll { !tasksToExclude.contains(it.name) }
        return """<a class="btn btn-primary btn-sm" href="$contextPath/apps/$appToken/case?id=$caseId" target="_top">Open <span class="badge"> $openTasks.size</span></a>"""
    }

    def String computeState(ProcessInstance instance, ProcessAPI processAPI) {
        // Check manager validation task
        def options = new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, instance.id)
            filter(ActivityInstanceSearchDescriptor.NAME, "Manager validation")
            done()
        }
        if (!processAPI.searchHumanTaskInstances(options).getResult().isEmpty()) {
            return asLabel("WAITING FOR MANAGER VALIDATION", "warning")
        }

        // Check accounting validation task
        options = new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, instance.id)
            filter(ActivityInstanceSearchDescriptor.NAME, "Accounting validation")
            done()
        }
        if (!processAPI.searchHumanTaskInstances(options).getResult().isEmpty()) {
            return asLabel("WAITING FOR ACCOUNTING VALIDATION", "danger")
        }

        asLabel(instance.state.toUpperCase(), "info")
    }
}
