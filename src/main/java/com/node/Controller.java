package com.node;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

/*
 * created by divya at 1/17/2018
 */
public class Controller {
    private static final int controllerPort = 2999;
    private static String machinePort = "";
    private static String controllerIP = "";
    private HashMap<String, NodeInfo> nodeInfoHashMap = new HashMap<String, NodeInfo>();
    private HashMap<String, Path> pathHashMap = new HashMap<String, Path>();

    public Controller(int port) {

    }

    public void readConfigFile() {
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

                    Path path = new Path(Integer.parseInt(keywords[2]), Integer.parseInt(keywords[3]), switchId1, switchId2);
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
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void messageExchange(int port) throws IOException {

        DatagramSocket ds = null;
        try {
            // creating a socket header
            ds = new DatagramSocket(2999);

            //receiving register request message from switch
            byte[] b = new byte[1024];
            DatagramPacket regRequest = new DatagramPacket(b, b.length);
            ds.receive(regRequest);
            System.out.println("register request from switch received");

            //converting data in bytes to string and splitting the string
            String str = new String(regRequest.getData(), 0, regRequest.getLength()); //converting data in bytes to String
            String[] splittedString = parseString(str); //calling the function to split the string
            updateNodeInfoHashMap(splittedString[0], splittedString[1]); //function to construct node info list

            //Now sending the response to the switch
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);
            objOpStream.writeObject(nodeInfoHashMap);
            int length = 0;
            byte[] buf = null;
            buf = byteArrayOutputStream.toByteArray();
            length = buf.length;
            InetAddress ia = InetAddress.getLocalHost();
            DatagramPacket response = new DatagramPacket(buf, length, ia, (regRequest.getPort()));
            ds.send(response);
            System.out.println("register response to switch sent");

        } catch (Exception e) {

        } finally {
            ds.close();
        }
    }

    private void updateNodeInfoHashMap(String s, String s1) {
        if(s.equalsIgnoreCase("register request")){
            if(nodeInfoHashMap.containsKey(s1)){

            }
        }
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

}


