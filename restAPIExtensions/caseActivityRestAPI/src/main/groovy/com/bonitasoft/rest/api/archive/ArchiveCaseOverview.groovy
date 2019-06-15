package com.bonitasoft.rest.api.archive

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.rest.api.exception.ProcessInstanceNotFoundException
import com.bonitasoft.rest.api.helper.Helper
import com.bonitasoft.rest.api.serializer.ExpenseMapper
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

class ArchiveCaseOverview implements RestApiController, Helper {

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

        try {
            validateCaseAccess(caseId, context);
        } catch (IllegalAccessException e) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_FORBIDDEN,"""{"error" : "You don't have access to this case"}""")
        }

        def processAPI = context.getApiClient().getProcessAPI()
        try {
            def archivedInstance = retrieveArchivedInstance(context, processAPI, caseId as long)
            SimpleBusinessDataReference businessDataRef = processAPI.getArchivedProcessInstanceExecutionContext(archivedInstance.id)['expenseReport_ref']
            def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)

            def body = MAPPER.writeValueAsString(expenseReport)
            return buildResponse(responseBuilder, HttpServletResponse.SC_OK, body)
        } catch (ProcessInstanceNotFoundException e) {
            buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, e.getMessage())
        }
    }
}
