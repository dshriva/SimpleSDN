package com.domain;

import java.util.HashSet;
import static com.util.NetworkConstants.LINK;

/*
 * created by divya at 1/28/2018
 * updated by Ashley at 2/4/2018
 */

public class Path {
    private String pathId;
    private int bandwidth;
    private int delay;
    private boolean usable;
    private HashSet<String> vertexSet = new HashSet<String>(2);

    public Path(int bandwidth, int delay, String id1, String id2,boolean usable) {
        this.pathId = id1 + LINK + id2;
        this.bandwidth = bandwidth;
        this.delay = delay;
        vertexSet.add(id1);
        vertexSet.add(id2);
        this.usable = usable;
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

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }

    @Override
    public String toString() {
        return "Path{" +
                "pathId='" + pathId + '\'' +
                ", bandwidth=" + bandwidth +
                ", delay=" + delay +
                ", usable=" + usable +
                '}';
    }
}


