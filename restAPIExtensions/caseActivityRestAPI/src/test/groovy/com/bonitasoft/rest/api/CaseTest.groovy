package com.bonitasoft.rest.api

import javax.servlet.http.HttpServletRequest

import org.bonitasoft.engine.bpm.data.DataInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.FlowNodeType
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.impl.internal.ActivityInstanceImpl
import org.bonitasoft.engine.bpm.process.ProcessDefinition
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.bpm.process.impl.internal.ProcessDefinitionImpl
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.engine.session.APISession
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonita.lr.model.ExpenseHeader
import com.bonita.lr.model.ExpenseReport
import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.APIClient
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.web.extension.rest.RestAPIContext

import spock.lang.Specification

// TODO: ne test pas si on ne return que les process du user qui fait le call ... (comment faire?)
class CaseTest extends Specification {

    RestApiResponseBuilder responseBuilder = new RestApiResponseBuilder()
    ProcessDefinition processDefinition = new ProcessDefinitionImpl("name", "version")

    ProcessAPI processAPI = Mock()
    APIClient apiClient = Mock()
    APISession apiSession = Mock()
    HttpServletRequest request = Mock()
    RestAPIContext context = Mock()
    SearchResult<ProcessDeploymentInfo> processDesployementResult = Mock()
    SearchResult<ProcessInstance> result = Mock()
    SearchResult<HumanTaskInstance> taskResult = Mock()
    DataInstance dataInstance = Mock()
    ProcessDeploymentInfo processDeploymentInfo = Mock()
    SimpleBusinessDataReference businessDataRef = Mock()
    ExpenseReportDAO dao = Mock()
    ExpenseReport report = Mock()
    ExpenseHeader header = Mock()

    def "setup"() {
        request.contextPath >> "/bonitaExpenseReport"
        request.getParameter("appToken") >> "expenseReportEmployee"
        context.apiClient >> apiClient
        context.apiSession >> apiSession
        apiSession.userId >> 0
        apiClient.getProcessAPI() >> processAPI
        apiClient.getDAO(ExpenseReportDAO.class) >> dao
        processAPI.searchProcessDeploymentInfos(_) >> processDesployementResult
        processAPI.searchProcessInstances(_) >> result
        processAPI.searchArchivedProcessInstances(_) >> result
        processAPI.getProcessDefinition(_) >> processDefinition
        processAPI.searchHumanTaskInstances(_) >> taskResult
        processAPI.getActivityTransientDataInstance(_, _) >> dataInstance
        processAPI.getProcessInstanceExecutionContext(_) >> ["expenseReport_ref":businessDataRef]
        processDesployementResult.getResult() >> [processDeploymentInfo]
        processDeploymentInfo.getProcessId()() >> 0
        dao.findByPersistenceId(_) >> report
        report.expenseHeader >> header
        header.description >> "My expense report"

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

    //    def "should keep server name and description"() {
    //        given:
    //
    //        Case aCase = new Case()
    //
    //        when:
    //        RestApiResponse restApiResponse = aCase.doHandle(request, responseBuilder, context)
    //
    //        then: 'this is no more references to localhost '
    //        JsonSlurper slurper = new JsonSlurper()
    //        def parsedResponse = slurper.parseText(restApiResponse.response)
    //        parsedResponse.each { result ->
    //            assert result.viewAction.contains("""href="/bonitaExpenseReport/apps/expenseReportEmployee/case?id=${result.id}" """)
    //            assert result.name.equals("My expense report")
    //
    //        }
    //    }
}