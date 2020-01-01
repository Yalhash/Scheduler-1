package com.common;

import static com.common.Utils.logInfo;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;

public class GraphUtils {

    /**
     * Creates a new {@link Graph} with vertices
     * that are of type {@link ITask}
     * @return Graph<ITask, DefaultEdge>
     */
    public static Graph<ITask, Edge> newGraph() {
        return new SimpleDirectedWeightedGraph<>(Edge.class);

    }

    /** Creates a .dot language representation of the current graph */
    public static void createGraphVis(Graph<ITask, Edge> graph) {
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


    public static boolean addVertexToGraph(Graph<ITask, Edge> graph, ITask vertex) {
        // this.graph.vertexSet().contains(vertex);
        ITask exists = graph.vertexSet().stream().filter(task -> task.getTaskID().equals(vertex.getTaskID())).findAny().orElse(null);
        if (exists != null) {
            return false;
        }
        return graph.addVertex(vertex);
    }


    public static boolean addEdgeToGraph(Graph<ITask, Edge> graph, ITask task1, ITask task2) {
        logInfo("adding edge :: " + task1.getDescription() + "->" + task2.getDescription());
        // create the edge and assign the weight to be the first task's time
        logInfo("addEdge :: firstTask " + (task1 == null));
        logInfo("addEdge :: secondTask " + (task2 == null));
        Edge edge = graph.addEdge(task1, task2);
        logInfo("addEdge :: edge weight :: " + task1.getTime());
        logInfo("addEdge :: edge null? " + (edge == null));
        if (edge != null) {
            graph.setEdgeWeight(edge, task1.getTime());
            return true;
        }
        return false;
    }

    public static void DFS(Graph<ITask, Edge> graph, ITask start) {
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


    /**
         * Returns the source vertex of the graph. This method is for testing purposes.
         * @return {@link ITask}
         */
    public static ITask getSourceTask(Graph<ITask, Edge> graph, boolean source) {
        return graph.vertexSet().stream().filter(task -> source ? task.isSource(): task.isSink()).findFirst().orElse(null);
    }


    public static void cloneGraph(Graph<ITask, Edge> destinationGraph, Graph<ITask, Edge> sourceGraph) {
        Set<Edge> sEdges = sourceGraph.edgeSet();
        ITask source;
        ITask target;
        for (Edge e : sEdges) {
            logInfo("cloneGraph :: edge :: "+ e.toString());
            source = sourceGraph.getEdgeSource(e);
            target = sourceGraph.getEdgeTarget(e);
            if (source != null && target != null) {
                addVertexToGraph(destinationGraph, source);
                addVertexToGraph(destinationGraph, target);
                addEdgeToGraph(destinationGraph, source, target);
            }
        }
        logInfo("cloned graph :: ");
        createGraphVis(destinationGraph);
    }


    /**
         * Removes all edges that directly connect source and sink (source -> sink)
         * @param graph
         */
    public static void removeSourceSinkEdges(Graph<ITask, Edge> graph) {
        ITask source = getSourceTask(graph, true);
        ITask sink = getSourceTask(graph, false);
        Set<Edge> edges = graph.getAllEdges(source, sink);

        for(Edge edge : edges) graph.removeEdge(edge);
    }


    /**
         * Returns the max edge weight for purpose of rescaling the edge weights.
         * @return double
         */
    public static double getMaximumEdgeWeight(Graph<ITask, Edge> graph) {
        Set<Edge> edges = graph.edgeSet();
        List<Double> times = new ArrayList<>();
        for (Edge edge: edges) times.add(graph.getEdgeWeight(edge));
        return Collections.max(times);
    }


    /**
         * NOTE: THIS METHOD DOES WORK, BUT MY LOGIC WAS OFF
         * FOR MAKING IT IN THE FIRST PLACE.
         * This method resacles a graph such that the edge weights can be used
         * with a shortest path algorithm to find the criticalPath (or longest path)
         *
         * graph does not allow for negative edges in their graph definitions as the
         * presence of a negative edge cycle means there can be no shortest path (the shortest path will effectively be -INF).
         * Instead, I find the largest edge weight, temporarily invert all edge weights, then add the largest edge weight to
         * every weight. This way the largest edge weight is transformed to 0 and is set as the base weight for which the other
         * weights are based on. So now, the largest edge weight is now the smallest (and positive!), so a shortest path algorithm
         * (like Djiktra's) can be used to find the longest (critical) path of the graph.
         * @param graph
         */
    public static void rescaleWeight(Graph<ITask, Edge> graph) {
        float maxEdgeWeight = (float) getMaximumEdgeWeight(graph);
        logInfo("rescaleWeight :: maxEdgeWeight :: " + maxEdgeWeight);
        for (Edge edge: graph.edgeSet()) {
            float tmp = (float) graph.getEdgeWeight(edge);
            float negate = -tmp;
            float newWeight = maxEdgeWeight + negate;
            graph.setEdgeWeight(edge, newWeight);
        }
    }

    /**
     * NOTE This method DOES NOT WORK
     * My logic when making this was off
     * @param graph
     * @param pathGraph
     * @return {@link Graph<ITask, DefaultEdge>}
     */
    public static List<ITask> findCriticalPath(Graph<ITask, Edge> graph) {
        // get all source tasks
        // List<ITask> sources = this.getTasksNoDeps(graph.vertexSet());
        logInfo("findCriticalPath :: ");
        DijkstraShortestPath<ITask, Edge> shortestPath = new DijkstraShortestPath(graph);
        ITask source = getSourceTask(graph, true);
        ITask sink = getSourceTask(graph, false);
        List<ITask> shortestPathGraph = shortestPath.getPath(source, sink).getVertexList();
        for (ITask task : shortestPathGraph) logInfo("findCriticalPath :: task vertex :: " + task.getDescription());
        return shortestPathGraph;
    }
}
