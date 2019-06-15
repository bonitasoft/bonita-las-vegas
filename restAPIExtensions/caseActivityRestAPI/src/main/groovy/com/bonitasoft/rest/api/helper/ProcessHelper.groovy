package com.bonitasoft.rest.api.helper

import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor
import com.bonitasoft.rest.api.exception.ProcessInstanceNotFoundException
import com.bonitasoft.web.extension.rest.RestAPIContext

trait ProcessHelper {

    /**
     * @param processAPI
     * @param processName
     * @param processVersion
     * @return The ProcessDeployementInfo that matches the given tuple [processName - processVersion]
     */
    def ProcessDeploymentInfo retrieveProcess(ProcessAPI processAPI, String processName, String processVersion) throws ProcessInstanceNotFoundException {
        def result = processAPI.searchProcessDeploymentInfos(new SearchOptionsBuilder(0, 1)
                .filter(ProcessDeploymentInfoSearchDescriptor.NAME, processName)
                .filter(ProcessDeploymentInfoSearchDescriptor.VERSION, processVersion)
                .done()
                ).getResult()
        if (result.isEmpty()) {
            throw new ProcessInstanceNotFoundException("The process $processName - $processVersion doesn't exist")
        }
        return result[0]
    }

    /**
     *
     * @param processAPI
     * @param process
     * @return All the active instances of the input process
     */
    def List<ProcessInstance> retrieveCaseInstances(ProcessAPI processAPI, ProcessDeploymentInfo process) {
        def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE)
                .filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, process.processId)
                .done()
        processAPI.searchProcessInstances(searchOptions).getResult()
    }

    /**
     * 
     * @param context
     * @param processAPI
     * @param caseId
     * @return the archived instance of this case
     * @throws ProcessInstanceNotFoundException
     */
    def ArchivedProcessInstance retrieveArchivedInstance(RestAPIContext context, ProcessAPI processAPI, long caseId) throws ProcessInstanceNotFoundException {
        def options = new SearchOptionsBuilder(0, 1)
                .filter(ArchivedProcessInstancesSearchDescriptor.ID, caseId)
                . done()
        def results = processAPI.searchArchivedProcessInstances(options).getResult()
        if (results.isEmpty()) {
            throw new ProcessInstanceNotFoundException("The archived process with the id $caseId doesn't exist")
        }
        return results[0]
    }

    /**
     * 
     * @param caseId
     * @param processAPI
     * @return the opened tasks of the input case
     */
    def SearchResult<HumanTaskInstance> searchOpenedTasks(Long caseId, ProcessAPI processAPI) {
        def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE)
                .filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, caseId)
                .done()
        return processAPI.searchHumanTaskInstances(searchOptions)
    }

    /**
     * 
     * @param context
     * @param instance
     * @return the name of the user that started the instance
     */
    def String user(RestAPIContext context, ProcessInstance instance) {
        def user =  context.apiClient.getIdentityAPI().getUser(instance.startedBy)
        "$user.firstName $user.lastName"
    }

    /**
     *
     * @param context
     * @param instance
     * @return the name of the user that started the instance
     */
    def String user(RestAPIContext context, ArchivedProcessInstance instance) {
        def user =  context.apiClient.getIdentityAPI().getUser(instance.startedBy)
        "$user.firstName $user.lastName"
    }
}
