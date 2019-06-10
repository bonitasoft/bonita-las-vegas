package com.bonitasoft.rest.api.helper

import org.bonitasoft.engine.identity.UserMembershipCriterion
import org.bonitasoft.engine.search.SearchOptionsBuilder

import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor
import com.bonitasoft.web.extension.rest.RestAPIContext

trait SecurityHelper {

    private static final String ACCOUNTING_GROUP_NAME = "finance"

    /**
     * Throw an exception if the calling user should not access to this case
     */
    def void validateCaseAccess(String caseId, RestAPIContext context) {
        def processAPI = context.getApiClient().getProcessAPI()
        def userId = context.apiSession.userId

        if (!isAccountingMember(context, userId)) {
            def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE)
                    .filter(ProcessInstanceSearchDescriptor.ID, caseId)
                    .filter(ProcessInstanceSearchDescriptor.STARTED_BY, userId)
                    .done()

            def processAPISearchProcessInstancesGetResult = processAPI.searchProcessInstances(searchOptions).getResult()
            if (processAPISearchProcessInstancesGetResult.isEmpty()) {
                searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE)
                        .filter(ArchivedProcessInstancesSearchDescriptor.STARTED_BY, userId)
                        .done()
                processAPISearchProcessInstancesGetResult = processAPI.searchArchivedProcessInstances(searchOptions).getResult()
                if (processAPISearchProcessInstancesGetResult.isEmpty() && !checkManagerCaseAccess(caseId, context)) {
                    throw new IllegalAccessException()
                }
            }
        }
    }

    /**
     * Return true if the calling user is the manager of the user that started the case in input
     */
    def boolean checkManagerCaseAccess(String caseId, RestAPIContext context) {
        def processAPI = context.getApiClient().getProcessAPI()
        def managerId = context.apiSession.userId

        def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE)
                .filter(ProcessInstanceSearchDescriptor.ID, caseId)
                .done()
        def processInstances = processAPI.searchProcessInstances(searchOptions).getResult();
        if (processInstances.isEmpty()) {
            throw new IllegalArgumentException("The case $caseId doesn't exist")
        }
        def user = context.getApiClient().getIdentityAPI().getUser(processInstances[0].startedBy)
        Objects.equals(user.getManagerUserId(), managerId)
        // TODO archivedProcess? -> Manager could have changed, what do we do?
    }

    def boolean isAccountingMember(RestAPIContext context, Long userId) {
        def res  = context.getApiClient().getIdentityAPI()
                .getUserMemberships(userId, 0, Integer.MAX_VALUE, UserMembershipCriterion.GROUP_NAME_ASC)
                .any {
                    Objects.equals(it.groupName , ACCOUNTING_GROUP_NAME)
                }
    }
}
