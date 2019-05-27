package com.bonitasoft.rest.api;


import org.bonitasoft.engine.bpm.data.DataNotFoundException
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.bpm.flownode.ManualTaskInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult

import com.bonitasoft.engine.api.ProcessAPI

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
}
