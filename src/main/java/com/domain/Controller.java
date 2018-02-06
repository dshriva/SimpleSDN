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
    public static HashMap<String, NodeInfo> nodeInfoHashMap = new HashMap<String, NodeInfo>();
    public static HashMap<String, Path> pathHashMap = new HashMap<String, Path>();
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
            System.out.println("Reading Configuration File");
            LOGGER.trace("Reading Configuration File");
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
            LOGGER.error("Unable to open file '" + fileName + "'");
        } catch (IOException e) {
            LOGGER.error(e.getStackTrace());
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (IOException e) {
                LOGGER.error(e.getStackTrace());
                e.printStackTrace();
            }
        }
    }

    public void createSocket(int port) {
        // creating a socket header
        try {
            controllerSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            LOGGER.error(e.getStackTrace());
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
                    LOGGER.error(e.getStackTrace());
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getStackTrace());
            ex.printStackTrace();
        } finally {
            LOGGER.debug("Exiting...");
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
            LOGGER.error(e.getStackTrace());
            e.printStackTrace();
        }
    }

    private void handleTopologyUpdateMessage(HashMap responseHashMap) {
        LOGGER.debug("Entering the method: Controller.handleTopologyUpdateMessage");
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
        LOGGER.debug("Exiting the method: Controller.handleTopologyUpdateMessage");
    }

    private void computeWidestPath() {
        Graph graph = new Graph(nodeInfoHashMap, pathHashMap);
        LOGGER.debug("Entering the method: Controller.computeWidestPath");
        graph.computeWidestPath();
        LOGGER.debug("Exiting the method: Controller.computeWidestPath");
    }

    private void handleRegisterRequestMessage(HashMap responseHashMap, DatagramPacket regRequest) throws IOException {
        LOGGER.debug("Entering the method: Controller.handleRegisterRequestMessage");
        String switchId = (String) responseHashMap.get(REGISTER_REQUEST_MESSAGE);
        String switchIpAddress = String.valueOf(regRequest.getAddress());
        LOGGER.info("Controller.handleRegisterRequestMessage: switchId = "+switchId+", switch ip = "+switchIpAddress);
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
            LOGGER.info("Register response to switch sent");
            //System.out.println("register response to switch sent");
        }
        LOGGER.debug("Entering the method: Controller.handleRegisterRequestMessage");
    }

    private HashMap<String, NodeInfo> updateNodeInfoHashMap(String switchId, String switchHost, int switchPort) {
        LOGGER.debug("Entering the method Controller.updateNodeInfoHashMap");
        if (nodeInfoHashMap.containsKey(switchId)) {
            LOGGER.info("Updating membership list");
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
        LOGGER.debug("Exiting the method Controller.updateNodeInfoHashMap");
        return null;
    }

    static TimerTask failureDetection() {
        TimerTask failureDetectionThread = new TimerTask() {
            @Override
            public void run() {
                try {
                    //Need keep alive byte to be consistent across switches
                    System.out.println("Detecting Failures");
                    LOGGER.debug("Detecting Failures");
                    for (Map.Entry<String, NodeInfo> entrySet : nodeInfoHashMap.entrySet()) {
                        long currentTime = System.currentTimeMillis();
                        String currNodeId = entrySet.getKey();
                        if ((currentTime - (M * K)) > entrySet.getValue().getLastSeenAt()) {
                            if(entrySet.getValue().isActive()) {
                                System.out.println("Marking Switch "+entrySet.getKey()+" as dead");
                                LOGGER.info("Marking Switch "+entrySet.getKey()+" as dead");
                                entrySet.getValue().setActive(false);
                            }
                            //System.out.println("Marking the following paths as unreachable");
                            for(Map.Entry<String, Path> eSet : pathHashMap.entrySet()) {
                                Path path = eSet.getValue();
                                if(path.getVertexSet().contains(currNodeId)){
                                    //System.out.println("Path Id: "+path.getPathId());
                                    if(path.isUsable()) {
                                        System.out.println("Marking the link"+path.getPathId()+" as unreachable");
                                        LOGGER.info("Marking the link"+path.getPathId()+" as unreachable");
                                        path.setUsable(false);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex.getStackTrace());
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
                    LOGGER.trace("Displaying Switches");
                    System.out.println("Displaying Switches");
                    for (Map.Entry<String, NodeInfo> entrySet : nodeInfoHashMap.entrySet()) {
                        System.out.println(entrySet.getValue());
                        LOGGER.trace(entrySet.getValue());
                       // System.out.println(entrySet.getValue());
                    }
                    LOGGER.trace(" Displaying Links");
                    System.out.println(" Displaying Links");
                    for (Map.Entry<String, Path> entrySet : pathHashMap.entrySet()) {
                        System.out.println(entrySet.getValue());
                        LOGGER.trace(entrySet.getValue());
                        //System.out.println(entrySet.getValue());
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex.getStackTrace());
                    ex.printStackTrace();
                }
            }
        };
        return displayNodePathInfoThread;
    }

    public void scheduleDisplay() {
        Timer periodicTimer = new Timer();
        periodicTimer.schedule(displayNodeInfoPathInfo(), 0, 10000);
        periodicTimer.schedule(failureDetection(), 0, M *K);
    }

}


