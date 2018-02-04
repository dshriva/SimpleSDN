package com.node;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

import static com.util.NetworkConstants.REGISTER_REQUEST_MESSAGE;
import static com.util.NetworkConstants.REGISTER_RESPONSE_MESSAGE;

/*
 * created by divya at 1/17/2018
 */
public class Controller {
    private static String machinePort = "";
    private static String controllerIP = "";
    private static HashMap<String, NodeInfo> nodeInfoHashMap = new HashMap<String, NodeInfo>();
    private static HashMap<String, Path> pathHashMap = new HashMap<String, Path>();
    private static DatagramSocket controllerSocket = null;

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

                    Path path = new Path(Integer.parseInt(keywords[2]), Integer.parseInt(keywords[3]), switchId1, switchId2,false);
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
                if(bufferedReader != null)
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
                    DatagramPacket regRequest = new DatagramPacket(b, b.length);
                    controllerSocket.receive(regRequest);
                    handleRegisterRequestMessage(regRequest);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }  finally {
            System.out.println("Exiting...");
            controllerSocket.close();
        }

    }

    private void handleRegisterRequestMessage(DatagramPacket regRequest) throws IOException {
        System.out.println("register request from switch received");
        //converting data in bytes to string and splitting the string
        String str = new String(regRequest.getData(), 0, regRequest.getLength());
        System.out.println(str);
        //converting data in bytes to String
        String[] splittedString = parseString(str); //calling the function to split the string
        //System.out.println(Arrays.toString(splittedString));

        String switchIpAddress = String.valueOf(regRequest.getAddress());
        if (switchIpAddress.startsWith("/")) {
            switchIpAddress = switchIpAddress.substring(1);
        }
        int switchPort = regRequest.getPort();
        HashMap<String, NodeInfo> retMap = updateNodeInfoHashMap(splittedString[0], splittedString[1], switchIpAddress, switchPort);

        //function to construct node info list
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

    private HashMap<String, NodeInfo> updateNodeInfoHashMap(String message, String switchId, String switchHost, int switchPort) {
        if(message.equalsIgnoreCase(REGISTER_REQUEST_MESSAGE)){
            if(nodeInfoHashMap.containsKey(switchId)){
                NodeInfo currNode = nodeInfoHashMap.get(switchId);
                currNode.setActive(true);
                currNode.setHost(switchHost);
                currNode.setPort(switchPort);
                currNode.setLastSeenAt(System.currentTimeMillis());
               //return the neighbor set
                HashMap<String, NodeInfo> retHashMap = new HashMap<String, NodeInfo>();
                retHashMap.put(REGISTER_RESPONSE_MESSAGE, null);
                for(NodeInfo neighborNodeInfo : currNode.getNeighbourSet()) {
                    retHashMap.put(neighborNodeInfo.getId(), neighborNodeInfo);
                    if(neighborNodeInfo.isActive()) {
                        // set the path as usable
                        if(pathHashMap.containsKey(neighborNodeInfo.getId() + "<->" + currNode.getId())){
                           pathHashMap.get(neighborNodeInfo.getId() + "<->" + currNode.getId()).setUsable(true);
                        } else if(pathHashMap.containsKey(currNode.getId() + "<->" + neighborNodeInfo.getId())) {
                            pathHashMap.get(currNode.getId() + "<->" + neighborNodeInfo.getId()).setUsable(true);
                        }
                    }
                }
                return retHashMap;
            }
        }
        return null;
    }

    static TimerTask displayNodeInfoPathInfo() {
        TimerTask displayNodePathInfoThread = new TimerTask() {
            @Override
            public void run() {
                try {
                    //Need keep alive byte to be consistent across switches
                    System.out.println("\n\n ------------------------------- ");
                    System.out.println(" Displaying NodeInfo");
                    for (Map.Entry<String, NodeInfo> entrySet : nodeInfoHashMap.entrySet()) {
                        System.out.println(entrySet.getValue());
                    }
                    System.out.println(" ------------------------------- \n");

                    System.out.println("\n\n ------------------------------- ");
                    System.out.println(" Displaying PathInfo");
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

        //creating node (Switch) information list
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
       for(NodeInfo node1 : node.getNeighbourSet()) {
           node1.setActive(true);
       }
    }

    public void activateNeighbors2(Set<NodeInfo> set) {
        for(NodeInfo node : set){
            node.setActive(true);
        }
    }

    public void activateNeighbors3(Set<String> nodeIdSet) { // 2,4,6
        if(nodeIdSet !=null){
            for(String nodeID : nodeIdSet){
                nodeInfoHashMap.get(nodeID).setActive(true);
            }
        }

    }

    public void activateNeighbors4(String nodeId) { // a1
        NodeInfo n = nodeInfoHashMap.get(nodeId);
        if(n != null) {
            n.setActive(true);
        }
    }

    public void scheduleDisplay() {
        Timer periodicTimer = new Timer();
        periodicTimer.schedule(displayNodeInfoPathInfo(), 0, 10000);
    }
}


