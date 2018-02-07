package com.domain;

/*
 * created by divya at 1/27/2018
 * @author - divya, ashley
 */

import org.apache.log4j.Logger;

import java.net.*;
import java.io.*;
import java.util.*;

import static com.util.NetworkConstants.*;

public class Switch {

    //Define switches unique IP (needs to be adapted to take command line argument)
    private static InetAddress switchInetAddress = null;
    private static String switchId;
    private static int SwitchPort;
    private static int controllerPort;
    private static String controllerIp;
    private static InetAddress controllerInetAddress = null;
    private static DatagramSocket switchDatagramSocket = null;
    private static HashMap<String, NodeInfo> neighborHashMap = new HashMap<String, NodeInfo>();
    public static Logger LOGGER = Logger.getLogger(String.valueOf(Switch.class));
    private static Set<String> unreachableSwitches = new HashSet<String>();


    //creating parameterized constructor
    public Switch(int port, String switchID, String controllerIP, int controllerPort) {
        this.SwitchPort = port;
        this.switchId = switchID;
        this.controllerIp = controllerIP;
        this.controllerPort = controllerPort;
    }

    public Set<String> getUnreachableSwitches() {
        return unreachableSwitches;
    }

    //Function to initialise the UDP socket for a switch
    private static DatagramSocket initSwitchSocket(int port, InetAddress address) {
        DatagramSocket switchSocket = null;
        try {
            //DatagramSocket is used for UDP (Socket for TCP)
            switchSocket = new DatagramSocket(port, address);
        } catch (SocketException e) {
            LOGGER.error("Error creating switchSocket");
            System.out.println("Error creating switchSocket");
            System.exit(1);
        }
        return (switchSocket);
    }

