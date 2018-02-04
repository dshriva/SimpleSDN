package node;

import java.util.HashSet;

/*
 * created by divya at 1/28/2018
 * updated by Ashley at 2/4/2018
 */
public class Path {
    private String pathId;
    private int node1;
    private int node2;
    private int bandwidth;
    private int delay;
    private HashSet<String> vertexSet = new HashSet<String>(2);

    public Path(int bandwidth, int delay, String id1, String id2) {
    	this.node1 = Integer.parseInt(id1);
    	this.node2 = Integer.parseInt(id2);
        this.pathId = id1 + "<->" + id2;
        this.bandwidth = bandwidth;
        this.delay = delay;
        vertexSet.add(id1);
        vertexSet.add(id2);
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public HashSet<String> getVertexSet() {
        return vertexSet;
    }
    
    public int getNode1() {
        return node1;
    }
    
    public int getNode2() {
        return node2;
    }
}
