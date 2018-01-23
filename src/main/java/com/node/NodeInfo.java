package com.node;

import java.util.HashSet;

/*
 * created by divya at 1/20/2018
 */
public class NodeInfo {
   private String id;
   private boolean active;
   private String host;
   private int port;
   private HashSet<NodeInfo> neighbourSet = new HashSet<NodeInfo>();

   // parameterized constructor
   public NodeInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public HashSet<NodeInfo> getNeighbourSet(){
       return neighbourSet;
    }

    public void setNeighbourSet(HashSet<NodeInfo> set){
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

    @Override
    public String toString() {
        return "NodeInfo{" +
                "id='" + id + '\'' +
                ", active=" + active +
                ", host='" + host + '\'' +
                ", port=" + port + ", neighbour=" + neighbourSet +
                '}';
    }
}
