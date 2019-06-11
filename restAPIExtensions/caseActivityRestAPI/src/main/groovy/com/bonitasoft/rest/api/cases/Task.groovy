package com.bonitasoft.rest.api.cases

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonitasoft.rest.api.helper.Helper
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

/**
 * Return an task if it is available (i.e BPMN ready && $activityState != N/A)
 */
class Task implements RestApiController, Helper {

    @Override
    public RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder,
            RestAPIContext context) {
        def caseId = request.getParameter "caseId"
        if (!caseId) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter caseId is missing"}""")
        }

        def taskName = request.getParameter "taskName"
        if (!taskName) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter taskName is missing"}""")
        }

        try {
            validateCaseAccess(caseId, context);
        } catch (IllegalAccessException e) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_FORBIDDEN,"""{"error" : "You don't have access to this case"}""")
        }

        def processAPI = context.apiClient.processAPI

        def searchOptions = new SearchOptionsBuilder(0, 1).with {
            filter(ActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, caseId)
            filter(ActivityInstanceSearchDescriptor.NAME, taskName)
            done()
        }
        def results = processAPI.searchPendingTasksForUser(context.apiSession.userId, searchOptions).getResult()

        if (!results.isEmpty()) {
            def task = results[0]
            def metadata = getMetadata(task,processAPI)
            if (canExecute(metadata.$activityState)) {
                return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(task).toString())
            }
        }
        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, "");
    }
}
