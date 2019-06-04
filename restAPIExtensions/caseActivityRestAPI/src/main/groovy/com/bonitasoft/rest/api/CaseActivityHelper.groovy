package com.bonitasoft.rest.api;


import org.bonitasoft.engine.bpm.data.DataNotFoundException
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.ManualTaskInstance
import org.bonitasoft.engine.bpm.flownode.UserTaskInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor
import org.bonitasoft.engine.bpm.process.ProcessInstance

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor
import com.bonitasoft.web.extension.rest.RestAPIContext

trait CaseActivityHelper {

	def canExecute(String state) {
		return state != "N/A" &&
				state != ActivityStates.COMPLETED_STATE &&
				state != ActivityStates.FAILED_STATE &&
				state != ActivityStates.ABORTED_STATE
	}

	def getState(ActivityInstance activityInstance, ProcessAPI processAPI) {
		try {
			def defaultState = activityInstance.getState()
			if(activityInstance instanceof ManualTaskInstance) {
				return [name: 'Optional', id: idOfState('Optional')]
			}
			def instance = processAPI.getActivityTransientDataInstance('$activityState', activityInstance.id)
			if (defaultState == ActivityStates.ABORTED_STATE || defaultState == ActivityStates.FAILED_STATE) {
				return [name: defaultState, id: idOfState(instance.value)]
			}
			return [name: instance.value, id: idOfState(instance.value)]
		} catch (DataNotFoundException e) {
			println e.getMessage()
			return [name: "Optional", id: idOfState("Optional")]
		}
	}

	def idOfState(String state) {
		switch (state) {
			case "Required": return 1
			case "Optional": return 2
			case "Discretionary": return 3
			case "N/A": return 4
			case "completed": return 5
			default: return 6
		}
	}

	def SearchResult searchOpenedTasks(Long caseId, ProcessAPI processAPI) {
		def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE)
		return processAPI.searchHumanTaskInstances(searchOptions.filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, caseId)
				.differentFrom(ActivityInstanceSearchDescriptor.NAME, "Dymanic Activity Container")
				.and()
				.differentFrom(ActivityInstanceSearchDescriptor.NAME, "Create Activity")
				.done())
	}

	/**
	 * Throw an exception if the calling user should not access to this case 
	 */
	// TODO: take into account the manager, the accounting ...
	def void validateCaseAccess(String caseId, RestAPIContext context) {
		def processAPI = context.getApiClient().getProcessAPI()
		def userId = context.apiSession.userId

		def searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE)
				.filter(ProcessInstanceSearchDescriptor.ID, caseId)
				.filter(ProcessInstanceSearchDescriptor.STARTED_BY, userId)
				.done()

		def processAPISearchProcessInstancesGetResult = processAPI.searchProcessInstances(searchOptions).getResult()
		if (processAPISearchProcessInstancesGetResult.isEmpty()) {
			searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE)
					.filter(ArchivedProcessInstancesSearchDescriptor.STARTED_BY, userId)
					.done()
			if (processAPISearchProcessInstancesGetResult.isEmpty()
                 && !checkManagerCaseAccess(caseId, context)) {
				throw new IllegalStateException()
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

	/**
	 * @param processAPI
	 * @param processName
	 * @param processVersion
	 * @return The ProcessDeployementInfo that matches the given tuple [processName - processVersion]
	 */
	def ProcessDeploymentInfo retrieveProcess(ProcessAPI processAPI, String processName, String processVersion) {
		def result = processAPI.searchProcessDeploymentInfos(new SearchOptionsBuilder(0, Integer.MAX_VALUE)
				.filter(ProcessDeploymentInfoSearchDescriptor.NAME, processName)
				.filter(ProcessDeploymentInfoSearchDescriptor.VERSION, processVersion)
				.done()
			).getResult()
		if (result.isEmpty()) {
			throw new IllegalArgumentException("The process $processName - $processVersion doesn't exist")
		}
		return result[0]
	}
	
	def getMetadata(HumanTaskInstance task, ProcessAPI processAPI) {
		def res = [:]
		if(task instanceof ManualTaskInstance) {
			res.put('$activityState', 'Optional')
		} else {
			processAPI.getActivityTransientDataInstances(task.id, 0, Integer.MAX_VALUE)
					.findAll{ it.name.startsWith('$') }
					.collect{ res.put(it.name,it.value) }
		}
		return res
	}
	
	
	def String linkTarget(ActivityInstance instance) {
		if(instance instanceof UserTaskInstance) {
			"_self"
		}else if(instance instanceof ManualTaskInstance) {
			"_parent"
		}
	}
}
