package com.Scheduler.CriticalPath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.Scheduler.CriticalPath.CriticalPath;
import com.common.Edge;
import com.common.ITask;
import com.common.Task;

import org.jgrapht.Graph;
import org.junit.Before;
import org.junit.Test;

import static com.common.Utils.logInfo;
import static com.common.GraphUtils.createGraphVis;
import static com.common.GraphUtils.getSourceTask;
import static com.common.GraphUtils.getMaximumEdgeWeight;

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
        Graph<ITask, Edge> graph = critPath.getGraph();
        createGraphVis(graph);
    }

    @Test public void testRemoveNode() {
        logInfo("testRemoveNode");
        ITask taskToRemove = this.tasks.get(0);
        Graph<ITask, Edge> graph = critPath.getGraph();
        ITask source = getSourceTask(graph, true);
        this.critPath.removeNoDepNode(graph, source, taskToRemove);
        createGraphVis(graph);
    }

    @Test public void testGetMaxEdgeWeight() {
        Graph<ITask, Edge> graph = critPath.getGraph();
        double maxEdgeWeight = getMaximumEdgeWeight(graph);
        assertEquals(6, maxEdgeWeight, 0.1d);
    }

    @Test public void testFindCriticalPath() {
        logInfo("testFindCriticalPath");
        Graph<ITask, Edge> graph = critPath.getGraph();
        ITask source = getSourceTask(graph, true);
        ITask sink = getSourceTask(graph, false);
        List<ITask> shortest = this.critPath.maximumPathDP(graph, source, sink).getSecond();
        List<ITask> expected = new ArrayList<>();
        List<String> sExp = new ArrayList<>();
        List<String> sRes = new ArrayList<>();
        expected.add(source);
        expected.add(tasks.get(0));
        expected.add(tasks.get(1));
        expected.add(tasks.get(3));
        expected.add(sink);
        for (ITask task: expected) {
            sExp.add(task.getDescription());
        }
        for (ITask task: shortest) {
            sRes.add(task.getDescription());
        }
        logInfo("testFindCriticalPath :: expected :: " + String.join("->", sExp));
        logInfo("testFindCriticalPath :: result :: " + String.join("->", sRes));
        assertEquals(expected, shortest);
    }

    @Test public void testFindSchedule() {
        logInfo("testFindSchedule :: ");
        List<ITask> tasks = critPath.getSchedule();
        List<String> expected = new ArrayList<>();
        List<String> result = new ArrayList<>();
        expected.add(tasks.get(0).getDescription());
        expected.add(tasks.get(1).getDescription());
        expected.add(tasks.get(2).getDescription());
        expected.add(tasks.get(3).getDescription());
        for (ITask task: tasks) {
            logInfo("testFindSchedule :: task :: " + task.getDescription());
            result.add(task.getDescription());
        }
        logInfo("testFindSchedule :: " + String.join("->", result));
        assertEquals(expected, result);
    }

    @Test public void testMakeMultiprocessorSchedule() {
        logInfo("testMakeMultiprocessorSchedule :: ");
        List<ITask> tasks = critPath.getSchedule();
        List< List <ITask>> answer = critPath.makeMultiprocessorSchedule(tasks, 2);
        List< List <ITask>> expected = new ArrayList<>();
        expected.add(new ArrayList<>());
        expected.add(new ArrayList<>());
        expected.get(0).add(tasks.get(0));
        expected.get(0).add(tasks.get(1));
        expected.get(0).add(tasks.get(2));
        expected.get(1).add(tasks.get(3));
        expected.get(1).add(Task.idleTask(2f));
        int q = 1;
        List<String> sAnswer;
        for (List<ITask> list : answer) {
            sAnswer = new ArrayList<>();
            for (ITask task : list) {
                sAnswer.add(task.getDescription());
            }
            logInfo("testMakeMultiprocessorSchedule :: processor " + q);
            logInfo("testMakeMultiprocessorSchedule :: processor :: tasks " + String.join("->", sAnswer));
            q++;
        }
        for (int i = 0; i < answer.size(); i++) {
            logInfo("testMakeMultiprocessorSchedule :: answer :: processor " + i);
            for (int j = 0; j < answer.get(i).size(); j++) {
                logInfo("testMakeMultiprocessorSchedule :: task :: " + answer.get(i).get(j).getDescription());
            }
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < expected.get(i).size(); j++) {
                logInfo("testMakeMultiprocessorSchedule :: expected :: " + expected.get(i).get(j).getDescription());
                logInfo("testMakeMultiprocessorSchedule :: answer :: " + answer.get(i).get(j).getDescription());
                if (expected.get(i).get(j).isIdle()) {
                    assertEquals(true, answer.get(i).get(j).isIdle());
                } else {
                    assertEquals(expected.get(i).get(j).getTaskID(), answer.get(i).get(j).getTaskID());
                }
            }
        }
    }
}
