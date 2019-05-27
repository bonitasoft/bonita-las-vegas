package com.bonitasoft.rest.api;

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
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class CaseComment implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseComment.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def caseId = request.getParameter "caseId"
        if (caseId == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter caseId is missing"}""")
        }
		def result = []
		def processAPI = context.apiClient.getProcessAPI()
		processAPI.searchComments(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
			filter(SearchCommentsDescriptor.PROCESS_INSTANCE_ID, caseId.toLong())
			sort(SearchCommentsDescriptor.POSTDATE, Order.DESC)
			done()
		}).getResult()
		.findAll{
			it.userId != null && it.userId != -1
		}.collect{
			result << [content:it.content.replace("\n", "<br>"),postDate:it.postDate,username:username(it.userId,context.apiClient.getIdentityAPI())]
		}
	
        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
    }
	
	def String username(Long userId,IdentityAPI identityApi) {
		def user = identityApi.getUser(userId)
		user.firstName + " " + user.lastName
	}

    /**
     * Build an HTTP response.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  httpStatus the status of the response
     * @param  body the response body
     * @return a RestAPIResponse
     */
    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }


}
