package com.bonitasoft.rest.api;


import org.bonitasoft.engine.bpm.data.DataNotFoundException
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import com.bonitasoft.engine.api.ProcessAPI

class CaseActivityHelper {
	
	def static canExecute(state) {
		return state.name != "N/A"&&
			   state.name != ActivityStates.COMPLETED_STATE &&
			   state.name != ActivityStates.FAILED_STATE &&
			   state.name != ActivityStates.ABORTED_STATE
	}
	
	def static getState(ActivityInstance activityInstance, ProcessAPI processAPI) {
		try {
			def defaultState = activityInstance.getState()
			def instance = processAPI.getActivityDataInstance("activityState", activityInstance.id);
			if(defaultState == ActivityStates.ABORTED_STATE || defaultState == ActivityStates.FAILED_STATE ) {
				return [name:defaultState,id:idOfState(instance.value)]
			}
			return [name:instance.value,id:idOfState(instance.value)]
		}catch(DataNotFoundException e) {
			return [name:"Optional",id:idOfState("Optional")]
		}
	}
	
	def static idOfState(String state) {
		switch(state) {
			case "Required": return 1
			case "Optional": return 2
			case "Discretionary" : return 3
			case "N/A": return 4
			case "Completed" : return 5
			default: return 6
		}
	}
	def static SearchResult searchOpenedTasks( caseId, processAPI) {
		return processAPI.searchHumanTaskInstances(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
			filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, caseId)
			differentFrom(ActivityInstanceSearchDescriptor.NAME, "Dymanic Activity Container")
			and()
			differentFrom(ActivityInstanceSearchDescriptor.NAME, "Create Activity")
			done()
		})
	}
	
}
