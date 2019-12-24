package com.Scheduler.CriticalPath;

import java.awt.Graphics;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;


import com.common.ITask;
import com.common.Task;

import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;
import org.jgrapht.traverse.DepthFirstIterator;

import static com.common.Utils.logInfo;

public class CriticalPath {

    private List<ITask> tasks;
    // The underlying graph structure of the class
    private Graph<ITask, DefaultEdge> graph = this.newGraph();


    /** Primary Constructor */
    private CriticalPath() {
        super();
        this.tasks = new ArrayList<>();
    }

    /**
     * Secondary Constructor that takes a {@link List<ITask>}.
     * @param tasks
     */
    private CriticalPath(List<ITask> tasks) {
        this();
        this.fromArrayofTasks(tasks);
    }

    /**
     * Static factory that creates a Critical Path graph.
     * @param tasks
     * @return {@link CriticalPath}
     */
    public static CriticalPath ofTasks(List<ITask> tasks) {
        return new CriticalPath(tasks);
    }


    /** Creates a .dot language representation of the current graph */
    protected void createGraphVis() {
        if (this.graph != null) {

            ComponentNameProvider<ITask> vertexIDProvider = new ComponentNameProvider<ITask>() {
                @Override
                public String getName(ITask component) {
                    return component.getDescription();
                }
            };

            ComponentNameProvider<ITask> labelIDProvider = new ComponentNameProvider<ITask>() {
                @Override
                public String getName(ITask component) {
                    return component.getDescription();
                }
            };

            GraphExporter<ITask, DefaultEdge> exporter = new DOTExporter<>(vertexIDProvider, labelIDProvider, null);
            Writer writer = new StringWriter();
            try {
                exporter.exportGraph(this.graph, writer);
                logInfo("\n" + writer.toString());
            } catch (ExportException e) {
                logInfo("ExportException :: " + e.toString());
            }
        }
    }
    /**
     * Creates a new {@link com.google.common.graph.Graph} with vertices
     * that are of type {@link ITask}
     * @return Graph<ITask, DefaultEdge>
     */
    private Graph<ITask, DefaultEdge> newGraph() {
        return new SimpleDirectedWeightedGraph<>(DefaultEdge.class);

    }

    private ITask getTaskofUUID(UUID id) {
        return this.graph.vertexSet().stream().filter(task -> task.getTaskID().equals(id)).findAny().orElse(null);
    }

    /**
     * This method adds a vertex to the graph if it is not already there.
     * This method adds a {@link Task} only if its
     * UUID is not present in the graph's vertexSet.
     * @param vertex
     * @return
     */
    private boolean addVertex(ITask vertex) {
        // this.graph.vertexSet().contains(vertex);
        ITask exists = this.graph.vertexSet().stream().filter(task -> task.getTaskID().equals(vertex.getTaskID())).findAny().orElse(null);
        if (exists != null) {
            return false;
        }
        return this.graph.addVertex(vertex);
    }

    /**
     * This private method returns a boolean if an edge is successfully added
     * @param task1
     * @param task2
     * @return {boolean}
     */
    private boolean addEdge(ITask task1, ITask task2) {

        this.graph.addEdge(task1, task2);
        return true;
    }


    /**
     * This method recursively calculates the "degree" of a task.
     * The degree is defined as the largest "degree" of one of its subtasks, plus one.
     * If a task has no dependencies, its "degree" is 0.
     * @param task
     * @return {@link Integer}
     */
    protected int calcDegree(ITask task) {
        logInfo("Master Task calcDegree :: " + task.getTaskID().toString());

        List<UUID> deps = task.getDependencies();

        if (deps.isEmpty()) {
            return 0;
        }

        int maxDegree = 0;

        Task taskOfId;
        logInfo("graph vertexSet :: " + graph.toString());
        // go through all dependencies of current task
        for (UUID dep: deps) {
            logInfo("calcDegree :: " + dep.toString());
            // return the task that matches the id (dep)
            taskOfId = (Task) this.graph.vertexSet().stream().filter(t -> t.getTaskID().equals(dep)).findFirst().get();
            // find the degree of that task
            int currDegree = calcDegree(taskOfId);
            // set the max if it's larger
            if (currDegree > maxDegree) {
                maxDegree = currDegree;
            }

        }

        logInfo("degree of " + task.getTaskID().toString() + "::: " +String.valueOf(maxDegree + 1));
        // by definition:
        return maxDegree + 1;
    }

