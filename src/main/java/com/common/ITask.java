package com.common;

import java.util.List;
import java.util.UUID;

public interface ITask {

    /**
     * Adds a dependency to the task
     * @param dependency
     * @return boolean
     */
    public boolean addDependencies(int dependency);

    /**
     * Modifies the task's description
     * @param description
     */
    public void modifyDescription(String description);

    /**
     * Get's the task's description
     * @return {@link String}
     */
    public String getDescription();

    /**
     * This method returns the taskID of a task
     * @return UUID
     */
    public UUID getTaskID();

    /**
     * Grabs the dependencies of a task
     * @return {List<UUID>}
     */
    public List<UUID> getDependencies();

    /**
     * Returns the time a task should take
     * @return {float}
     */
    public float getTime();

    /** Let's the user modify a task's time to complete */
    public void modifyTime(float time);

    public void setSink(boolean sink);
    public void setIdle(boolean idle);
    public boolean isSink();
    public boolean isSource();
    public boolean isIdle();
}
