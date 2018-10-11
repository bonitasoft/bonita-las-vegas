package com.bonitasoft.handler;

import java.io.Serializable

import org.bonitasoft.engine.api.ProcessAPI
import org.bonitasoft.engine.api.impl.APIUtils
import org.bonitasoft.engine.api.impl.ProcessAPIImpl
import org.bonitasoft.engine.bpm.connector.ConnectorDefinition
import org.bonitasoft.engine.bpm.data.DataDefinition
import org.bonitasoft.engine.bpm.data.DataInstance
import org.bonitasoft.engine.bpm.data.DataNotFoundException
import org.bonitasoft.engine.bpm.data.impl.ShortTextDataInstanceImpl
import org.bonitasoft.engine.bpm.flownode.ActivityDefinitionNotFoundException
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.UserTaskInstance
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition
import org.bonitasoft.engine.core.process.instance.api.states.FlowNodeState
import org.bonitasoft.engine.core.process.instance.model.SUserTaskInstance
import org.bonitasoft.engine.data.instance.api.DataInstanceContainer
import org.bonitasoft.engine.data.instance.exception.SDataInstanceException
import org.bonitasoft.engine.data.instance.model.SDataInstance
import org.bonitasoft.engine.events.model.SEvent
import org.bonitasoft.engine.events.model.SHandler
import org.bonitasoft.engine.events.model.SHandlerExecutionException
import org.bonitasoft.engine.events.model.SUpdateEvent
import org.bonitasoft.engine.expression.model.SExpression
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity
import org.bonitasoft.engine.log.technical.TechnicalLoggerService
import org.bonitasoft.engine.persistence.OrderByType
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javassist.bytecode.stackmap.BasicBlock.Catch


class CaseEventHandler implements SHandler<SEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CaseEventHandler.class)
	private static final String ACTIVITY_CONTAINER = "Dymanic Activity Container"
	private static final String CREATE_ACTIVITY = "Create Activity"

	public CaseEventHandler(){
	}

	@Override
	public void execute(SEvent event) throws SHandlerExecutionException {
		def SUserTaskInstance taskInstance =  event.getObject()
		def processInstanceId = taskInstance.parentProcessInstanceId
		ProcessAPI processAPI = new ProcessAPIImpl()
		def allDataToUpdate = processAPI.searchHumanTaskInstances(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
			filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, processInstanceId)
			differentFrom(ActivityInstanceSearchDescriptor.NAME, ACTIVITY_CONTAINER)
			and()
			differentFrom(ActivityInstanceSearchDescriptor.NAME, CREATE_ACTIVITY)
			and()
			filter(ActivityInstanceSearchDescriptor.STATE_NAME, ActivityStates.READY_STATE)
			done()
		}).getResult()
		.collect{ HumanTaskInstance task ->
			def DesignProcessDefinition design = processAPI.getDesignProcessDefinition(taskInstance.processDefinitionId)
			try{
				[
					definition:processAPI.getActivityDataDefinitions(taskInstance.processDefinitionId, task.name, 0, Integer.MAX_VALUE).find {it.name == "activityState"},
					instance:processAPI.getActivityDataInstance("activityState", task.id),
					taskId:task.id,
					processDefId:taskInstance.processDefinitionId
				]
			} catch( DataNotFoundException e) {
				println "No 'activityState' data defined in $task.displayName"
			}catch( ActivityDefinitionNotFoundException e1) {
				println "No 'activityState' data defined in $task.name"
			}
		}.findAll { it != null && it.definition != null }
		.each {
			def DataDefinition dataDefinition = it.definition
			def DataInstance dataInstance = it.instance
			println "refreshing state using data default value expression"
			def deps = [:]
			def expressions = [:]
			expressions.put(dataDefinition.defaultValueExpression, deps)
			def result = processAPI.evaluateExpressionsOnActivityInstance(it.taskId, expressions)
			def newValue =result.values()[0]

			processAPI.updateActivityDataInstance("activityState", it.taskId, newValue)
			println "'activityState' value of $it.taskId has been updated to $newValue"
		}
	}

	@Override
	public boolean isInterested(SEvent event) {
		return event.getType() == "ACTIVITYINSTANCE_STATE_UPDATED" &&
				event.asType(SUpdateEvent) &&
				event.getObject() instanceof SUserTaskInstance &&
				((SUserTaskInstance)event.getObject()).stateName == ActivityStates.COMPLETED_STATE &&
				((SUserTaskInstance)event.getObject()).name != "Dymanic Activity Container"
	}

	@Override
	public String getIdentifier() {
		return UUID.randomUUID().toString();
	}
}
