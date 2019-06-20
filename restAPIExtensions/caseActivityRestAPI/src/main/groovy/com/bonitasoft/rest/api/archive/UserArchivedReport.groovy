package com.bonitasoft.rest.api.archive

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.search.SearchOptions
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonita.lr.model.ExpenseReport
import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor
import com.bonitasoft.rest.api.cases.Case
import com.bonitasoft.web.extension.rest.RestAPIContext

import groovy.json.JsonBuilder

/**
 * This API returns all the archived instances of the expense report process started by the calling user.
 */
class UserArchivedReport extends Case {

    public static final String ACCEPTED = "accepted"
    public static final String REFUSED = "refused"
    public static final String ALL = "all"

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def contextPath = request.contextPath
        def processAPI = context.apiClient.getProcessAPI()
        def userId = context.apiSession.userId

        def appToken = request.getParameter "appToken"
        def type = request.getParameter "type"
        def p = request.getParameter "p"
        def c = request.getParameter "c"
        if(!appToken) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `appToken` is mandatory")
        }
        if(!type) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `type` is mandatory")
        }
        if(type != ACCEPTED && type != REFUSED && type != ALL) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `type` must be equals to `all`, `accepted` or `refused`")
        }
        if(!p) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `p` is mandatory")
        }
        if(!c) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `c` is mandatory")
        }

        def searchOptions = buildSearchOptions(p as int, c as int, userId)
        def searchResults = retrieveResults(processAPI, searchOptions)
        def instances = retrieveProcessInstance(context, processAPI, contextPath, appToken, searchResults, type)

        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(instances).toString(), p as int, c as int, searchResults.count)
    }

    def retrieveProcessInstance(RestAPIContext context, ProcessAPI processAPI, String contextPath, String appToken, SearchResult searchResult, String type) {
        def accepted = type == ACCEPTED
        def result = searchResult.getResult()
                .collect {
                    SimpleBusinessDataReference businessDataRef = processAPI.getArchivedProcessInstanceExecutionContext(it.id)['expenseReport_ref']
                    def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)
                    if(type == ALL || expenseReport.reportAccepted == accepted) {
                        return format(context, contextPath, appToken, it, expenseReport)
                    }
                }
                .findAll()
                .sort { report1, report2 -> LocalDate.parse(report2.date) <=> LocalDate.parse(report1.date) }
        result
    }

    @Override
    SearchResult retrieveResults(ProcessAPI processAPI, SearchOptions searchOptions) {
        processAPI.searchArchivedProcessInstances(searchOptions)
    }

    @Override
    def buildSearchOptions(int p, int c, Long userId) {
        ArchivedProcessInstance a;
        return new SearchOptionsBuilder(p * c, c).with {
            filter(ArchivedProcessInstancesSearchDescriptor.STARTED_BY, userId)
            filter(ArchivedProcessInstancesSearchDescriptor.NAME, EXPENSE_REPORT_PROCESS_NAME)
            differentFrom(ArchivedProcessInstancesSearchDescriptor.STATE_ID, 3) // Cancel state id
            done()
        }
    }

    def format(RestAPIContext context, String contextPath, String appToken, ArchivedProcessInstance instance, ExpenseReport report) {
        return [
            id: instance.id,
            user: user(context, instance),
            name: report.expenseHeader.description ?: "New expense report",
            state: asLabel(report.isReportAccepted() ? "Accepted" : "Refused", report.isReportAccepted() ? "success" : "danger"),
            date: report.closeDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            viewAction: viewActionLink(instance.id, contextPath, appToken)
        ]
    }

    @Override
    def String viewActionLink(long caseId, contextPath, String appToken) {
        return """<a href="$contextPath/apps/$appToken/archivedCase?id=$caseId" target="_top"><i class=\"glyphicon glyphicon-play\"></i></a>"""
    }
}
