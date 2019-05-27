package com.bonitasoft.rest.api

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class Case implements RestApiController, CaseActivityHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Case.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def contextPath = request.contextPath
        def processAPI = context.apiClient.getProcessAPI()
        def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE).done()
        def result = processAPI.searchProcessInstances(searchOptions).getResult()
                .collect {
                    [id: it.id, name: processName(it.processDefinitionId, processAPI), state: asLabel(it.state.toUpperCase(), "info"), viewAction: viewActionLink(it.id, processAPI, contextPath)]
                }
        processAPI.searchArchivedProcessInstances(searchOptions).getResult()
                .collect {
                    result << [id: it.sourceObjectId, name: processName(it.processDefinitionId, processAPI), state: asLabel(it.state.toUpperCase(), "default"), viewAction: viewActionLink(it.sourceObjectId, processAPI, contextPath)]
                }

        return responseBuilder.with {
            withResponseStatus(HttpServletResponse.SC_OK)
            withResponse(new JsonBuilder(result).toString())
            build()
        }
    }

    def asLabel(state, style) {
        """<span class="label label-$style">$state</span>"""
    }

    def String processName(long processDefId, ProcessAPI processAPI) {
        def definition = processAPI.getProcessDefinition(processDefId)
        return "$definition.name ($definition.version)"
    }

    def String viewActionLink(long caseId, ProcessAPI processAPI, contextPath) {
        def openTasks = searchOpenedTasks(caseId, processAPI).result
                .findAll { canExecute(getState(it, processAPI).name) }
        if (openTasks.size() > 0) {
            return """<a class="btn btn-primary btn-sm" href="$contextPath/apps/cases/case?id=$caseId" target="_target">Open <span class="badge"> $openTasks.size</span></a>"""
        } else {
            return ""
        }
    }
}
