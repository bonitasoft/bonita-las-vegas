package com.bonitasoft.rest.api.cases

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.search.SearchOptions
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor
import com.bonitasoft.rest.api.helper.Helper
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

/**
 * This API returns all the active instances of the expense report process started by the calling user.
 */
class Case implements RestApiController, Helper {

    public static final String EXPENSE_REPORT_PROCESS_NAME = "Expense Report"
    public static final String EXPENSE_REPORT_PROCESS_VERSION = "1.0"

    private static final Logger LOGGER = LoggerFactory.getLogger(Case.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def contextPath = request.contextPath
        def processAPI = context.apiClient.getProcessAPI()
        def userId = context.apiSession.userId

        def appToken = request.getParameter "appToken"
        def p = request.getParameter "p"
        def c = request.getParameter "c"
        if(!appToken) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `appToken` is mandatory")
        }
        if(!p) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `p` is mandatory")
        }
        if(!c) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `c` is mandatory")
        }

        def searchOptions = buildSearchOptions(p as int, c as int, userId)
        def searchResults = retrieveResults(processAPI, searchOptions)
        def instances = retrieveProcessInstance(context, processAPI, contextPath, appToken, searchResults)

        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(instances).toString(), p as int, c as int, searchResults.count)
    }

    def retrieveProcessInstance(RestAPIContext context,  ProcessAPI processAPI, String contextPath, String appToken, SearchResult searchResult) {
        def result = searchResult.getResult()
                .collect {
                    SimpleBusinessDataReference businessDataRef = processAPI.getProcessInstanceExecutionContext(it.id)['expenseReport_ref']
                    def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)
                    [
                        id: it.id,
                        name: expenseReport.expenseHeader.description ?: "New expense report",
                        state: computeState(it, processAPI),
                        viewAction: viewActionLink(it.id, contextPath, appToken)
                    ]
                }
        result
    }

    SearchResult retrieveResults(ProcessAPI processAPI, SearchOptions searchOptions) {
        processAPI.searchProcessInstances(searchOptions)
    }

    def buildSearchOptions(int p, int c, Long userId) {
        return new SearchOptionsBuilder(p * c, c).with {
            filter(ProcessInstanceSearchDescriptor.STARTED_BY, userId)
            filter(ProcessInstanceSearchDescriptor.NAME, EXPENSE_REPORT_PROCESS_NAME)
            done()
        }
    }

    def asLabel(state, style) {
        """<span class="label label-$style">$state</span>"""
    }

    def String openTasksCount(long caseId, ProcessAPI processAPI, contextPath) {
        searchOpenedTasks(caseId, processAPI).result
                .findAll {
                    canExecute(getState(it, processAPI).name)
                }
                .size()
    }

    def String viewActionLink(long caseId, contextPath, String appToken) {
        return """<a class="btn btn-primary btn-sm" href="$contextPath/apps/$appToken/case?id=$caseId" target="_top">Open</a>"""
    }

    def  String computeState(ProcessInstance instance, ProcessAPI processAPI) {
        def tasks = searchOpenedTasks(instance.id, processAPI).getResult()
        if (tasks.any { Objects.equals(it.name, "Manager validation") }) {
            return asLabel("MANAGER VALIDATION", "warning")
        }
        if (tasks.any { Objects.equals(it.name, "Accounting validation") }) {
            return asLabel("ACCOUNTING VALIDATION", "danger")
        }
        if (tasks.any { Objects.equals(it.name, "Payment") }) {
            return asLabel("WAITING FOR PAYMENT", "success")
        }

        asLabel(instance.state.toUpperCase(), "info");
    }
}
