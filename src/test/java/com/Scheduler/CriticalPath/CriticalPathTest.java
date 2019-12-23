package com.Scheduler.CriticalPath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.Scheduler.CriticalPath.CriticalPath;
import com.common.ITask;
import com.common.Task;

import org.junit.Before;
import org.junit.Test;

import static com.common.Utils.logInfo;

public class CriticalPathTest {

    List<ITask> tasks = new ArrayList<>();
    CriticalPath critPath;

    @Before public void initTasks() {
        // create first task and add it
        List<UUID> first = new ArrayList<>();
        float time1 = 4f;
        Task firstTask = Task.ofDescriptionAndDeps(time1, "first", first);
        this.tasks.add(firstTask);

        // create second task with the first as dependency and add it
        List<UUID> second = new ArrayList<>();
        second.add(firstTask.getTaskID());
        float time2 = 2f;
        Task secondTask = Task.ofDescriptionAndDeps(time2, "second", second);
        this.tasks.add(secondTask);

        // create third task with no dependencies
        List<UUID> third = new ArrayList<>();
        float time3 = 4f;
        Task thirdTask = Task.ofDescriptionAndDeps(time3, "third", third);
        this.tasks.add(thirdTask);

        // create fourth task with dependency of second
        List<UUID> fourth = new ArrayList<>();
        fourth.add(secondTask.getTaskID());
        float time4 = 6f;
        Task fourthTask = Task.ofDescriptionAndDeps(time4, "fourth", fourth);
        this.tasks.add(fourthTask);

        // create a CriticalPath and initialize graph from List
        // make a copy as it seems the sorting changed the array here too
        critPath = CriticalPath.ofTasks(new ArrayList<>(this.tasks));

    }



    @Test public void criticalPathIsTruthy() {
        assertTrue(critPath != null);
    }

    @Test public void testDegree() {
        logInfo("firstDegree test");
        int firstDegree = critPath.calcDegree(tasks.get(0));
        assertEquals(0, firstDegree);
        logInfo("secondDegree test");
        int secondDegree = critPath.calcDegree(this.tasks.get(1));
        assertEquals(1, secondDegree);
        logInfo("thirdDegree test");
        int thirdDegree = critPath.calcDegree(this.tasks.get(2));
        assertEquals(0, thirdDegree);
        logInfo("fourthDegree test");
        int fourthDegree = critPath.calcDegree(tasks.get(3));
        assertEquals(2, fourthDegree);
        critPath.createGraphVis();
    }

}
