package node;
//ECE50862 Project 1
//Java code for the network graph and widest path algorithm ver1.
//Ashley Gregg

import java.util.*;

public class graph {
	private int order;//Total number of nodes
	private int totPaths;//Total number of paths (or size)
	private HashMap<String, Path> origPaths;
	private HashMap<String, Path> workingPaths;
	
	public graph(int order, int totPaths, HashMap<String, Path> pathHashMap ) {
		this.order = order;
		this.totPaths = totPaths;
		this.origPaths = pathHashMap;
		this.workingPaths = pathHashMap;
	}
	//Define all types of variables to be used/////////////////////////////	
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
    
    public HashMap<String, Path> getOrigPaths() {
        return origPaths;
    }
    public void setOrigPaths(HashMap<String, Path> origPaths) {
        this.origPaths = origPaths;
    }
    public HashMap<String, Path> getWorkingPaths() {
        return workingPaths;
    }
    public void setWorkingPaths(HashMap<String, Path> workingPaths) {
        this.workingPaths = workingPaths;
    }
    ////////////////////////////////////////////////////// 
    
    
    //Calculates the widest path for the graph/network
    public HashMap<String, Path> computeWidestPath() {
    	HashMap<String, Path> pathHashMap = new HashMap<String, Path>();
    	this.workingPaths = this.getOrigPaths();
    	// Number of nodes - 1 = minimum spanning tree number of paths
    	int pathMax = this.getOrder() - 1;
    	List<Path> listOfPaths = sortPaths(this.workingPaths);
    	
    	List<Integer> connectedNodes= new ArrayList<Integer>();
    	
    	//Largest bandwidth path will always be in the network (assuming number of paths > 0)
    	pathHashMap.put(listOfPaths.get(0).getPathId(), listOfPaths.get(0));
    	connectedNodes.add(listOfPaths.get(0).getNode1());
    	connectedNodes.add(listOfPaths.get(0).getNode2());
    	//need to loop through by highest bandwidth)
		//check one node is allready connected - to prevent multiple, split networks
		//check one node is not connected: If this is the case then connect and restart loop.
		//when total path reaches the max limit (nodes-1) stop as done
    	for (int i = 0 ; i < pathMax; i++) {
    		for (int pathNumber = 1; pathNumber < this.gettotpaths() + 1; pathNumber++) {
    			if (nodeNeedsConnecting(listOfPaths.get(pathNumber), connectedNodes) == true) {
    				pathHashMap.put(listOfPaths.get(pathNumber).getPathId(), listOfPaths.get(pathNumber));
    		    	connectedNodes.add(listOfPaths.get(pathNumber).getNode1());
    		    	connectedNodes.add(listOfPaths.get(pathNumber).getNode2());
    				break;
    			}
    		}
    	}
    	System.out.println("Minimum spanning tree paths: "+Arrays.asList(pathHashMap));//for debugging purposes
    	return pathHashMap;
    	
    }   
    
    //Comparator based on Bandwidth of Path -needed to sort lists
    public class bandWidthComp implements Comparator<Path> {
    	  @Override
    	  public int compare(Path a, Path b) {
    	    return Integer.compare(a.getBandwidth(), b.getBandwidth());
    	  }
    	}
    
    //sort Paths and reverse so in descending order
    public List<Path> sortPaths(HashMap<String, Path> origPaths)
	{
    	List<Path> sortedList = new ArrayList<Path>();
    	Collections.sort(sortedList, new bandWidthComp());
    	Collections.reverse(sortedList);
		return sortedList;
	}
    
    //check one node is allready connected - to prevent multiple, split networks
  	//check one node is not connected: If this is the case then connect and restart loop.
    private boolean nodeNeedsConnecting(Path path, List<Integer> connectedNodes) {
    	int node1 = path.getNode1();
    	int node2 = path.getNode1();
    	int NodesConnected = 0;
    	//Must be done seperately to determine if seperate ends are connected
    	for (Integer x : connectedNodes) {
    		if (x==node1) {
    			NodesConnected++;
    			break;
    		}
    	}
		for (Integer x : connectedNodes) {
    		if (x==node2) {
    			NodesConnected++;
    			break;
    		}	
    	}
		//Exactly one node in network, the other isn't so needs to be added
    	if (NodesConnected==1)
    		return true;
    	//2 nodes so not wanted, or zero causing split networks (link may be incorporated later if it is correct to do so)
    	else
    		return false;
    }

}
    
