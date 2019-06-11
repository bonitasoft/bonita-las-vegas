package com.bonitasoft.rest.api.document;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.document.Document
import org.bonitasoft.engine.bpm.document.DocumentsSearchDescriptor
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

class CaseDocument implements RestApiController, Helper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocument.class)

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
        def documents = retrieveDocuments(context, processAPI, caseId as long)

        def result = documents.collect{ Document doc ->
            [
                name:doc.name,
                url:doc.url,
                creationDate:doc.creationDate,
                fileName:doc.contentFileName,
                id:doc.id,
                description:doc.description.replace("\n", "<br>"),
                username:username(doc.author,context.apiClient.getIdentityAPI())
            ]
        }

        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
    }

    def retrieveDocuments(RestAPIContext context, ProcessAPI processAPI, long caseId) {
        processAPI.searchDocuments(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, caseId)
            sort(DocumentsSearchDescriptor.DOCUMENT_CREATIONDATE, Order.DESC)
            done()
        }).getResult()
    }

    def String username(Long userId,IdentityAPI identityApi) {
        def user = identityApi.getUser(userId)
        user.firstName + " " + user.lastName
    }
}
