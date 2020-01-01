package com.Scheduler.CriticalPath;

import java.util.*;
import java.util.stream.Collectors;

import com.common.Edge;
import com.common.ITask;
import com.common.Task;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;

import static com.common.Utils.logInfo;
import static com.common.GraphUtils.createGraphVis;
import static com.common.GraphUtils.addVertexToGraph;
import static com.common.GraphUtils.addEdgeToGraph;
import static com.common.GraphUtils.DFS;
import static com.common.GraphUtils.getSourceTask;
import static com.common.GraphUtils.cloneGraph;
import static com.common.GraphUtils.removeSourceSinkEdges;
import static com.common.GraphUtils.newGraph;

public class CriticalPath {

    private List<ITask> tasks;
    // The underlying graph structure of the class
    private Graph<ITask, Edge> graph = newGraph();


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
        return addVertexToGraph(this.graph, vertex);
    }



    /**
     * This private method returns a boolean if an edge is successfully added
     * @param task1
     * @param task2
     * @return {boolean}
     */
    private boolean addEdge(ITask task1, ITask task2) {
        return addEdgeToGraph(this.graph, task1, task2);
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
                addEdgeToGraph(graph, source, target);
            }
        }

        for (Edge edge: toRemove) graph.removeEdge(edge);
        graph.removeVertex(node);
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
            this.graph = newGraph();
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
        DFS(this.graph, source);

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



    public List<ITask> getSchedule() {
        Graph<ITask, Edge> graph = newGraph();
        cloneGraph(graph, this.graph);
        List<ITask> schedule = new ArrayList<>();
        ITask source = getSourceTask(graph, true);
        ITask sink = getSourceTask(graph, false);
        while (graph.vertexSet().size() > 2) {
            logInfo("getSchedule :: graph size :: " + graph.vertexSet().size());
            // check if second is not sink
            // List<ITask> criticalPath = this.findCriticalPathTMP(graph);
            Pair<Double, List<ITask>> pair = this.maximumPathDP(graph, source, sink);
            List<ITask> criticalPath = pair.getSecond();
            // The task to remove is always the second as the first is the source node
            // the length of this should always be at least 2 (source, sink)
            for (ITask task : criticalPath) logInfo("getSchedule :: list :: " + task.getDescription());
            ITask toRemove = criticalPath.get(1);
            logInfo("getSchedule :: toRemove :: " + toRemove.getDescription());
            if (!toRemove.isSink() && !toRemove.isSource()) {
                this.removeNoDepNode(graph, source, toRemove);
                schedule.add(toRemove);
            }
            removeSourceSinkEdges(graph);
        }

        logInfo("after schedule");
        createGraphVis(graph);

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
        ITask source = getSourceTask(graph, true);
        ITask sink = getSourceTask(graph, false);
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


    /**
     * This method recursively finds the longest path while keeping track of all previous longest paths it has visited
     * @param graph2
     * @param source
     * @param sink
     * @param nodeMap
     * @return
     */
    private Pair<Double, List<ITask>> maximumPathDP(Graph<ITask, Edge> graph2, ITask source, ITask sink, HashMap<ITask, Pair<Double, List<ITask>>> nodeMap) {
        if (source.equals(sink)) {
            logInfo("maximumPathDP :: base case");
            List<ITask> baseAnswer = new ArrayList<>();
            baseAnswer.add(source);
            return new Pair<Double,List<ITask>>(0.0, baseAnswer);
        }
        Set<Edge> incoming = graph2.incomingEdgesOf(sink);

        double maxDist = 0;
        List<ITask> currPath = null;
        Pair<Double, List<ITask>> tmp;
        for (Edge edge : incoming) {
            logInfo("maximumPathDP :: Analyzing edge :: " + graph2.getEdgeSource(edge).getDescription() + " -> " + graph2.getEdgeTarget(edge).getDescription());
            ITask intermediateSource = graph2.getEdgeSource(edge);
            if (nodeMap.containsKey(intermediateSource)) {
                tmp = nodeMap.get(intermediateSource);
            } else {
                tmp = maximumPathDP(graph2, source, intermediateSource, nodeMap);
            }
            if (graph2.getEdgeWeight(edge) == 0.0) {
                currPath = tmp.getSecond();
            } else if (tmp.getFirst() + graph2.getEdgeWeight(edge) > maxDist) {
                maxDist = tmp.getFirst() + graph2.getEdgeWeight(edge);
                currPath = tmp.getSecond();
            }
        }

        logInfo("maximumPathDP :: currPath :: " + (currPath == null));
        currPath.add(sink);
        Pair<Double, List<ITask>> pair = new Pair<Double,List<ITask>>(maxDist, currPath);
        nodeMap.put(sink, pair);
        return pair;

    }


    /**
     * This method calls a recursive method that finds the longest path
     */
    protected Pair<Double, List<ITask>> maximumPathDP(Graph<ITask, Edge> graph, ITask source, ITask sink) {
        HashMap<ITask, Pair<Double, List<ITask>>> nodeMap = new HashMap<>();
        return maximumPathDP(graph, source, sink, nodeMap);
    }


<<<<<<< HEAD
    /**
     * Helper function for makeMultiprocessorSchedule, finds the time to idle before the next task
     * @param workTimes
     * @return
     */
=======

>>>>>>> upstream/master
    private float findIdleTime(List<Float> workTimes) {
        // TODO: test if having 2 processors delaying at the same time still works with the main function
        float first = Float.MAX_VALUE;
        float second = first;
        for (int i = 0; i < workTimes.size(); i++) {
            if (first > workTimes.get(i)) {
                second = first;
                first = workTimes.get(i);
            }
        }
        return second;
    }

    /**
     * Helper function for makeMultiprocessorSchedule, finds the time to step based on work times
     * @param workTimes
     * @return
     */
    private float findStepTime(List<Float> workTimes) {
        float minT = Float.MAX_VALUE;
        for (int i = 0; i < workTimes.size(); i++) {
            if (minT > workTimes.get(i)) {
                minT = workTimes.get(i);
            }
        }
        return minT;
    }

<<<<<<< HEAD
    /**
     * Helper function for makeMultiprocessorSchedule, finds an available task based on priority and dependencies
     * @param orderedTask
     * @param dependencyMap
     * @return
     */
    private UUID findAvailableTask(List<ITask> orderedTask, Map<UUID, Set<UUID>> dependencyMap){
=======
    private UUID findAvailableTask(List<ITask> orderedTask, Map<UUID, Set<UUID>> dependencyMap) {
>>>>>>> upstream/master
        UUID currID;
        for (int i = 0; i < orderedTask.size(); i++) {
            currID = orderedTask.get(i).getTaskID();
            //if the task is not processing/processed and it has no dependencies:
            if (dependencyMap.get(currID) != null && dependencyMap.get(currID).size() == 0) {
                return currID;
            }
        }
        //if no available tasks exist, return null
        return null;
    }

<<<<<<< HEAD
    /**
     * creates a multiprocessor schedule represented by a list for each processor
     * @param orderedTask
     * @param numProcessors
     * @return
     */
    protected List <List <ITask>> makeMultiprocessorSchedule(List<ITask> orderedTask,  int numProcessors){
=======
    protected List <List <ITask>> makeMultiprocessorSchedule(List<ITask> orderedTask,  int numProcessors) {
>>>>>>> upstream/master

        List < List <ITask>> schedule = new ArrayList<>(numProcessors);
        //default cases
        if (numProcessors < 1) {
            return null;
        } else if (numProcessors == 1) {
            schedule.set(0, orderedTask);
            return schedule;
        }

        List <Float> workTime = new ArrayList<>(numProcessors);

        //set up workTimes
        for (int i = 0; i < numProcessors; i++) {
            workTime.add(0f);
        }
        //set up schedule
        for (int i = 0; i < numProcessors; i++) {
            schedule.add(new ArrayList<>());
        }

        //create HashMap: Task -> dependencies
        Map<UUID, Set<UUID>> dependencyMap = new HashMap<>();
        UUID currID;
        for (int i = 0; i < orderedTask.size(); i++) {
            currID = orderedTask.get(i).getTaskID();
            List<UUID> deps = orderedTask.get(i).getDependencies();
            dependencyMap.put(currID, new HashSet<>());
            for (int j = 0; j < deps.size(); j++) {
                dependencyMap.get(currID).add(deps.get(j));
            }
        }

        float currTime = 0;
        float minTime;
        while (dependencyMap.size() > 0) {
            //update all dependencies
            for (int i = 0; i < numProcessors; i++) {
                //check if processor is available
                if (currTime >= workTime.get(i)) {
                    //update hashMap with previous task completed
                    //if the last task was a task and not an idle
                    if(schedule.get(i).size() != 0 && schedule.get(i).get(schedule.get(i).size() - 1).isIdle() == false) {
                        //remove the task from all dependencies
                        UUID tempID = schedule.get(i).get(schedule.get(i).size() - 1).getTaskID();
                        for (Set<UUID> task : dependencyMap.values()) {
                            task.remove(tempID);
                        }
                    }
                }
            }

            //find an available task
            currID = this.findAvailableTask(orderedTask, dependencyMap);

            if (currID == null) {
                //find the time to wait (next lowest workTime)
                minTime = this.findIdleTime(workTime);
                //add a delay, find the lowest workTime and add the delay to that processor
                ITask tempTask = Task.idleTask(minTime);
                float tempMin = Float.MAX_VALUE;
                int tempIndex = -1;
                for (int i = 0; i < numProcessors; i++) {
                    if (tempMin > workTime.get(i)) {
                        tempMin = workTime.get(i);
                        tempIndex = i;
                    }
                }
                schedule.get(tempIndex).add(tempTask);
                workTime.set(tempIndex, workTime.get(tempIndex) + minTime);

            } else {
                //find the processor with the lowest work Time
                int tempIndex = -1;
                float tempMin = Float.MAX_VALUE;
                for (int i = 0; i < numProcessors; i++) {
                    if (tempMin > workTime.get(i)) {
                        tempMin = workTime.get(i);
                        tempIndex = i;
                    }
                }
                //assign the available task to that processor
                ITask tmpTask = null;
                for (ITask task : orderedTask) {
                    if (task.getTaskID().equals(currID)) {
                        tmpTask = task;
                    }
                }
                logInfo("makeMultiprocessorSchedule :: Assigning Task: " + tmpTask.getDescription() + " to Processor" + tempIndex);
                schedule.get(tempIndex).add(tmpTask);
                workTime.set(tempIndex, workTime.get(tempIndex) + tmpTask.getTime());
                dependencyMap.remove(currID);
            }
            //get the current time:
            currTime = this.findStepTime(workTime);
        }

        return schedule;
    }

    protected Pair<Double, List<ITask>> maximumPathDPOLD(Graph<ITask, Edge> graph, ITask source, ITask sink) {
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
}
