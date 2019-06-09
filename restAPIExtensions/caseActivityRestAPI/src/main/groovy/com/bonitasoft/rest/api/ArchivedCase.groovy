package com.bonitasoft.rest.api

import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.search.SearchOptionsBuilder

import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor
import com.bonitasoft.web.extension.rest.RestAPIContext

/**
 * This API returns all the archived instances of the expense report process started by the calling user.
 */
class ArchivedCase extends Case {

    /**
     * @param processAPI
     * @param process
     * @param contextPath
     * @return All archived instances of the process in input
     */
    @Override
    def retrieveProcessInstance(RestAPIContext context, ProcessAPI processAPI, ProcessDeploymentInfo process, String contextPath, Long userId, String appToken) {
        def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ArchivedProcessInstancesSearchDescriptor.PROCESS_DEFINITION_ID, process.processId)
            filter(ArchivedProcessInstancesSearchDescriptor.STARTED_BY, userId)
            filter(ArchivedProcessInstancesSearchDescriptor.NAME, "Expense report")
            done()
        }
        def result = processAPI.searchArchivedProcessInstances(searchOptions).getResult()
                .collect {
                    SimpleBusinessDataReference businessDataRef = processAPI.getArchivedProcessInstanceExecutionContext(it.id)['expenseReport_ref']
                    def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)
                    [
                        id: it.sourceObjectId,
                        name: expenseReport.expenseHeader.description ?: "New expense report",
                        state: asLabel(expenseReport.isReportAccepted() ? "Accepted" : "Refused", expenseReport.isReportAccepted() ? "success" : "danger")
                        // TODO a link to see the an overview of the archived process
                    ]
                }
        result
    }
}
