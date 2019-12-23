package com.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Task implements ITask {

    // instance variables
    private String _description;
    private List<UUID> dependencies;
    private UUID taskID;
    private float _time = 0.0f;
    private boolean sink = false;

    // private constructors

    /**
     * Default constructor for {@link Task}
     */
    private Task(float time) {
        super();
        this.dependencies = new ArrayList<>();
        this.taskID = UUID.randomUUID();
        this.modifyTime(time);
    }

    /**
     * Constructor for {@link Task} that takes a description.
     * @param description
     */
    private Task(float time, String description) {
        this(time);
        this.modifyDescription(description);
    }

    /**
     * Constructor for {@link Task} that takes both a description and a dependency.
     * @param description
     * @param dependency
     */
    private Task(float time, String description, UUID dependency) {
        this(time, description);
        this.dependencies.add(dependency);
    }

    private Task(float time, String description, List<UUID> deps) {
        this(time, description);
        for (UUID dep : deps) {
            this.dependencies.add(dep);
        }
    }

    /**
     * Static factory that creates a new Task with a description and a list of dependencies (list of taskIDs)
     * @param description
     * @param deps
     * @return
     */
    public static Task ofDescriptionAndDeps(float time, String description, List<UUID> deps) {
        return new Task(time, description, deps);
    }

    public static Task sinkTask() {
        Task task = new Task(0.0f);
        task.setSink();
        task.modifyDescription("sink");
        return task;
    }

    /**
     * @return the taskID
     */
    @Override
    public UUID getTaskID() {
        return taskID;
    }


    /**
     * @return the Description
     */
    @Override
    public String getDescription() {
        return _description;
    }

    @Override
    public boolean addDependencies(int dependency) {
        return false;
    }

    /**
     * @return the dependencies
     */
    @Override
    public List<UUID> getDependencies() {
        return dependencies;
    }

    @Override
    public void modifyDescription(String description) {
        this._description = description;
    }

    /** Returns the time the task should take to complete */
    @Override
    public float getTime() {
        return this._time;
    }

    /** Let's the user modify a task's time */
    @Override
    public void modifyTime(float time) {
        this._time = time;
    }

    @Override
    public void setSink() {
        this.sink = true;
    }

    @Override
    public boolean isSink() {
        return this.sink;
    }
}
