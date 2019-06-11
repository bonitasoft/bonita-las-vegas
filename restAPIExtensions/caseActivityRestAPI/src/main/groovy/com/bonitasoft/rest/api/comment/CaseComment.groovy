package com.bonitasoft.rest.api.comment;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.comment.SearchCommentsDescriptor
import org.bonitasoft.engine.search.Order
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.IdentityAPI
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.rest.api.helper.Helper
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class CaseComment implements RestApiController, Helper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseComment.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def caseId = request.getParameter "caseId"
        if (caseId == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter caseId is missing"}""")
        }

        try {
            validateCaseAccess(caseId, context);
        } catch (IllegalAccessException e) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_FORBIDDEN,"""{"error" : "You don't have access to this case"}""")
        }

        def processAPI = context.apiClient.getProcessAPI()
        def comments = retrieveComments(context, processAPI, caseId as long)
        def result = comments.collect{
            [content:it.content.replace("\n", "<br>"),postDate:it.postDate,username:username(it.userId,context.apiClient.getIdentityAPI())]
        }

        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
    }

    def retrieveComments(RestAPIContext context, ProcessAPI processAPI, long caseId) {
        processAPI.searchComments(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(SearchCommentsDescriptor.PROCESS_INSTANCE_ID, caseId)
            sort(SearchCommentsDescriptor.POSTDATE, Order.DESC)
            done()
        }).getResult().findAll {
            it.userId != null && it.userId != -1
        }
    }

    def String username(Long userId,IdentityAPI identityApi) {
        def user = identityApi.getUser(userId)
        user.firstName + " " + user.lastName
    }
}
