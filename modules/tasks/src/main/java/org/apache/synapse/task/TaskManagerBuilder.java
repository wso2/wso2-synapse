package org.apache.synapse.task;

public class TaskManagerBuilder {
    private static TaskManagerBuilder taskManagerBuilder;

    static {
        taskManagerBuilder = new TaskManagerBuilder();
    }

    private TaskManagerBuilder() {}

    public static TaskManagerBuilder getInstance() {
        return taskManagerBuilder;
    }

    public static TaskManager getTaskManager(String name) {
        if ("org.apache.synapse.startup.tasks.MessageInjector".equals(name)) {
         // todo return quartz task manager.
        }
        System.out.println("Returning a new task manager instance.");
        return null;
    }
}
