package com.domain;
//ECE50862 Project 1
//Java code for the network graph and widest path algorithm ver1.
//Ashley Gregg
// Refactored by divya (2/5/2018)

import com.util.NetworkConstants;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class Graph {
    private static int order = 0;//Total number of nodes
    private static int totPaths = 0;//Total number of paths (or size)
    private HashMap<String, Path> activePathMap = new HashMap<String, Path>();
    private HashMap<String, NodeInfo> activeNodeMap = new HashMap<String, NodeInfo>();
    public static Logger LOGGER = Logger.getLogger(String.valueOf(Graph.class));

    public Graph(HashMap<String, NodeInfo> origNodes, HashMap<String, Path> origPath ) {
        setOrderAndPaths(origNodes, origPath);
    }

    private void setOrderAndPaths(HashMap<String, NodeInfo> origNodes, HashMap<String, Path> origPath) {
        for(NodeInfo node : origNodes.values()) {
            if(node.isActive()) {
                this.activeNodeMap.put(node.getId(), node);
            }
        }
        order = activeNodeMap.size();

        for(Path path : origPath.values()) {
            if(path.isUsable()) {
                this.activePathMap.put(path.getPathId(), path);
            }
        }
        totPaths = activePathMap.size();
    }

    //Define all types of variables to be used
    public int getOrder() {
        return order;
    }
    public void setOrder(int order) {
        this.order = order;
    }

    public int gettotpaths() {
        return totPaths;
    }
    public void setTotPaths(int totPaths) {
        this.totPaths = totPaths;
    }

    public static void main(String[] args) {
        Controller c = new Controller(1000);
        try {
            c.readConfigFile();
            for(NodeInfo n : Controller.nodeInfoHashMap.values())
                n.setActive(true);
            for(Path p : Controller.pathHashMap.values())
                p.setUsable(true);
            Graph g = new Graph(Controller.nodeInfoHashMap, Controller.pathHashMap);
            g.computeWidestPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Calculates the widest path for the graph/network
    public HashMap<String, Path> computeWidestPath() {
        LOGGER.debug("Entering the method: Graph.computeWidestPath");
        LinkedHashMap<String, Path> pathHashMap = new LinkedHashMap<String, Path>();
        // Number of nodes - 1 = minimum spanning tree number of paths
        int maxNode = this.getOrder() - 1;
        List<Path> listOfPaths = sortPaths(activePathMap);

        if(listOfPaths.isEmpty()) {
            return pathHashMap;
        }

        //Largest bandwidth path will always be in the network (assuming number of paths > 0)
        Set<String> connectedNodes= new HashSet<String>(listOfPaths.get(0).getVertexSet());

        pathHashMap.put(listOfPaths.get(0).getPathId(), listOfPaths.get(0));

        //need to loop through by highest bandwidth)
        //check one node is already connected - to prevent multiple, split networks
        //check one node is not connected: If this is the case then connect and restart loop.
        //when total path reaches the max limit (nodes-1) stop as done
        for (int i = 0 ; i < maxNode; i++) {
            for (int j = 1; j < listOfPaths.size(); j++){
                Path path = listOfPaths.get(j);
                String pathId = path.getPathId();
                String[] nodes = pathId.split(NetworkConstants.LINK);
                if (nodeNeedsConnecting(connectedNodes, nodes[0], nodes[1])) {
                    pathHashMap.put(path.getPathId(), path);
                    connectedNodes.add(nodes[0]);
                    connectedNodes.add(nodes[1]);
                    break;
                }
            }
        }
        LOGGER.info("Minimum spanning tree paths: ");//for debugging purposes
        for(Path path : pathHashMap.values()) {
            LOGGER.info(path.toString());
        }
        LOGGER.debug("Exiting the method: Graph.computeWidestPath");
        return pathHashMap;

    }

    //Comparator based on Bandwidth of Path -needed to sort lists
    public class PathComp implements Comparator<Path> {
        public int compare(Path a, Path b) {
            if(a.getBandwidth() < b.getBandwidth())
                return 1;
            else if (a.getBandwidth() > b.getBandwidth())
                return -1;
            else {
                if(a.getDelay() > b.getDelay()) return 1;
                else return -1;
            }
        }
    }

    //sort Paths and reverse so in descending order
    private List<Path> sortPaths(HashMap<String, Path> origPaths) {
        List<Path> sortedList = new ArrayList<Path>();
        for(Path p : origPaths.values()) {
            if(p.isUsable())
                sortedList.add(p);
        }
        Collections.sort(sortedList, new PathComp());
        //Collections.reverse(sortedList);
        return sortedList;
    }

    //check one node is already connected - to prevent multiple, split networks
    //check one node is not connected: If this is the case then connect and restart loop.
    private boolean nodeNeedsConnecting(Set<String> connectedNodes, String node1, String node2) {

        int NodesConnected = 0;
        //Must be done separately to determine if seperate ends are connected
        if(connectedNodes.contains(node1)) {
            NodesConnected++;
        }

        if(connectedNodes.contains(node2)) {
            NodesConnected++;
        }

        //Exactly one node in network, the other isn't so needs to be added
        if (NodesConnected==1)
            return true;
            //2 nodes so not wanted, or zero causing split networks (link may be incorporated later if it is correct to do so)
        else
            return false;
    }

}