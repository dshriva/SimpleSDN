package com.node;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * created by divya at 1/20/2018
 */
public class NodeInfo implements Serializable {
    private String id;
    private boolean active;
    private String host;
    private int port;
    private HashSet<NodeInfo> neighbourSet = new HashSet<NodeInfo>();
    private Set<String> neighborIdSet =  new HashSet<String>();

    // parameterized constructor
    public NodeInfo(String id) {
        this.id = id;
    }

    public HashSet<NodeInfo> getNeighbourSet() {
        return neighbourSet;
    }

    public void setNeighbourSet(HashSet<NodeInfo> set) {
        this.neighbourSet = set;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getNeighbourIds() {
        List<String> neighbourIds = new ArrayList<String>();
        for(NodeInfo node : neighbourSet) {
            neighbourIds.add(node.getId());
        }
        return neighbourIds;
    }

    public Set<String> getNeighborIdSet() {
        return neighborIdSet;
    }

    public void setNeighborIdSet(Set<String> neighborIdSet) {
        this.neighborIdSet = neighborIdSet;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "id='" + id + '\'' +
                ", active=" + active +
                ", host='" + host + '\'' +
                ", port=" + port + '\'' +
                ", neighbourSet=" + this.getNeighbourIds().toString() +
                '}';
    }
}
