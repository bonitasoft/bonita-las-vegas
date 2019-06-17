package com.bonitasoft.rest.api.helper

import org.bonitasoft.engine.bpm.data.DataNotFoundException
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.ManualTaskInstance

import com.bonitasoft.engine.api.ProcessAPI

trait StateHelper {

    def canExecute(String state) {
        return state != "N/A" &&
                state != "Not available" &&
                state != ActivityStates.COMPLETED_STATE &&
                state != ActivityStates.FAILED_STATE &&
                state != ActivityStates.ABORTED_STATE
    }

    def idOfState(String state) {
        switch (state) {
            case "Required": return 1
            case "Optional": return 2
            case "Discretionary": return 3
            case "Available": return 3
            case "N/A": return 4
            case "completed": return 5
            default: return 6
        }
    }

    def getMetadata(HumanTaskInstance task, ProcessAPI processAPI) {
        def res = [:]
        if(task instanceof ManualTaskInstance) {
            res.put('$activityState', 'Optional')
        } else {
            processAPI.getActivityTransientDataInstances(task.id, 0, Integer.MAX_VALUE)
                    .findAll{
                        it.name.startsWith('$')
                    }
                    .collect{
                        res.put(it.name, toStateDisplayName(it.value))
                    }
        }
        return res
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

    def toStateDisplayName(String state) {
        switch (state) {
            case "N/A": return "Not available"
            case "Discretionary": return "Available"
            default: return state.capitalize()
        }
    }
}
