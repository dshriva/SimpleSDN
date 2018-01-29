package com.bootstrap;

import com.node.Controller;
import com.node.Switch;

import java.io.IOException;

/*
 * created by divya at 1/17/2018
 */
public class Starter {

    private static int port = 0;
    private static String id = null;
    private static String host = null;

    public static void main(String args[]) throws IOException, ClassNotFoundException {

        // Checking the arguments
        if (!validateInput(args)) {
            System.out.println("Terminating");
            return;
        }

        System.out.println("The port number set for this machine is " + port);

    }

    private static boolean validateInput(String[] args) throws IOException, ClassNotFoundException {
        if (args != null && args.length >= 2) {
            System.out.println("Bootstrapping your machine");
            System.out.println("Port Number entered is : " + args[0]);
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                //nfe.printStackTrace();
                System.out.println("Enter a valid port number. Try again");
                return false;
            }

            if (args[1].equals("c")) {
                System.out.println("It is a Controller");
                Controller controller = new Controller(port);
                controller.readConfigFile();
                try {
                    controller.messageExchange(port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (args[1].equals("s")) {
                System.out.println("It is a Switch");
                System.out.println("Ip entered is : "+args[2]);
                Switch newSwitch = new Switch(args[0], args[2]);
                newSwitch.messageExchangeinSwitch(port, id, host);

            } else {
                System.out.println("Invalid argument. Enter a switch or a Controller.");
            }

        } else {
            System.out.println("Invalid argument");
            return false;
        }
        return true;
    }
}
