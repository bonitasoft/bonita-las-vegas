package com.bonitasoft.rest.api.cases;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

class ResolveCase implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolveCase.class)
    private static final String ACTIVITY_CONTAINER = "Dymanic Activity Container"

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def id = request.getParameter "caseId"
        if(!id){
            return responseBuilder.with {
                withResponseStatus(HttpServletResponse.SC_BAD_REQUEST)
                withResponse("""{"error" : "the parameter caseId is missing"}""")
                build()
            }
        }

        def processAPI = context.apiClient.getProcessAPI()
        processAPI.cancelProcessInstance(id as long)

        def result = processAPI.searchArchivedProcessInstances(new SearchOptionsBuilder(0, 1).with {
            filter(ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID, id)
            done()
        }).getResult()
        if(result && result.size() > 0) {
            id = result[0]
        }
        return responseBuilder.with {
            withResponseStatus(HttpServletResponse.SC_OK)
            withResponse(id)
            build()
        }
    }
}
