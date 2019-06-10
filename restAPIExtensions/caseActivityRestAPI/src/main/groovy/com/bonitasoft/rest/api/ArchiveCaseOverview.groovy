package com.bonitasoft.rest.api

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.rest.api.serializer.ExpenseMapper
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

class ArchiveCaseOverview implements RestApiController , CaseActivityHelper{

    private static final ExpenseMapper MAPPER = new ExpenseMapper()

    @Override
    public RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder,
            RestAPIContext context) {

        def caseId = request.getParameter("caseId")

        if (!caseId) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `caseId` is mandatory")
        }
        if (!caseId.isLong()) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `caseId` must be a Long")
        }

        // TODO: validate doesn't look for archived cases
        //        try {
        //            validateCaseAccess(caseId, context);
        //        } catch (IllegalStateException e) {
        //            return buildResponse(responseBuilder, HttpServletResponse.SC_FORBIDDEN,"""{"error" : "You don't have access to this case"}""")
        //        }

        def processAPI = context.getApiClient().getProcessAPI()
        def archivedInstance = retrieveArchivedInstance(context, processAPI, caseId as long)
        if (archivedInstance) {
            SimpleBusinessDataReference businessDataRef = processAPI.getArchivedProcessInstanceExecutionContext(archivedInstance.id)['expenseReport_ref']
            def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)

            def body = MAPPER.writeValueAsString(expenseReport)
            return buildResponse(responseBuilder, HttpServletResponse.SC_OK, body)
        }

        buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "The archived case $caseId has not been found")
    }

    ArchivedProcessInstance retrieveArchivedInstance(RestAPIContext context, ProcessAPI processAPI, long caseId) {
        def options = new SearchOptionsBuilder(0, 1).with {
            filter(ArchivedProcessInstancesSearchDescriptor.ID, caseId)
            done()
        }
        def results = processAPI.searchArchivedProcessInstances(options).getResult()
        if (!results.isEmpty()) {
            return results[0]
        }
    }

    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }
}
