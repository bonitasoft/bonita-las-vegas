package com.bonitasoft.rest.api.validation

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.rest.api.cases.Case
import com.bonitasoft.rest.api.helper.Helper
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

/**
 *  This API returns the tasks available for the calling accounting
 *  the API takes in parameter the task name to return
 */
class TaskAccounting implements RestApiController, Helper {

    @Override
    public RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder,
            RestAPIContext context) {
        def contextPath = request.contextPath
        def processAPI = context.apiClient.getProcessAPI()
        def userId = context.apiSession.userId

        if (!isAccountingMember(context, context.apiSession.userId)) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_FORBIDDEN, "")
        }

        def taskName = request.getParameter "taskName"
        def appToken = request.getParameter "appToken"
        if(!taskName) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "The parameter taskName is mandatory")
        }
        if(!appToken) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `appToken` is mandatory")
        }


        def expenseReportProcessDef = retrieveProcess(processAPI, Case.EXPENSE_REPORT_PROCESS_NAME, Case.EXPENSE_REPORT_PROCESS_VERSION)

        def instances = retrieveTaskInstances(context, processAPI, expenseReportProcessDef, contextPath, taskName, appToken)

        buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(instances).toString())
    }

    def String forge(String processName, String processVersion, ActivityInstance instance, contextPath, appToken) {
        "$contextPath/portal/resource/taskInstance/$processName/$processVersion/$instance.name/content/?id=$instance.id&displayConfirmation=false&app=$appToken"
    }

    def retrieveTaskInstances(RestAPIContext context, ProcessAPI processAPI, ProcessDeploymentInfo process,
            String contextPath, String taskName, String appToken) {
        retrieveCaseInstances(processAPI, process)
                .collect { instance ->
                    HumanTaskInstance task = processAPI.searchHumanTaskInstances(new SearchOptionsBuilder(0, 1)
                            .filter(HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID, instance.id)
                            .filter(HumanTaskInstanceSearchDescriptor.NAME, taskName)
                            .done()).result[0]
                    if (task) {
                        SimpleBusinessDataReference businessDataRef = processAPI.getProcessInstanceExecutionContext(instance.id)['expenseReport_ref']
                        def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)
                        def date = task.reachedStateDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        return [
                            user:user(context, instance),
                            description:expenseReport.expenseHeader.description,
                            date: date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            url:forge(process.name,process.version,task,contextPath,appToken),
                            target:"_self",
                        ]
                    }
                }.findAll().sort { task1, task2 -> LocalDate.parse(task1.date) <=> LocalDate.parse(task2.date) }
    }
}