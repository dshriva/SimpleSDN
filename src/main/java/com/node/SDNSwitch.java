package com.node;

/*
 * created by divya at 1/27/2018
 * @author - divya, ashley
 */

import javax.xml.soap.Node;
import java.net.*;
import java.io.*;
import java.util.*;

public class SDNSwitch {

    //Define switches unique IP (needs to be adapted to take command line argument)
    private static InetAddress switchInetAddress = null;
    private static String Switchid;
    private static int SwitchPort;
    private static int controllerPort;
    private static String controllerIp;
    private static InetAddress controllerInetAddress = null;
    private static DatagramSocket switchDatagramSocket = null;
    private static HashMap<String, NodeInfo> NodeInfoHashMap = new HashMap<String, NodeInfo>();

    //creating parameterized constructor
    public SDNSwitch(int port, String switchID, String controllerIP, int controllerPort) {
     this.SwitchPort = port;
     this.Switchid = switchID;
     this.controllerIp = controllerIP;
     this.controllerPort = controllerPort;
    }

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
    static TimerTask sendKeepAlive(final DatagramSocket Socket, final int port, final HashMap<String, NodeInfo> NodeInfoHashMap) {
        TimerTask sendKeepAlivethread = new TimerTask() {
            @Override
            public void run() {
                //Need keep alive byte to be consistent across switches
                String keepalive = "KEEPALIVE";
                byte[] buf = keepalive.getBytes();
                for(Map.Entry<String, NodeInfo> entrySet : NodeInfoHashMap.entrySet()){
                    NodeInfo node = entrySet.getValue();
                    String neighborSwitchId = entrySet.getKey();
                    DatagramPacket keepAliveDataPacket = null;
                    try {
                        keepAliveDataPacket = new DatagramPacket(buf, buf.length, InetAddress.getByName(String.valueOf(node.getHost())), node.getPort());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    try {
                        Socket.send(keepAliveDataPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        return sendKeepAlivethread;
    }

    //Define the TOPOLOGY UPDATE task
    static TimerTask topologyUpdate(final DatagramSocket Socket, final int port, final HashMap<String, NodeInfo> nodeInfoHashMap) {
        TimerTask topologyUpdatethread = new TimerTask() {
            @Override
            public void run() {
                byte[] buf = null; // actually need to send topology
                DatagramPacket TOPOLOGY_UPDATE = new DatagramPacket(buf, buf.length, controllerInetAddress, controllerPort);
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
    public static void main(String[] args) throws ClassNotFoundException {
        SDNSwitch sdnSwitch = new SDNSwitch(SwitchPort,Switchid,"127.0.0.1", 3000);
        try {
            sdnSwitch.startSwitch();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startSwitch() throws IOException {
        try {
            initSwitch();

            //Send initial REGISTER_REQUEST message format must be consistent with controller
            sendRegisterReqMsg();
            //sendPeriodicMessages();


            //Initialise a data buffer and packet to use for incoming data
            byte[] inBuffer = new byte[1000];
            //TODO buffer size may require changing
            DatagramPacket incomingData = new DatagramPacket(inBuffer, inBuffer.length);


            //Main program thread
            while (true) {
                switchDatagramSocket.receive(incomingData);
                InetAddress incomingAddress = incomingData.getAddress();
                byte[] incomingMessage = incomingData.getData();
               // String receivedMessage = new String(incomingMessage, 0, incomingData.getLength());
                //System.out.println(receivedMessage);
                ByteArrayInputStream byteIn = new ByteArrayInputStream(incomingMessage);
                ObjectInputStream in = new ObjectInputStream(byteIn);
                HashMap<String,NodeInfo> updatedNodeInfoHashMap = (HashMap<String,NodeInfo>) in.readObject();
                System.out.println(updatedNodeInfoHashMap.toString());
                if (incomingAddress == controllerInetAddress) {
                  //  if (receivedMessage.equals("REGISTER_RESPONSE")) {
                        //TODO update topology and hence switchIPs
                        //
                 //   } else if (receivedMessage.equals("ROUTE_UPDATE")) {
                        //TODO update routing table
                    }
                    //TODO check for ROUTE_UPDATE and update routing table
                    //Also check for data to be sent on, check where it should go using routing table
                    //For current network only one switch and controller so simple topology
               // } else if (incomingAddress == switchInetAddress) {
                    //TODO essentially ignore packets from self (Shouldn't happen)
                //} else {
                    //packets from other switches
               //     if (receivedMessage.equals("KEEP_ALIVE")) {
                        //switchIPs.add(incomingAddress);
                    }
                    //TODO deal with packets to be sent onwards, Also need to implement expiry on IP addresses
                }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
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
        periodicTimer.schedule(sendKeepAlive(switchDatagramSocket, controllerPort, NodeInfoHashMap), 0, k);
        periodicTimer.scheduleAtFixedRate(topologyUpdate(switchDatagramSocket,controllerPort, NodeInfoHashMap), new Date(), k);
    }


    private static void sendRegisterReqMsg() throws IOException {
        String sendRegisterRequestMessage = "REGISTER_REQUEST" + ":" + Switchid;
        System.out.println(sendRegisterRequestMessage);
        byte[] dataByte = sendRegisterRequestMessage.getBytes();
        DatagramPacket registerRequest = new DatagramPacket(dataByte, dataByte.length, controllerInetAddress, controllerPort);
        switchDatagramSocket.send(registerRequest);
    }

    private static void initSwitch() throws UnknownHostException {
        switchInetAddress = InetAddress.getByName("127.0.0.1");
        SDNSwitch.controllerInetAddress = InetAddress.getByName("127.0.0.1");
        switchDatagramSocket = initSwitchSocket(SwitchPort, SDNSwitch.controllerInetAddress);
    }

    public void messageExchangeinSwitch(int port, String id, String host) {

    }

    /*
    1. List of InetAddress replace with List of NodeInfo
     */
}
