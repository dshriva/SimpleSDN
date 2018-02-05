package com.domain;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;

import static com.util.NetworkConstants.*;

/*
 * created by divya at 1/17/2018
 */
public class Controller {
    private static String machinePort = "";
    private static String controllerIP = "";
    private static HashMap<String, NodeInfo> nodeInfoHashMap = new HashMap<String, NodeInfo>();
    private static HashMap<String, Path> pathHashMap = new HashMap<String, Path>();
    private static DatagramSocket controllerSocket = null;
    public static Logger LOGGER = Logger.getLogger(String.valueOf(Controller.class));


    public Controller(int port) {

    }

    public void readConfigFile() throws IOException {
        String fileName = "config_map.txt";
        String line = null;
        BufferedReader bufferedReader = null;
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                i++;
                if (i > 1) {
                    String[] keywords = line.split(" ");
                    String switchId1 = keywords[0];
                    String switchId2 = keywords[1];

                    NodeInfo nodeInfo1 = null;
                    NodeInfo nodeInfo2 = null;

                    if (nodeInfoHashMap.containsKey(switchId1)) {
                        nodeInfo1 = nodeInfoHashMap.get(switchId1);
                    } else {
                        nodeInfo1 = new NodeInfo(switchId1);
                    }
                    if (nodeInfoHashMap.containsKey(switchId2)) {
                        nodeInfo2 = nodeInfoHashMap.get(switchId2);
                    } else {
                        nodeInfo2 = new NodeInfo(switchId2);
                    }
                    nodeInfo1.getNeighbourSet().add(nodeInfo2);
                    nodeInfo2.getNeighbourSet().add(nodeInfo1);

                    nodeInfoHashMap.put(nodeInfo1.getId(), nodeInfo1);
                    nodeInfoHashMap.put(nodeInfo2.getId(), nodeInfo2);

                    Path path = new Path(Integer.parseInt(keywords[2]), Integer.parseInt(keywords[3]), switchId1, switchId2, false);
                    pathHashMap.put(path.getPathId(), path);
                }
                System.out.println(line);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + fileName + "'");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void createSocket(int port) {
        // creating a socket header
        try {
            controllerSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void messageExchange(int port) throws IOException {
        try {
            while (true) {
                try {
                    //receiving register request message from switch
                    byte[] b = new byte[1024];
                    DatagramPacket incomingData = new DatagramPacket(b, b.length);
                    controllerSocket.receive(incomingData);
                    InetAddress incomingAddress = incomingData.getAddress();
                    byte[] incomingMessage = incomingData.getData();

                    ByteArrayInputStream byteIn = new ByteArrayInputStream(incomingMessage);
                    ObjectInputStream in = new ObjectInputStream(byteIn);
                    HashMap responseHashMap = (HashMap) in.readObject();
                    processResponse(responseHashMap, incomingData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("Exiting...");
            controllerSocket.close();
        }

    }

    private void processResponse(HashMap responseHashMap, DatagramPacket incomingData) {
        try {
            if (responseHashMap.containsKey(REGISTER_REQUEST_MESSAGE)) {
                handleRegisterRequestMessage(responseHashMap, incomingData);
            } else if (responseHashMap.containsKey(TOPOLOGY_UPDATE_MESSAGE)) {
                handleTopologyUpdateMessage(responseHashMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleTopologyUpdateMessage(HashMap responseHashMap) {
        NodeInfo senderSwitchNode = (NodeInfo) responseHashMap.get(TOPOLOGY_UPDATE_MESSAGE);
        String senderSwitchId = senderSwitchNode.getId();
        nodeInfoHashMap.get(senderSwitchId).setLastSeenAt(System.currentTimeMillis());
        for (Object set : responseHashMap.entrySet()) {
            Map.Entry<String, NodeInfo> entrySet = (Map.Entry<String, NodeInfo>) set;
            String switchId = entrySet.getKey();
            NodeInfo switchNode = entrySet.getValue();
            if (switchNode.isActive()) {
                if (pathHashMap.get(switchId + LINK + senderSwitchId) != null) {
                    pathHashMap.get(switchId + LINK + senderSwitchId).setUsable(true);
                } else if (pathHashMap.get(senderSwitchId + LINK + switchId) != null) {
                    pathHashMap.get(senderSwitchId + LINK + switchId).setUsable(true);
                }
            } else {
                if (pathHashMap.get(switchId + LINK + senderSwitchId) != null) {
                    pathHashMap.get(switchId + LINK + senderSwitchId).setUsable(false);
                } else if (pathHashMap.get(senderSwitchId + LINK + switchId) != null) {
                    pathHashMap.get(senderSwitchId + LINK + switchId).setUsable(false);
                }
            }
        }
        computeWidestPath();
    }

    private void computeWidestPath() {
        Graph graph = new Graph(nodeInfoHashMap.size(), pathHashMap.size(), pathHashMap);
        HashMap<String, Path> map = graph.computeWidestPath();
        for(Map.Entry<String, Path> entrySet : map.entrySet()) {
            LOGGER.info("Link Info : "+entrySet.getValue());
        }
    }

    private void handleRegisterRequestMessage(HashMap responseHashMap, DatagramPacket regRequest) throws IOException {
        System.out.println("register request from switch received");
        String switchId = (String) responseHashMap.get(REGISTER_REQUEST_MESSAGE);
        String switchIpAddress = String.valueOf(regRequest.getAddress());
        if (switchIpAddress.startsWith("/")) {
            switchIpAddress = switchIpAddress.substring(1);
        }
        int switchPort = regRequest.getPort();
        HashMap<String, NodeInfo> retMap = updateNodeInfoHashMap(switchId, switchIpAddress, switchPort);

        //function to construct domain info list
        //Now sending the response to the switch
        if (retMap != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);
            objOpStream.writeObject(retMap);
            int length = 0;
            byte[] buf = null;
            buf = byteArrayOutputStream.toByteArray();
            length = buf.length;
            DatagramPacket response = new DatagramPacket(buf, length, regRequest.getAddress(), (regRequest.getPort()));
            controllerSocket.send(response);
            System.out.println("register response to switch sent");
        }
    }

    private HashMap<String, NodeInfo> updateNodeInfoHashMap(String switchId, String switchHost, int switchPort) {
        if (nodeInfoHashMap.containsKey(switchId)) {
            NodeInfo currNode = nodeInfoHashMap.get(switchId);
            currNode.setActive(true);
            currNode.setHost(switchHost);
            currNode.setPort(switchPort);
            currNode.setLastSeenAt(System.currentTimeMillis());
            //return the neighbor set
            HashMap<String, NodeInfo> retHashMap = new HashMap<String, NodeInfo>();
            retHashMap.put(REGISTER_RESPONSE_MESSAGE, null);
            for (NodeInfo neighborNodeInfo : currNode.getNeighbourSet()) {
                retHashMap.put(neighborNodeInfo.getId(), neighborNodeInfo);
                if (neighborNodeInfo.isActive()) {
                    // set the path as usable
                    if (pathHashMap.containsKey(neighborNodeInfo.getId() + LINK + currNode.getId())) {
                        pathHashMap.get(neighborNodeInfo.getId() + LINK + currNode.getId()).setUsable(true);
                    } else if (pathHashMap.containsKey(currNode.getId() + LINK + neighborNodeInfo.getId())) {
                        pathHashMap.get(currNode.getId() + LINK + neighborNodeInfo.getId()).setUsable(true);
                    }
                }
            }
            return retHashMap;
        }
        return null;
    }

    static TimerTask failureDetection() {
        TimerTask failureDetectionThread = new TimerTask() {
            @Override
            public void run() {
                try {
                    //Need keep alive byte to be consistent across switches
                    System.out.println("\n\n ------------------------------- ");
                    System.out.println(" Detecting failures");
                    LOGGER.debug("Detecting Failures");
                    for (Map.Entry<String, NodeInfo> entrySet : nodeInfoHashMap.entrySet()) {
                        long currentTime = System.currentTimeMillis();
                        String currNodeId = entrySet.getKey();
                        if ((currentTime - (M * K)) > entrySet.getValue().getLastSeenAt()) {
                            if(entrySet.getValue().isActive()) {
                                System.out.println("Marking Switch "+entrySet.getKey()+" as dead");
                                entrySet.getValue().setActive(false);
                            }
                            //System.out.println("Marking the following paths as unreachable");
                            for(Map.Entry<String, Path> eSet : pathHashMap.entrySet()) {
                                Path path = eSet.getValue();
                                if(path.getVertexSet().contains(currNodeId)){
                                    //System.out.println("Path Id: "+path.getPathId());
                                    if(path.isUsable()) {
                                        LOGGER.fatal("Marking the link"+path.getPathId()+" as unreachable");
                                        System.out.println("Marking the link"+path.getPathId()+" as unreachable");
                                        path.setUsable(false);
                                    }
                                }
                            }
                        }
                    }
                    System.out.println(" ------------------------------- \n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        return failureDetectionThread;
    }

    static TimerTask displayNodeInfoPathInfo() {
        TimerTask displayNodePathInfoThread = new TimerTask() {
            @Override
            public void run() {
                try {
                    //Need keep alive byte to be consistent across switches
                    System.out.println("\n\n ------------------------------- ");
                    System.out.println(" Displaying Switches");
                    for (Map.Entry<String, NodeInfo> entrySet : nodeInfoHashMap.entrySet()) {
                        System.out.println(entrySet.getValue());
                    }
                    System.out.println(" ------------------------------- ");

                    System.out.println(" ------------------------------- ");
                    System.out.println(" Displaying Links");
                    for (Map.Entry<String, Path> entrySet : pathHashMap.entrySet()) {
                        System.out.println(entrySet.getValue());
                    }
                    System.out.println(" ------------------------------- \n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        return displayNodePathInfoThread;
    }

    public static String[] parseString(String str) { //function to split the string in order to serve the purpose of activating the switch

        String split[] = str.split("\\:");
        return split;

    }

    public static List<NodeInfo> constructNodeInfoList(String splittedString, String switchId) {

        //creating domain (Switch) information list
        List<NodeInfo> nodeInfoList = new ArrayList<NodeInfo>();
        NodeInfo nodeInfo1 = new NodeInfo(switchId);
        if (splittedString.equalsIgnoreCase("register me")) {
            nodeInfo1.setActive(true); //setting switch as active
        }
        nodeInfoList.add(nodeInfo1);

        return nodeInfoList;
    }

    public static void displayNodeInfo(List<NodeInfo> nodeInfoList) {
        for (int i = 0; i < nodeInfoList.size(); i++) {
            System.out.println(nodeInfoList.get(i).toString());
        }
    }

    public void activateNeighbors(NodeInfo node) {
        for (NodeInfo node1 : node.getNeighbourSet()) {
            node1.setActive(true);
        }
    }

    public void activateNeighbors2(Set<NodeInfo> set) {
        for (NodeInfo node : set) {
            node.setActive(true);
        }
    }

    public void activateNeighbors3(Set<String> nodeIdSet) { // 2,4,6
        if (nodeIdSet != null) {
            for (String nodeID : nodeIdSet) {
                nodeInfoHashMap.get(nodeID).setActive(true);
            }
        }

    }

    public void activateNeighbors4(String nodeId) { // a1
        NodeInfo n = nodeInfoHashMap.get(nodeId);
        if (n != null) {
            n.setActive(true);
        }
    }

    public void scheduleDisplay() {
        Timer periodicTimer = new Timer();
        periodicTimer.schedule(displayNodeInfoPathInfo(), 0, 10000);
        periodicTimer.schedule(failureDetection(), 0, M *K);
    }

    public void initLogging() {
    }
}


