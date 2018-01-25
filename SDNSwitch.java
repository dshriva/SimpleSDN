//ECE50862 Project 1
//Java code for the switch ver1.
//Ashley Gregg
//Useful database on sockets in java:
//https://docs.oracle.com/javase/8/docs/api/java/net/Socket.html
	
//import classes required for sockets
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SDNSwitch {

	//Function to initialise the UDP socket for a switch
	private static DatagramSocket initSwitchSocket(int port,  InetAddress address) {
		DatagramSocket switchSocket = null;
		try {
			//DatagramSocket is used for UDP (Socket for TCP)
			switchSocket = new DatagramSocket(port, address);
		} 
		catch (SocketException e) {
			System.err.println("Error creating switchSocket");
            System.exit(1);
		}
		return (switchSocket);
	}
	
	//TODO The following two timer tasks do not currently work properly, needs finishing
	//Define the KEEP_ALIVE task
	static TimerTask sendKeepAlive(DatagramSocket Socket,  int port, List<InetAddress> addresses) = new TimerTask() {
		public void run() {
			//Need keep alive byte to be consistant across switches
		    byte[] buf = new byte[1000];
		    for (InetAddress address:addresses){
				DatagramPacket KEEP_ALIVE = new DatagramPacket(buf, buf.length, address, port);
				Socket.send(KEEP_ALIVE);
		    }
		}
	}
	//Define the TOPOLOGY_UPDATE task
	static TimerTask topologyUpdate(DatagramSocket Socket,  int port, InetAddress address, byte[] topology) = new TimerTask() {
		public void run() {
			//System.out.println("sending topology_update");(useful for debugging)
			byte[] buf = topology;
			DatagramPacket TOPOLOGY_UPDATE = new DatagramPacket(buf, buf.length, address, port);
			Socket.send(TOPOLOGY_UPDATE);
		}
	}
	
	//Main function class
	public static void main(String[] args) {
		
		//Define switches unique IP (needs to be adapted to take command line argument)
		InetAddress myIP = InetAddress.getByName("127.0.0.2");
		//Define the Port and controller IP address to use
		//NB MUST be same as for controller
		int PORT = 1025;
		InetAddress CONTROLLERIP = InetAddress.getByName("127.0.0.1");
		//Initialise Socket
		DatagramSocket Socket = initSwitchSocket(PORT, CONTROLLERIP);
		
		//Send initial REGISTER_REQUEST message format must be consistent with controller
		String sendMessage = "REGISTER_REQUEST";
		byte[] dataByte = sendMessage.getBytes("UTF-8");
		DatagramPacket registerRequest = new DatagramPacket(dataByte, 16, CONTROLLERIP, PORT);
		Socket.send(registerRequest);
		
		//Setup timer (runs on its own thread)
		Timer periodicTimer = new Timer();
		//Initialise (empty) list for neighbouring switches IP addresses and empty topology
		List<InetAddress> switchIPs = new ArrayList<InetAddress>();
		byte[] currentTopology = new byte [0];
		
		//Send keepAlive and TopologyUpdate messages every k milliseconds
		final int k = 1000; 
		//k currently arbitrarily set at 1000 --TODO Needs reviewing
		periodicTimer.schedule(sendKeepAlive(Socket, PORT, switchIPs), 0, k);
		periodicTimer.schedule(topologyUpdate(Socket, PORT, CONTROLLERIP, currentTopology), 0, k);
		
		//Initialise a data buffer and packet to use for incoming data 
		byte[] inBuffer = new byte[1000];
		//TODO buffer size may require changing
		DatagramPacket incomingData = new DatagramPacket(inBuffer, inBuffer.length);
		
		//Main program thread
		while(true) {
			Socket.receive(incomingData);
			InetAddress incomingAddress = incomingData.getAddress();
			byte[] incomingMessage = incomingData.getData();
			String receivedMessage = new String(incomingMessage, 0, incomingData.getLength());
			
			if (incomingAddress == CONTROLLERIP) {
				if (receivedMessage.equals("REGISTER_RESPONSE")) {
					//TODO update topology and hence switchIPs
					sendKeepAlive(Socket, PORT, switchIPs);
				}
				else if (receivedMessage.equals("ROUTE_UPDATE")) {
					//TODO update routing table	
				}
				//TODO check for ROUTE_UPDATE and update routing table
				//Also check for data to be sent on, check where it should go using routing table
				//For current network only one switch and controller so simple topology
			}
			else if (incomingAddress == myIP){
				//TODO essentially ignore packets from self (Shouldn't happen)
			}
			else {
				//packets from other switches
				if (receivedMessage.equals("KEEP_ALIVE")) {
						switchIPs.add(incomingAddress);
				}
				//TODO deal with packets to be sent onwards, Also need to implement expiry on IP addresses
			}
		}
	//Need to close Socket when done
	Socket.close();
	}
}
