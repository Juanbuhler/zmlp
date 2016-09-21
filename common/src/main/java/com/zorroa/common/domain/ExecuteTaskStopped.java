package com.zorroa.common.domain;

/**
 * ExecuteTaskStopped contains new state of the task.
 */
public class ExecuteTaskStopped implements TaskId, JobId {

    private ExecuteTask task;
    private TaskState newState;

    public ExecuteTaskStopped() { }

    public ExecuteTaskStopped(ExecuteTask task) {
        this.task = task;
    }

    public ExecuteTaskStopped(ExecuteTask task, TaskState newState) {
        this.task = task;
        this.newState = newState;
    }

    public TaskState getNewState() {
        return newState;
    }

    public ExecuteTaskStopped setNewState(TaskState newState) {
        this.newState = newState;
        return this;
    }

    public ExecuteTask getTask() {
        return task;
    }

    public ExecuteTaskStopped setTask(ExecuteTask task) {
        this.task = task;
        return this;
    }

    @Override
    public Integer getTaskId() {
        return task.getTaskId();
    }

    @Override
    public Integer getParentTaskId() {
        return task.getParentTaskId();
    }

    @Override
    public Integer getJobId() {
        return task.getJobId();
    }
}
