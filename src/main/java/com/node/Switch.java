package com.node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.List;

/*
 * created by divya at 1/17/2018
 */
public class Switch {
    private static String ipAddress = "127.0.0.1";
    private static int switchport = 0;

    public Switch(String port, String ipAddress) {
        this.switchport = Integer.parseInt(port);
        this.ipAddress = ipAddress;
    }

    public void messageExchangeinSwitch(int port, String id, String host) throws IOException, ClassNotFoundException {

        //creating socket descriptor for switch
        DatagramSocket datagramSocket = new DatagramSocket();
        String str1 = "register me" + "$" + "SwitchId1"; // String switch id which will be sent to the controller

        //sending register request to controller with ID information
        InetAddress ia = InetAddress.getLocalHost();
        byte[] b = str1.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(b, b.length, ia, 2999);
        datagramPacket.setAddress(InetAddress.getByName(ipAddress));
        datagramPacket.setPort(2999);
        datagramSocket.send(datagramPacket);
        System.out.println("register request from switch 1 to controller sent");

        //receiving register response from controller
        byte[] b1 = new byte[1024];
        DatagramPacket dp1 = new DatagramPacket(b1, b1.length);
        datagramSocket.receive(dp1);
        byte[] receivedBytes = dp1.getData();
        ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
        ObjectInputStream objInpStream = new ObjectInputStream(bais);
        List<NodeInfo> nodeInfoList = (List<NodeInfo>) objInpStream.readObject();
        System.out.println("Register response received from controller to the switch");
        displayNodeInfo(nodeInfoList);
    }

    public static void displayNodeInfo(List<NodeInfo> nodeInfoList) {
        for (int i = 0; i < nodeInfoList.size(); i++) {
            System.out.println(nodeInfoList.get(i).toString());
        }
    }
}
