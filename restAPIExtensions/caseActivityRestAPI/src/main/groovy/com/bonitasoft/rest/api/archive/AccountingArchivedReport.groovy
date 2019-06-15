package com.bonitasoft.rest.api.archive

import java.time.LocalDate
import java.time.ZoneOffset

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonitasoft.web.extension.rest.RestAPIContext

import groovy.json.JsonBuilder

class AccountingArchivedReport extends UserArchivedReport {

    private static final int MAX_DATE_LENGTH = 10

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def contextPath = request.contextPath
        def processAPI = context.apiClient.getProcessAPI()

        def appToken = request.getParameter("appToken")
        def startDate = request.getParameter("startDate")
        def endDate = request.getParameter("endDate")
        def p = request.getParameter "p"
        def c = request.getParameter "c"

        if(!appToken) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `appToken` is mandatory")
        }
        if(!startDate) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `starDate` is mandatory")
        }
        if(!endDate) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `endDate` is mandatory")
        }
        if(!p) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `p` is mandatory")
        }
        if(!endDate) {
            c buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `c` is mandatory")
        }

        def searchOptions = buildSearchOptions(p as int, c as int, startDate, endDate)
        def searchResults = processAPI.searchArchivedProcessInstances(searchOptions)
        def instances = retrieveProcessInstance(context, processAPI, contextPath, appToken, searchResults)
        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(instances).toString(), p as int, c as int, searchResults.count)
    }

    def buildSearchOptions(int p, int c, String startDate, String endDate) {
        def startDateValue = LocalDate.parse(startDate.substring(0, MAX_DATE_LENGTH)).atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli()
        def endDateValue = LocalDate.parse(endDate.substring(0, MAX_DATE_LENGTH)).atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli()
        new SearchOptionsBuilder(p * c, c).with {
            filter(ArchivedProcessInstancesSearchDescriptor.NAME, EXPENSE_REPORT_PROCESS_NAME)
            greaterOrEquals(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, startDateValue)
            lessOrEquals(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, endDateValue)
            done()
        }
    }
}
