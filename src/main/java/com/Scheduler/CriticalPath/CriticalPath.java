package com.Scheduler.CriticalPath;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.common.Edge;
import com.common.ITask;
import com.common.Task;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;

import static com.common.Utils.logInfo;

public class CriticalPath {

    private List<ITask> tasks;
    // The underlying graph structure of the class
    private Graph<ITask, Edge> graph = this.newGraph();


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

    /**
     * @return the graph
     */
    public Graph<ITask, Edge> getGraph() {
        return graph;
    }


    /** Creates a .dot language representation of the current graph */
    protected void createGraphVis(Graph<ITask, Edge> graph) {
        if (graph != null) {

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

            GraphExporter<ITask, Edge> exporter = new DOTExporter<>(vertexIDProvider, labelIDProvider, null);
            Writer writer = new StringWriter();
            try {
                exporter.exportGraph(graph, writer);
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
    private Graph<ITask, Edge> newGraph() {
        return new SimpleDirectedWeightedGraph<>(Edge.class);

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

        logInfo("adding edge :: " + task1.getDescription() + "->" + task2.getDescription());
        // create the edge and assign the weight to be the first task's time
        Edge edge = this.graph.addEdge(task1, task2);
        this.graph.setEdgeWeight(edge, task1.getTime());
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
            if (!task.isSink() && !task.isSource() && task.getDependencies().isEmpty()) {
                noDeps.add(task);
            }
        }
        return noDeps;
    }

    protected void removeNoDepNode(Graph<ITask, Edge> graph, ITask source, ITask node) {
        // NOTE: it is assumed that node has no dependencies and thus
        // has only one incoming edge: from source
        Set<Edge> outgoingEdges = graph.outgoingEdgesOf(node);
        ITask target;
        List<Edge> toRemove = new ArrayList<>();
        for (Edge edge: outgoingEdges) {
            // target = (ITask)edge.getTargetPublic();
            target = graph.getEdgeTarget(edge);
            logInfo(target.getDescription());
            if (target != null) {
                toRemove.add(edge);
                this.addEdge(source, target);
            }
        }

        for (Edge edge: toRemove) this.graph.removeEdge(edge);
        this.graph.removeVertex(node);
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
        ITask sink = Task.sinkTask(true);
        this.addVertex(sink);

        // add source task
        ITask source = Task.sinkTask(false);
        this.addVertex(source);


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
        this.connectSource(noDeps, source);
        this.recurseAndConnectSink(noDeps, sink);
        // logInfo("Graph so far :: " + this.graph);
        // this.rescaleWeight(this.graph);
        // for (ITask start : noDeps) this.DFS(this.graph, start);
        this.DFS(this.graph, source);

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
     * Returns the source vertex of the graph. This method is for testing purposes.
     * @return {@link ITask}
     */
    protected ITask getSourceTask(Graph<ITask, Edge> graph, boolean source) {
        return graph.vertexSet().stream().filter(task -> source ? task.isSource(): task.isSink()).findFirst().orElse(null);
    }

    private void connectSource(List<ITask> noDeps, ITask source) {
        logInfo("connect source");
        for (ITask task: noDeps) {
            this.addEdge(source, task);
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
     * Removes all edges that directly connect source and sink (source -> sink)
     * @param graph
     */
    private void removeSourceSinkEdges(Graph<ITask, Edge> graph) {
        ITask source = this.getSourceTask(graph, true);
        ITask sink = this.getSourceTask(graph, false);
        Set<Edge> edges = graph.getAllEdges(source, sink);

        for(Edge edge : edges) graph.removeEdge(edge);
    }

    public List<ITask> getSchedule() {
        Graph<ITask, Edge> graph = this.graph;
        List<ITask> schedule = new ArrayList<>();
        ITask source = this.getSourceTask(graph, true);
        ITask sink = this.getSourceTask(graph, false);
        while (graph.vertexSet().size() > 2) {
            // check if second is not sink
            // List<ITask> criticalPath = this.findCriticalPathTMP(graph);
            Pair<Double, List<ITask>> pair = this.maximumPathDP(graph, source, sink);
            List<ITask> criticalPath = pair.getSecond();
            // The task to remove is always the second as the first is the source node
            // the length of this should always be at least 2 (source, sink)
            for (ITask task : criticalPath) logInfo("getSchedule :: list :: " + task.getDescription());
            ITask toRemove = criticalPath.get(1);
            if (!toRemove.isSink() && !toRemove.isSource()) {
                this.removeNoDepNode(graph, source, toRemove);
                schedule.add(toRemove);
            }
            this.removeSourceSinkEdges(graph);
        }

        return schedule;
    }

    /**
     * Temporary solution to finding the criticalPath of a DAG as the first attempt
     * is currently not working.
     * This method uses jgrapht's {@link AllDirectedPaths} class to get all paths from
     * source to sink and then sorts the result.
     * @param graph
     * @return
     */
    protected List<ITask> findCriticalPathTMP(Graph<ITask, Edge> graph) {
        ITask source = this.getSourceTask(graph, true);
        ITask sink = this.getSourceTask(graph, false);
        AllDirectedPaths<ITask, Edge> allPaths = new AllDirectedPaths<>(graph);
        List<GraphPath<ITask, Edge>> path = allPaths.getAllPaths(source, sink, true, null)
                                            .stream()
                                            .sorted((GraphPath<ITask, Edge> g1, GraphPath<ITask, Edge> g2) -> new Double(g2.getWeight()).compareTo(g1.getWeight())).collect(Collectors.toList());
        path.forEach(p -> {
            logInfo("PATH :: " + p.getWeight());
            List<ITask> pp = p.getVertexList();
            pp.forEach(task -> logInfo("TASK :: " + task.getDescription()));
        });
        GraphPath<ITask, Edge> p = path.get(0);
        return p.getVertexList();
    }

    protected Pair<Double, List<ITask>> maximumPathDP(Graph<ITask, Edge> graph, ITask source, ITask sink) {
        if (source.equals(sink)) {
            logInfo("maximumPathDP :: base case");
            List<ITask> asdf = new ArrayList<>();
            asdf.add(source);
            return new Pair<Double,List<ITask>>(0.0, asdf);
        }
        Set<Edge> incoming = graph.incomingEdgesOf(sink);

        double maxDist = 0;
        List<ITask> currPath = null;
        Pair<Double, List<ITask>> tmp;
        for (Edge edge : incoming) {
            logInfo("maximumPathDP :: Analyzing edge :: " + graph.getEdgeSource(edge).getDescription() + " -> " + graph.getEdgeTarget(edge).getDescription());
            ITask intermediateSource = graph.getEdgeSource(edge);
            tmp = maximumPathDP(graph, source, intermediateSource);

            if (graph.getEdgeWeight(edge) == 0.0) {
                currPath = tmp.getSecond();
            } else if (tmp.getFirst() + graph.getEdgeWeight(edge) > maxDist) {
                maxDist = tmp.getFirst() + graph.getEdgeWeight(edge);
                currPath = tmp.getSecond();
            }
        }

        currPath.add(sink);



        Pair<Double, List<ITask>> pair = new Pair<Double,List<ITask>>(maxDist, currPath);
        return pair;

    }

    /**
     * TODO implement criticalPath algorithm
     * @param graph
     * @param pathGraph
     * @return {@link Graph<ITask, DefaultEdge>}
     */
    protected List<ITask> findCriticalPath(Graph<ITask, Edge> graph) {
        // get all source tasks
        // List<ITask> sources = this.getTasksNoDeps(graph.vertexSet());
        logInfo("findCriticalPath :: ");
        DijkstraShortestPath<ITask, Edge> shortestPath = new DijkstraShortestPath(graph);
        ITask source = this.getSourceTask(graph, true);
        ITask sink = this.getSourceTask(graph, false);
        List<ITask> shortestPathGraph = shortestPath.getPath(source, sink).getVertexList();
        for (ITask task : shortestPathGraph) logInfo("findCriticalPath :: task vertex :: " + task.getDescription());
        return shortestPathGraph;
    }

    /**
     * Returns the max edge weight for purpose of rescaling the edge weights.
     * @return float
     */
    protected double getMaximumEdgeWeight(Graph<ITask, Edge> graph) {
        Set<Edge> edges = graph.edgeSet();
        List<Double> times = new ArrayList<>();
        for (Edge edge: edges) times.add(graph.getEdgeWeight(edge));
        return Collections.max(times);
    }

    /**
     * This method resacles a graph such taht the edge weights can be used
     * with a shortest path algorithm to find the criticalPath (or longest path)
     *
     * Jgrapht does not allow for negative edges in their graph definitions as the
     * presence of a negative edge cycle means there can be no shortest path (the shortest path will effectively be -INF).
     * Instead, I find the largest edge weight, temporarily invert all edge weights, then add the largest edge weight to
     * every weight. This way the largest edge weight is transformed to 0 and is set as the base weight for which the other
     * weights are based on. So now, the largest edge weight is now the smallest (and positive!), so a shortest path algorithm
     * (like Djiktra's) can be used to find the longest (critical) path of the graph.
     * @param graph
     */
    protected void rescaleWeight(Graph<ITask, Edge> graph) {
        float maxEdgeWeight = (float) this.getMaximumEdgeWeight(graph);
        logInfo("rescaleWeight :: maxEdgeWeight :: " + maxEdgeWeight);
        Set<Edge> edges = graph.edgeSet();
        for (Edge edge: graph.edgeSet()) {
            float tmp = (float) graph.getEdgeWeight(edge);
            float negate = -tmp;
            float newWeight = maxEdgeWeight + negate;
            graph.setEdgeWeight(edge, newWeight);
        }
    }

    private void DFS(Graph<ITask, Edge> graph, ITask start) {
        Iterator<ITask> iterator = new DepthFirstIterator<>(graph, start);
        ITask prev = null;
        ITask next = start;
        while (iterator.hasNext()) {
            prev = next;
            next = iterator.next();
            logInfo("DFS :: " + next.getDescription());
            logInfo("DFS :: prev -> next :: " + prev.getDescription() + "->" + next.getDescription());
            if (prev != null && !prev.isSink() && prev != next) {
                Edge edge = graph.getEdge(prev, next);
                logInfo("DFS :: Edge weight :: " + graph.getEdgeWeight(edge));
            }
        }
        logInfo("DFS end :: " + next.getDescription());
    }

}
