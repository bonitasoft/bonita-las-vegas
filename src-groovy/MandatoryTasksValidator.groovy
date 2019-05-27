import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ActivityStates
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor
import org.bonitasoft.engine.search.SearchOptionsBuilder

import com.bonitasoft.engine.api.APIAccessor

public class MandatoryTasksValidator {

    public static boolean validateTaskExecution(long processInstanceId, APIAccessor apiAccessor, String... taskNames) {
        taskNames.findAll { taskNotExecuted(processInstanceId, apiAccessor, it) }.empty
    }

    private static boolean taskNotExecuted(long processInstanceId, APIAccessor apiAccessor, String taskName) {
        def archives = apiAccessor.getProcessAPI().searchArchivedHumanTasks(new SearchOptionsBuilder(0, 1).with {
            filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, processInstanceId)
            filter(ArchivedActivityInstanceSearchDescriptor.NAME, taskName)
            filter(ArchivedActivityInstanceSearchDescriptor.STATE_NAME, ActivityStates.COMPLETED_STATE)
            done()
        }).getResult()

        def completed =  apiAccessor.getProcessAPI().searchHumanTaskInstances(new SearchOptionsBuilder(0, 1).with {
            filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, processInstanceId)
            filter(ActivityInstanceSearchDescriptor.NAME, taskName)
            filter(ActivityInstanceSearchDescriptor.STATE_NAME, ActivityStates.COMPLETED_STATE)
            done()
        }).getResult()

        completed.isEmpty() && archives.isEmpty()
    }
}