    /**
     * Returns the maxDegree from an iterable of degrees and Tasks
     * @param list
     * @return {int}
     */
    private int maxDegree(List<Pair<Integer, ITask>> list) {
        int maxDegree = 0;
        for (Pair<Integer, ITask> pair : list) {
            int degree = pair.getFirst();
            if (degree > maxDegree) {
                maxDegree = degree;
            }
        }
        return maxDegree;
    }

    /**
     * This method traverses through the vertex set of a graph and returns a {@link List}
     * of all {@link ITask} that have no dependencies. This is necessary to connect all
     * these tasks to a sink vertex.
     * @param tasks
     * @return {@link List}
     */
    private List<ITask> getTasksNoDeps(Set<ITask> tasks) {
        List<ITask> noDeps = new ArrayList<>();
        for (ITask task : tasks) {
            if (!task.isSink() && task.getDependencies().isEmpty()) {
                noDeps.add(task);
            }
        }
        return noDeps;
    }


    /**
     * This method builds a graph from an iterable of Tasks.
     * It does this by adding the tasks to the graph as vertices first, and then
     * Sorting them by their degree - see {@link calcDegree}.
     * After sorting, it goes through all dependencies in order of their degree and
     * as long as their degrees does not equal zero, adds an edge between its dependencies.
     * NOTE: it assumes that every task listed in the dependencies iterable are not related.
     * That is: Any two tasks listed in a task's dependency iterable will not have one depend on the
     * other and vice-versa.
     * @param tasks
     */
    public void fromArrayofTasks(List<ITask> tasks) {
        this.tasks = tasks;
        // if there is an already defined graph, reset it
        if (!this.graph.vertexSet().isEmpty()) {
            this.graph = this.newGraph();
        }
        // set the vertices and calcDegree
        List<Pair<Integer, ITask>> pairs = new ArrayList<>();
        for (ITask task : tasks) {
            logInfo("Adding task to graph: " + task.getTaskID().toString());
            boolean res = this.addVertex(task);
            logInfo("Res :: " + res);
            pairs.add(new Pair<Integer,ITask>(calcDegree(task), task));
        }

        // add sink task
        ITask sink = Task.sinkTask();
        this.addVertex(sink);


        // set the edges in order of degree
        pairs.sort((Pair<Integer, ITask> task1, Pair<Integer, ITask> task2) -> task1.getFirst() - task2.getFirst());
        List<UUID> currDeps;
        for (Pair<Integer, ITask> pair : pairs) {
            logInfo("Going through sorted pairs :: " + String.valueOf(pair.getFirst()) + " " + pair.getSecond().getDescription());
            if (pair.getFirst() != 0) {
                ITask task = pair.getSecond();
                logInfo("Starting Task :: " + task.getDescription());
                currDeps = task.getDependencies();
                logInfo("Deps :: " + currDeps.toString());
                for (UUID uuid : currDeps) {
                    ITask depTask = this.getTaskofUUID(uuid);
                    if (depTask != null) {
                        logInfo("Adding edge to :: " + depTask.getDescription());
                        this.addEdge(depTask, task);
                    }
                }
            }
        }

        List<ITask> noDeps = this.getTasksNoDeps(this.graph.vertexSet());
        this.recurseAndConnectSink(noDeps, sink);
        logInfo("Graph so far :: " + this.graph);

    }

    /**
     * This method performs a DFS on a list of tasks with no dependencies.
     * Upon reaching a task at the end with no further connections, it connects a sink vertex to it.
     * @param noDeps
     * @param sink
     */
    private void recurseAndConnectSink(List<ITask> noDeps, ITask sink) {
        for (ITask task : noDeps) {
            Iterator<ITask> iterator = new DepthFirstIterator<>(this.graph, task);
            ITask next = task;
            while (iterator.hasNext()) {
                next = iterator.next();
            }
            this.addEdge(next, sink);
        }
    }

    /**
     * Public method that lets the user reset the graph with a new set of tasks.
     * @param tasks
     */
    public void fromArrayofTasks(ITask[] tasks) {
        this.tasks = Arrays.asList(tasks);
        this.fromArrayofTasks(tasks);
    }

    /**
     * TODO implement criticalPath algorithm
     * @param graph
     * @param pathGraph
     * @return {@link Graph<ITask, DefaultEdge>}
     */
    private Graph<ITask, DefaultEdge> findCriticalPath(Graph<ITask, DefaultEdge> graph, Graph<ITask, DefaultEdge> pathGraph) {
        if (graph.vertexSet().isEmpty()) {
            return pathGraph;
        }
        // get all source tasks
        List<ITask> sources = this.getTasksNoDeps(graph.vertexSet());
        return this.newGraph();
    }

}
