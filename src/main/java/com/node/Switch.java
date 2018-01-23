package com.node;

import java.io.IOException;
import java.net.*;

/*
 * created by divya at 1/17/2018
 */
public class Switch {
    private static String host;
    private static int port;
    private static String id;


    public Switch(int port,String id,String host){
        this.port = port;
        this.id =id;
        this.host =host;
    }

    /* public static void main(String[] args) throws IOException {
        Switch newSwitch = new Switch(9999,"1","dshriva@purdue");
        System.out.println("This is switch");
        messageExchangeinSwitch(port,id,host);

    }
    */

    public static void messageExchangeinSwitch(int port, String id, String host) throws IOException {
        //creating socket descriptor for switch
        DatagramSocket ds  = new DatagramSocket();
        String str1 = "hi controller!";
        //sending register request to controller with ID information
        InetAddress ia = InetAddress.getLocalHost();
        byte[] b = str1.getBytes();
        DatagramPacket dp = new DatagramPacket(b,b.length,ia,2999);
        ds.send(dp);
        System.out.println("register request to controller sent");


        //receiving register response from controller
        byte[] b1 =new byte[1024];
        DatagramPacket dp1 = new DatagramPacket(b1,b1.length);
        ds.receive(dp1);
        System.out.println("register response received from controller to the switch");
        String str3 = new String(dp1.getData(), 0, dp1.getLength());
        System.out.println("result is " + str3);



    }




}
