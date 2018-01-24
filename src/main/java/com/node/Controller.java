package com.node;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

    public Controller(int port) {

    }

    public void messageExchange(int port) throws IOException {

        // creating a socket header
        DatagramSocket ds = new DatagramSocket(2999);

        //receiving register request message from switch
        byte[] b = new byte[1024];
        DatagramPacket regRequest = new DatagramPacket(b, b.length);
        ds.receive(regRequest);
        System.out.println("register request from switch received");

        //converting data in bytes to string and splitting the string
        String str = new String(regRequest.getData(), 0, regRequest.getLength()); //converting data in bytes to String
        String[] splittedString = parseString(str); //calling the function to split the string
        List<NodeInfo> nodeInfoList = constructNodeInfoList(splittedString[0], splittedString[1]); //function to construct node info list

        //Now sending the response to the switch
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);
        objOpStream.writeObject(nodeInfoList);
        int length = 0;
        byte[] buf = null;
        buf = byteArrayOutputStream.toByteArray();
        length = buf.length;
        InetAddress ia = InetAddress.getLocalHost();
        DatagramPacket response = new DatagramPacket(buf, length, ia, (regRequest.getPort()));
        ds.send(response);
        System.out.println("register response to switch sent");
    }

    public static String[] parseString(String str) { //function to split the string in order to serve the purpose of activating the switch

        String split[] = str.split("\\$");
        return split;

    }

    public static List<NodeInfo> constructNodeInfoList(String splittedString, String switchId) {

        //creating node (Switch) information list
        List<NodeInfo> nodeInfoList = new ArrayList<NodeInfo>();
        NodeInfo nodeInfo1 = new NodeInfo(switchId, "dshriva@purdue", 3000);
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


}


