package com.node;

/*
 * created by divya at 1/27/2018
 * @author - divya, ashley
 */
import jdk.nashorn.internal.runtime.regexp.joni.NodeOptInfo;

import java.net.*;
import java.io.*;
import java.util.*;

public class SDNSwitch {

    //Define switches unique IP (needs to be adapted to take command line argument)
    private static InetAddress switchIp = null;
    private static int controllerPort;
    private static InetAddress controllerIp = null;
    private static DatagramSocket switchDatagramSocket = null;
    private static final String sendRegisterRequestMessage = "REGISTER_REQUEST";
    private static List<NodeInfo> NodeInfoList = new ArrayList<NodeInfo>();

    //Function to initialise the UDP socket for a switch
    private static DatagramSocket initSwitchSocket(int port, InetAddress address) {
        DatagramSocket switchSocket = null;
        try {
            //DatagramSocket is used for UDP (Socket for TCP)
            switchSocket = new DatagramSocket(port, address);
        } catch (SocketException e) {
            System.err.println("Error creating switchSocket");
            System.exit(1);
        }
        return (switchSocket);
    }

    //TODO The following two timer tasks do not currently work properly, needs finishing
    //Define the KEEP_ALIVE task
    static TimerTask sendKeepAlive(final DatagramSocket Socket, final int port, final List<NodeInfo> nodeInfoList) {
        TimerTask sendKeepAlivethread = new TimerTask() {
            @Override
            public void run() {
                //Need keep alive byte to be consistent across switches
                byte[] buf = new byte[1000];
                for (NodeInfo node : nodeInfoList) {
                    DatagramPacket KEEP_ALIVE = null;
                    try {
                        KEEP_ALIVE = new DatagramPacket(buf, buf.length, InetAddress.getByName(node.getHost()), port);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    try {
                        Socket.send(KEEP_ALIVE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        return sendKeepAlivethread;
    }

    //Define the TOPOLOGY UPDATE task
    static TimerTask topologyUpdate(final DatagramSocket Socket, final int port, final List<NodeInfo> nodeInfoList) {
        TimerTask topologyUpdatethread = new TimerTask() {
            @Override
            public void run() {
                byte[] buf = null; // actually need to send topology
                DatagramPacket TOPOLOGY_UPDATE = new DatagramPacket(buf, buf.length, controllerIp, controllerPort);
                try {
                    switchDatagramSocket.send(TOPOLOGY_UPDATE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        return topologyUpdatethread;
    }

    //Main function class - to be used only for testing.
    public static void main(String[] args) {
        SDNSwitch sdnSwitch = new SDNSwitch();
        sdnSwitch.startSwitch();
    }

    public void startSwitch() {
        try {
            initSwitch();

            //Send initial REGISTER_REQUEST message format must be consistent with controller
            sendRegisterReqMsg();
            sendPeriodicMessages();


            //Initialise a data buffer and packet to use for incoming data
            byte[] inBuffer = new byte[1000];
            //TODO buffer size may require changing
            DatagramPacket incomingData = new DatagramPacket(inBuffer, inBuffer.length);


            //Main program thread
            while (true) {
                switchDatagramSocket.receive(incomingData);
                InetAddress incomingAddress = incomingData.getAddress();
                byte[] incomingMessage = incomingData.getData();
                String receivedMessage = new String(incomingMessage, 0, incomingData.getLength());

                if (incomingAddress == controllerIp) {
                    if (receivedMessage.equals("REGISTER_RESPONSE")) {
                        //TODO update topology and hence switchIPs
                        sendKeepAlive(switchDatagramSocket, controllerPort, NodeInfoList);
                    } else if (receivedMessage.equals("ROUTE_UPDATE")) {
                        //TODO update routing table
                    }
                    //TODO check for ROUTE_UPDATE and update routing table
                    //Also check for data to be sent on, check where it should go using routing table
                    //For current network only one switch and controller so simple topology
                } else if (incomingAddress == switchIp) {
                    //TODO essentially ignore packets from self (Shouldn't happen)
                } else {
                    //packets from other switches
                    if (receivedMessage.equals("KEEP_ALIVE")) {
                        //switchIPs.add(incomingAddress);
                    }
                    //TODO deal with packets to be sent onwards, Also need to implement expiry on IP addresses
                }
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //Need to close switchDatagramSocket when done
            switchDatagramSocket.close();
        }
    }
    private static void sendPeriodicMessages() {
        //Setup timer (runs on its own thread)
        Timer periodicTimer = new Timer();

        byte[] currentTopology = new byte[0];

        //Send keepAlive and TopologyUpdate messages every k milliseconds
        final int k = 1000;
        //k currently arbitrarily set at 1000 --TODO Needs reviewing
        periodicTimer.schedule(sendKeepAlive(switchDatagramSocket, controllerPort, NodeInfoList), 0, k);
        periodicTimer.scheduleAtFixedRate(topologyUpdate(switchDatagramSocket,controllerPort, NodeInfoList), new Date(), k);
    }

    private static void sendRegisterReqMsg() throws IOException {
        byte[] dataByte = sendRegisterRequestMessage.getBytes("UTF-8");
        DatagramPacket registerRequest = new DatagramPacket(dataByte, 16, controllerIp, controllerPort);
        switchDatagramSocket.send(registerRequest);
    }

    private static void initSwitch() throws UnknownHostException {
        switchIp = InetAddress.getByName("127.0.0.2");
        controllerPort = 1025;
        controllerIp = InetAddress.getByName("127.0.0.1");
        switchDatagramSocket = initSwitchSocket(controllerPort, controllerIp);
    }

    /*
    1. List of InetAddress replace with List of NodeInfo
     */
}
