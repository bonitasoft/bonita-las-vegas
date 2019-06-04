import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder

import com.bonitasoft.engine.api.APIAccessor

class ActivityStateResolver {

    public static final String REQUIRED = "Required"
    public static final String DISCRETIONARY =  "Discretionary"
    public static final String COMPLETED =  "Completed"
    public static final String NOT_AVAILABLE = "N/A"

    /**
     * @param processInstanceId
     * @param apiAccessor
     * @param taskName the task that should be available once or more
     * @return Required` if the task has never been executed, `Discretionary` if it has.
     */
    public static String onceOrMore (long processInstanceId, APIAccessor apiAccessor, String taskName) {
        taskNotExecuted(processInstanceId, apiAccessor, taskName)? REQUIRED : DISCRETIONARY
    }

    /**
     * @param processInstanceId
     * @param apiAccessor
     * @param taskName
     * @return `Required` if the task has never been executed, `Completed` if it has.
     */
    public static String onlyOnce (long processInstanceId, APIAccessor apiAccessor, String taskName) {
        taskNotExecuted(processInstanceId, apiAccessor, taskName) ? REQUIRED : COMPLETED
    }

    /**
     * 
     * @param processInstanceId
     * @param apiAccessor
     * @param taskNames
     * @return `Required` if all the tasks in input have been executed at least once, `N/A` if not. 
     */
    public static String dependsOnOtherTaskExecutions(long processInstanceId, APIAccessor apiAccessor, String... taskNames) {
        taskNames.findAll { taskNotExecuted(processInstanceId, apiAccessor, it) }.empty ? REQUIRED : NOT_AVAILABLE
    }

    /* ########################################
     * ########### Internal methods  ##########
     * ######################################## */

    private static boolean taskNotExecuted(long processInstanceId, APIAccessor apiAccessor, String taskName) {
        def archives = findArchivedHumanTaskInstances(processInstanceId, apiAccessor, taskName, 0, 1, ActivityStates.COMPLETED_STATE)
        def completed =  findHumanTaskInstances(processInstanceId, apiAccessor, taskName, 0, 1, ActivityStates.COMPLETED_STATE)
        completed.isEmpty() && archives.isEmpty()
    }

    private static boolean taskAvailable(long processInstanceId, APIAccessor apiAccessor, String taskName) {
        !findHumanTaskInstances(processInstanceId, apiAccessor, taskName, 0, 1, ActivityStates.READY_STATE).isEmpty()
    }

    private static List<ArchivedHumanTaskInstance> findArchivedHumanTaskInstances(long processInstanceId, APIAccessor apiAccessor, String taskName,
            int startIndex, int maxResult, String activityState) {
        apiAccessor.getProcessAPI().searchArchivedHumanTasks(new SearchOptionsBuilder(startIndex, maxResult).with {
            filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, processInstanceId)
            filter(ArchivedActivityInstanceSearchDescriptor.NAME, taskName)
            filter(ArchivedActivityInstanceSearchDescriptor.STATE_NAME, activityState)
            done()
        }).getResult()
    }

    private static List<HumanTaskInstance> findHumanTaskInstances(long processInstanceId, APIAccessor apiAccessor, String taskName,
            int startIndex, int maxResult, String activityState) {
        apiAccessor.getProcessAPI().searchHumanTaskInstances(new SearchOptionsBuilder(startIndex, maxResult).with {
            filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, processInstanceId)
            filter(ActivityInstanceSearchDescriptor.NAME, taskName)
            filter(ActivityInstanceSearchDescriptor.STATE_NAME, activityState)
            done()
        }).getResult()
    }
}
