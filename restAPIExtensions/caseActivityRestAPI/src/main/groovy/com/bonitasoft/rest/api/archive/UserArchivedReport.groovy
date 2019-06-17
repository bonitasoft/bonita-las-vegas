package com.bonitasoft.rest.api.archive

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.search.SearchOptions
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult

import com.bonita.lr.model.ExpenseReport
import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor
import com.bonitasoft.rest.api.cases.Case
import com.bonitasoft.web.extension.rest.RestAPIContext

/**
 * This API returns all the archived instances of the expense report process started by the calling user.
 */
class UserArchivedReport extends Case {

    @Override
    def retrieveProcessInstance(RestAPIContext context, ProcessAPI processAPI, String contextPath, String appToken, SearchResult searchResult) {
        def result = searchResult.getResult()
                .collect {
                    SimpleBusinessDataReference businessDataRef = processAPI.getArchivedProcessInstanceExecutionContext(it.id)['expenseReport_ref']
                    def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)
                    format(context, contextPath, appToken, it, expenseReport)
                }
                .sort { report1, report2 -> LocalDate.parse(report2.date) <=> LocalDate.parse(report1.date) }
        result
    }

    @Override
    SearchResult retrieveResults(ProcessAPI processAPI, SearchOptions searchOptions) {
        processAPI.searchArchivedProcessInstances(searchOptions)
    }

    @Override
    def buildSearchOptions(int p, int c, Long userId) {
        return new SearchOptionsBuilder(p * c, c).with {
            filter(ArchivedProcessInstancesSearchDescriptor.STARTED_BY, userId)
            filter(ArchivedProcessInstancesSearchDescriptor.NAME, EXPENSE_REPORT_PROCESS_NAME)
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
