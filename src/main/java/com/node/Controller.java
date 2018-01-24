package com.node;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * created by divya at 1/17/2018
 */
public class Controller {
    private static final int controllerPort = 2999;
    private static String machinePort = "";

    public Controller(int port) {

    }


 /*  public static void main(String[] args) throws IOException {

        Controller controller = new Controller(2999);
        System.out.println("This is controller");

        messageExchange(controllerPort);






        List<NodeInfo> nodeInfoList = controller.constructNodeInfoList();



        controller.displayNodeInfo(nodeInfoList);


    }
    */

    public static void messageExchange(int port) throws IOException {
        //Now creating a socket header.
        DatagramSocket ds = new DatagramSocket(2999);
        System.out.println("This is controller socket");

        //receiving register request message from switch

        byte[] b = new byte[1024];
        DatagramPacket regRequest = new DatagramPacket(b, b.length);
        try {
            ds.receive(regRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("register request from switch received");
        String str = new String(regRequest.getData(), 0, regRequest.getLength());


        String splittedString = parseString(str);

        List<NodeInfo> nodeInfoList = constructNodeInfoList(splittedString);
        displayNodeInfo( nodeInfoList);

        //Now sending the register response to the switch
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);
        objOpStream.writeObject(nodeInfoList);
        int length = 0;
        byte[] buf = null;
        buf = byteArrayOutputStream.toByteArray();
        length = buf.length;
        InetAddress ia = InetAddress.getLocalHost();
        DatagramPacket response = new DatagramPacket(buf, length, ia, (regRequest.getPort()));
        //response.setAddress(InetAddress.getByName(String.valueOf(regRequest.getAddress())));
        // response.setPort((regRequest.getPort()));
        ds.send(response);
        System.out.println("register response to switch sent");

    }

    public static String parseString(String str) {

        String str1[] = str.split("$");
        return str1[0];

    }


   public static List<NodeInfo> constructNodeInfoList(String splittedString) {
        List<NodeInfo> nodeInfoList = new ArrayList<NodeInfo>();

        NodeInfo nodeInfo1 = new NodeInfo("S1", "dshriva@purdue", 2999);
       if(splittedString.equalsIgnoreCase("register me")) {
           nodeInfo1.setActive(true);
       }
        nodeInfoList.add(nodeInfo1);

       // NodeInfo nodeInfo2 = new NodeInfo("12dc13", "temp2.ecn.edu", 1100);
       // nodeInfoList.add(nodeInfo2);

       // NodeInfo nodeInfo3 = new NodeInfo("3458a", "divya@purdue", 2345);
       // nodeInfo3.setActive(true);
       // nodeInfoList.add(nodeInfo3);

       // nodeInfo1.getNeighbourSet().add(nodeInfo2);
       // nodeInfo1.getNeighbourSet().add(nodeInfo3);

       // nodeInfo2.getNeighbourSet().add(nodeInfo1);
       // nodeInfo3.getNeighbourSet().add(nodeInfo1);

        return nodeInfoList;
    }

    public static void displayNodeInfo(List<NodeInfo> nodeInfoList) {

        for (int i = 0; i < nodeInfoList.size(); i++) {
            System.out.println(nodeInfoList.get(i).toString());
        }
    }


}


