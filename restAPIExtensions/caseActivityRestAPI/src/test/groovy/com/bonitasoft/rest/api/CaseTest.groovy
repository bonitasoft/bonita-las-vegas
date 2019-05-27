package com.bonitasoft.rest.api

import javax.servlet.http.HttpServletRequest

import com.bonitasoft.engine.api.APIClient
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.rest.api.Case
import com.bonitasoft.web.extension.rest.RestAPIContext
import groovy.json.JsonSlurper
import org.bonitasoft.engine.bpm.data.DataInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.FlowNodeType
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.impl.internal.ActivityInstanceImpl
import org.bonitasoft.engine.bpm.process.ProcessDefinition
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.bpm.process.impl.internal.ProcessDefinitionImpl
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import spock.lang.Specification

class CaseTest extends Specification {

    RestApiResponseBuilder responseBuilder = new RestApiResponseBuilder()
    ProcessDefinition processDefinition = new ProcessDefinitionImpl("name", "version")

    ProcessAPI processAPI = Mock()
    APIClient apiClient = Mock()
    HttpServletRequest request = Mock()
    RestAPIContext context = Mock()
    SearchResult<ProcessInstance> result = Mock()
    SearchResult<HumanTaskInstance> taskResult = Mock()
    DataInstance dataInstance = Mock()

    def "setup"() {
        request.contextPath >> "/bonitaExpenseReport"
        context.apiClient >> apiClient
        apiClient.getProcessAPI() >> processAPI
        processAPI.searchProcessInstances(_) >> result
        processAPI.searchArchivedProcessInstances(_) >> result
        processAPI.getProcessDefinition(_) >> processDefinition
        processAPI.searchHumanTaskInstances(_) >> taskResult
        processAPI.getActivityTransientDataInstance(_, _) >> dataInstance

        def list = [id: 45L, processDefinitionId: 56L, sourceObjectId: 78L, state: 'state']
        result.getResult() >> [list]

        ActivityInstance activityInstance = new ActivityInstanceImpl("activity name", 65464L) {
            @Override
            FlowNodeType getType() {
                return "*"
            }

            @Override
            long getId() {
                45L
            }

            @Override
            String getState() {
                'state'
            }
        }
        taskResult.getResult() >> [activityInstance]
        dataInstance.value >> "data value"
    }

    def "should keep server name"() {
        given:

        Case aCase = new Case()

        when:
        RestApiResponse restApiResponse = aCase.doHandle(request, responseBuilder, context)

        then: 'this is no more references to localhost '
        JsonSlurper slurper = new JsonSlurper()
        def parsedResponse = slurper.parseText(restApiResponse.response)
        parsedResponse.each { result ->
            assert result.viewAction.contains("""href="/bonitaExpenseReport/apps/cases/case?id=${result.id}" """)

        }
    }
}