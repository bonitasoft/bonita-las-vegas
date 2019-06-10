package com.bonitasoft.rest.api.validation

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.identity.User
import org.bonitasoft.engine.identity.UserCriterion
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonita.lr.model.ExpenseReportDAO
import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor
import com.bonitasoft.rest.api.cases.Case
import com.bonitasoft.rest.api.helper.Helper
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

/**
 *	This API returns the tasks available for the calling manager. 
 *	To be returned, a task must:
 *		- Be the task `Manager Validation` of the process Expense Report
 *		- The case must have been started by a user managed by the calling manager
 */
class TaskManager implements RestApiController, Helper {

    private static final String MANAGER_VALIDATION = "Manager validation"

    @Override
    public RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder,
            RestAPIContext context) {
        def contextPath = request.contextPath
        def processAPI = context.apiClient.getProcessAPI()
        def userId = context.apiSession.userId
        def expenseReportProcessDef = retrieveProcess(processAPI, Case.EXPENSE_REPORT_PROCESS_NAME, Case.EXPENSE_REPORT_PROCESS_VERSION)

        def users = context.getApiClient().getIdentityAPI().getUsersWithManager(userId, 0, Integer.MAX_VALUE, UserCriterion.USER_NAME_ASC)

        if (users.isEmpty()) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_FORBIDDEN,"") // 403 -> not a manager, or a useless one
        }

        def instances = retrieveManagerValidationTaskInstances(context, processAPI, expenseReportProcessDef, contextPath, users)

        buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(instances).toString())
    }

    /**
     * @return All active instances of the task  `Manager Validation` available for the manager
     */
    def  retrieveManagerValidationTaskInstances(RestAPIContext context, ProcessAPI processAPI, ProcessDeploymentInfo process, String contextPath, List<User> users) {

        retrieveCaseInstances(processAPI, users, process)
                .collect { instance ->
                    HumanTaskInstance task = processAPI.searchHumanTaskInstances(new SearchOptionsBuilder(0, 1)
                            .filter(HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID, instance.id)
                            .filter(HumanTaskInstanceSearchDescriptor.NAME, MANAGER_VALIDATION)
                            .done()).result[0]

                    if (task) {
                        SimpleBusinessDataReference businessDataRef = processAPI.getProcessInstanceExecutionContext(instance.id)['expenseReport_ref']
                        def expenseReport = context.getApiClient().getDAO(ExpenseReportDAO.class).findByPersistenceId(businessDataRef.storageId)
                        return [
                            user:user(context, instance),
                            description:expenseReport.expenseHeader.description,
                            url:forge(process.name,process.version,task,contextPath),
                            target:"_self",
                        ]
                    }
                }.findAll() // null are ignored
    }

    def List<ProcessInstance> retrieveCaseInstances(ProcessAPI processAPI, List<User> users, ProcessDeploymentInfo process) {
        users
                .collect { user ->
                    new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
                        filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, process.processId)
                        filter(ProcessInstanceSearchDescriptor.STARTED_BY, user.id)
                        done()
                    }
                }.collect {
                    processAPI.searchProcessInstances(it).getResult()
                }
                .flatten()
    }

    def String forge(String processName, String processVersion, ActivityInstance instance, contextPath) {
        "$contextPath/portal/resource/taskInstance/$processName/$processVersion/$instance.name/content/?id=$instance.id&displayConfirmation=false"
    }

    def String user(RestAPIContext context, ProcessInstance instance) {
        def user =	context.apiClient.getIdentityAPI().getUser(instance.startedBy)
        "$user.firstName $user.lastName"
    }
}