    //Define the KEEP_ALIVE task
    static TimerTask sendKeepAlive(final DatagramSocket Socket, final int port) {
        TimerTask sendKeepAlivethread = new TimerTask() {
            @Override
            public void run() {
                //System.out.println("Start sending message " + KEEP_ALIVE_MESSAGE);
                LOGGER.debug("Start sending message " + KEEP_ALIVE_MESSAGE);
                //Need keep alive byte to be consistent across switches
                for (Map.Entry<String, NodeInfo> entrySet : neighborHashMap.entrySet()) {
                    NodeInfo neighbor = entrySet.getValue();
                    // simulating link failures
                    if(unreachableSwitches.contains(neighbor.getId())) {
                        //System.out.println("Skipping sending message to "+neighbor.getId()+ " as its unreachable.");
                        LOGGER.error("Skipping sending message to "+neighbor.getId()+ " as its unreachable.");
                        continue;
                    }
                    if (neighbor.isActive()) {
                        DatagramPacket keepAliveDataPacket = null;
                        try {
                            HashMap<String, String> sendMap = new HashMap<String, String>();
                            sendMap.put(KEEP_ALIVE_MESSAGE, switchId);
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);
                            objOpStream.writeObject(sendMap);
                            int length = 0;
                            byte[] buf = null;
                            buf = byteArrayOutputStream.toByteArray();
                            length = buf.length;
                            DatagramPacket response = new DatagramPacket(buf, length, InetAddress.getByName(neighbor.getHost()), neighbor.getPort());
                            switchDatagramSocket.send(response);
                            //System.out.println("Sent Keep Alive to switch " + neighbor.getId());
                            LOGGER.debug("Sent Keep Alive to switch " + neighbor.getId());
                        } catch (UnknownHostException e) {
                            LOGGER.error( e.getStackTrace());
                        } catch (IOException e) {
                            LOGGER.error( e.getStackTrace());
                        }
                    }
                }
                LOGGER.debug("Done sending message " + KEEP_ALIVE_MESSAGE);
                //System.out.println("Done sending message " + KEEP_ALIVE_MESSAGE);
            }
        };
        return sendKeepAlivethread;
    }

    //Define the TOPOLOGY UPDATE task
    static TimerTask topologyUpdate(final DatagramSocket Socket, final int port) {
        TimerTask topologyUpdatethread = new TimerTask() {
            @Override
            public void run() {
                LOGGER.debug("Sending message " + TOPOLOGY_UPDATE_MESSAGE);
                //System.out.println("\n\tSending message " + TOPOLOGY_UPDATE_MESSAGE);
                HashMap<String, NodeInfo> sendMap = new HashMap<String, NodeInfo>();
                NodeInfo dummyNode = new NodeInfo();
                dummyNode.setId(switchId);
                sendMap.put(TOPOLOGY_UPDATE_MESSAGE, dummyNode);
                for (Map.Entry<String, NodeInfo> entrySet : Switch.neighborHashMap.entrySet()) {
                    NodeInfo neighbor = entrySet.getValue();
                    sendMap.put(neighbor.getId(), neighbor);
                }
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                ObjectOutputStream out = null;
                try {
                    out = new ObjectOutputStream(byteOut);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                    e.printStackTrace();
                }
                try {
                    out.writeObject(sendMap);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                    //e.printStackTrace();
                }
                int length = 0;
                byte[] buf = null;
                buf = byteOut.toByteArray();
                length = buf.length;
                DatagramPacket TOPOLOGY_UPDATE = new DatagramPacket(buf, buf.length, controllerInetAddress, controllerPort);
                try {
                    switchDatagramSocket.send(TOPOLOGY_UPDATE);
                } catch (IOException e) {
                    LOGGER.error(e.getStackTrace());
                    //e.printStackTrace();
                }
                LOGGER.debug("Done sending message "+ TOPOLOGY_UPDATE_MESSAGE);
                //System.out.println("Done sending message "+ TOPOLOGY_UPDATE_MESSAGE);
            }
        };
        return topologyUpdatethread;
    }

    static TimerTask displayCurrentNeighbor(final DatagramSocket Socket, final int port) {
        TimerTask displayNeighborThread = new TimerTask() {
            @Override
            public void run() {
                //Need keep alive byte to be consistent across switches
                LOGGER.debug(" Displaying neighbor status");
                //System.out.println(" Displaying neighbor status");
                for (Map.Entry<String, NodeInfo> entrySet : neighborHashMap.entrySet()) {
                    //System.out.println(entrySet.getValue());
                    LOGGER.debug(entrySet.getValue());
                }
            }
        };
        return displayNeighborThread;
    }

    static TimerTask failureDetection() {
        TimerTask failureDetectionThread = new TimerTask() {
            @Override
            public void run() {
                try {
                    //Need keep alive byte to be consistent across switches
                    LOGGER.debug(" Detecting unreachable neighbors");
                    //System.out.println(" Detecting unreachable neighbors");
                    boolean sendTopologyUpdate = false;
                    for (Map.Entry<String, NodeInfo> entrySet : neighborHashMap.entrySet()) {
                        if (entrySet.getValue().isActive()) {
                            long currentTime = System.currentTimeMillis();
                            if ((currentTime - (M * K)) > entrySet.getValue().getLastSeenAt()) {
                                System.out.println("Marking Switch " + entrySet.getKey() + " as unreachable");
                                LOGGER.error("Marking Switch " + entrySet.getKey() + " as unreachable");
                                entrySet.getValue().setActive(false);
                                sendTopologyUpdate = true;
                            }
                        }
                    }
                    if (sendTopologyUpdate) {
                        //System.out.println("Sending message " + TOPOLOGY_UPDATE_MESSAGE + " to controller");
                        LOGGER.info("Sending message " + TOPOLOGY_UPDATE_MESSAGE + " to controller");
                        sendTopologyUpdateImmediately();
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex.getStackTrace());
                }
            }

            private void sendTopologyUpdateImmediately() {
                LOGGER.info("Failure detected - sending message "+TOPOLOGY_UPDATE_MESSAGE+" to controller");
                HashMap<String, NodeInfo> sendMap = new HashMap<String, NodeInfo>();
                NodeInfo dummyNode = new NodeInfo();
                dummyNode.setId(switchId);
                sendMap.put(TOPOLOGY_UPDATE_MESSAGE, dummyNode);
                for (Map.Entry<String, NodeInfo> entrySet : Switch.neighborHashMap.entrySet()) {
                    NodeInfo neighbor = entrySet.getValue();
                    sendMap.put(neighbor.getId(), neighbor);
                }
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                ObjectOutputStream out = null;
                try {
                    out = new ObjectOutputStream(byteOut);
                } catch (IOException e) {
                    LOGGER.error(e.getStackTrace());
                    e.printStackTrace();
                }
                try {
                    out.writeObject(sendMap);
                } catch (IOException e) {
                    LOGGER.error(e.getStackTrace());
                    e.printStackTrace();
                }
                int length = 0;
                byte[] buf = null;
                buf = byteOut.toByteArray();
                length = buf.length;
                DatagramPacket TOPOLOGY_UPDATE = new DatagramPacket(buf, buf.length, controllerInetAddress, controllerPort);
                try {
                    switchDatagramSocket.send(TOPOLOGY_UPDATE);
                } catch (IOException e) {
                    LOGGER.error(e.getStackTrace());
                    e.printStackTrace();
                }
                //System.out.println("\tDone sending message " + TOPOLOGY_UPDATE_MESSAGE);
                LOGGER.info("Done sending message " + TOPOLOGY_UPDATE_MESSAGE);
            }
        };
        return failureDetectionThread;
    }


    public void startSwitch() throws IOException {
        try {
            initSwitch();
            LOGGER.info("Switch Socket created");
            //Send initial REGISTER_REQUEST message format must be consistent with controller
            sendRegisterReqMsg();

            //Initialise a data buffer and packet to use for incoming data
            byte[] inBuffer = new byte[1000];
            DatagramPacket incomingData = new DatagramPacket(inBuffer, inBuffer.length);
            sendPeriodicMessages();

            //Main program thread to receive message
            while (true) {
                switchDatagramSocket.receive(incomingData);
                byte[] incomingMessage = incomingData.getData();

                ByteArrayInputStream byteIn = new ByteArrayInputStream(incomingMessage);
                ObjectInputStream in = new ObjectInputStream(byteIn);
                HashMap responseHashMap = (HashMap) in.readObject();
                processResponse(responseHashMap, incomingData);
            }
        } catch (UnknownHostException e) {
            LOGGER.error(e.getStackTrace());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getStackTrace());
            e.printStackTrace();
        } finally {
            //Need to close switchDatagramSocket when done
            LOGGER.debug("Exiting..");
            System.out.println("Exiting..");
            switchDatagramSocket.close();
        }
    }

    private void processResponse(HashMap responseHashMap, DatagramPacket incomingData) {
        if (responseHashMap.containsKey(REGISTER_RESPONSE_MESSAGE)) {
            LOGGER.info("Message received "+REGISTER_RESPONSE_MESSAGE);
            for (Object set : responseHashMap.entrySet()) {
                Map.Entry<String, NodeInfo> entrySet = (Map.Entry<String, NodeInfo>) set;
                String switchId = entrySet.getKey();
                if (switchId.equalsIgnoreCase(REGISTER_RESPONSE_MESSAGE))
                    continue;
                NodeInfo neighbor = entrySet.getValue();
                neighborHashMap.put(switchId, neighbor);
                //System.out.println("Neighbor = " + neighbor);
                LOGGER.info("Neighbor = " + neighbor);
            }
        } else if (responseHashMap.containsKey(KEEP_ALIVE_MESSAGE)) {
            String neighborSwitchId = (String) responseHashMap.get(KEEP_ALIVE_MESSAGE); // 1
            LOGGER.debug("==========================================");
            LOGGER.debug("Message received " + KEEP_ALIVE_MESSAGE + " from " + neighborSwitchId);
            LOGGER.debug("==========================================");
            if(unreachableSwitches.contains(neighborSwitchId)) {
                //System.out.println("Skipping receiving message from "+neighborSwitchId +" as its unreachable.");
                LOGGER.error("Skipping receiving message from "+neighborSwitchId +" as its unreachable.");
            } else {
                NodeInfo neighborNode = neighborHashMap.get(neighborSwitchId);
                neighborNode.setActive(true);
                neighborNode.setHost(incomingData.getAddress().getHostAddress());
                neighborNode.setPort(incomingData.getPort());
                neighborNode.setLastSeenAt(System.currentTimeMillis());
            }
        } else if (responseHashMap.containsKey(ROUTE_UPDATE_MESSAGE)) {
            LOGGER.info("Message received "+ROUTE_UPDATE_MESSAGE);
            for (Object set : responseHashMap.entrySet()) {
                Map.Entry<String, Path> entrySet = (Map.Entry<String, Path>) set;
                String key = entrySet.getKey();
                if (key.equalsIgnoreCase(ROUTE_UPDATE_MESSAGE))
                    continue;

                LOGGER.debug("Active Path: " + entrySet.getValue());
            }
        }
    }

    private static void sendPeriodicMessages() {
        //Setup timer (runs on its own thread)
        Timer periodicTimer = new Timer();
        //Send keepAlive and TopologyUpdate messages every k milliseconds
        periodicTimer.schedule(sendKeepAlive(switchDatagramSocket, controllerPort), 0, K);
        periodicTimer.scheduleAtFixedRate(topologyUpdate(switchDatagramSocket, controllerPort), new Date(), K);
        periodicTimer.schedule(displayCurrentNeighbor(switchDatagramSocket, controllerPort), 0, 10000);
        periodicTimer.scheduleAtFixedRate(failureDetection(), new Date(), M * K);
    }

    private static void sendRegisterReqMsg() throws IOException {
        HashMap<String, String> sendMap = new HashMap<String, String>();
        sendMap.put(REGISTER_REQUEST_MESSAGE, switchId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);
        objOpStream.writeObject(sendMap);
        int length = 0;
        byte[] buf = null;
        buf = byteArrayOutputStream.toByteArray();
        length = buf.length;
        DatagramPacket response = new DatagramPacket(buf, length, InetAddress.getByName(controllerIp), controllerPort);
        switchDatagramSocket.send(response);
        LOGGER.debug("register request message sent to the controller");
    }

    private static void initSwitch() throws UnknownHostException {
        switchInetAddress = InetAddress.getByName("127.0.0.1");
        Switch.controllerInetAddress = InetAddress.getByName("127.0.0.1");
        switchDatagramSocket = initSwitchSocket(SwitchPort, Switch.controllerInetAddress);
    }
}
