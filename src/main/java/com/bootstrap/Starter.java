package com.bootstrap;

/*
 * created by divya at 1/17/2018
 */
public class Starter {

    private static int port = 0;

    public static void main(String args[]) {

        // Checking the arguments
        if (!validateInput(args)) {
            System.out.println("Terminating");
            return;
        }

        System.out.println("The port number set for this machine is " + port);

    }

    private static boolean validateInput(String[] args) {
        if (args != null && args.length == 2) {
            System.out.println("Bootstrapping your machine");
            System.out.println("Port Number entered is : " + args[0]);

            if (args[1].equals("c")) {
                System.out.println("It is a Controller");

            } else if (args[1].equals("s")) {
                System.out.println("It is a Switch");
            } else {
                System.out.println("Invalid argument. Enter a switch or a Controller.");
            }

            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                //nfe.printStackTrace();
                System.out.println("Enter a valid port number. Try again");
                return false;
            }
        } else {
            System.out.println("Invalid argument");
            return false;
        }
        return true;
    }
}
