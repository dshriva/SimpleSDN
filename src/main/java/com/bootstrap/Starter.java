package com.bootstrap;

import com.domain.Controller;
import com.domain.Switch;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import java.io.IOException;

/*
 * created by divya at 1/17/2018
 */
public class Starter {

    private static int port = 0;
    private static String id = null;
    private static String host = null;
    public static Logger _logger = Logger.getLogger(String.valueOf(Starter.class));

    public static void main(String args[]) throws IOException, ClassNotFoundException {

        // Checking the arguments
        if (!validateInput(args)) {
            System.out.println("Terminating");
            return;
        }

        System.out.println("The port number set for this machine is " + port);

    }

    private static boolean validateInput(String[] args) throws IOException {
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

            initLogging();

            if (args[1].equals("c")) {
                System.out.println("It is a Controller");
                Controller controller = new Controller(port);
                controller.readConfigFile();
                controller.createSocket(port);
                controller.scheduleDisplay();
                try {
                    controller.messageExchange(port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (args[1].equals("s")) {
                System.out.println("It is a Switch");
                System.out.println("Ip entered is : "+args[2]);
                id =args[3];
                host =args[2];
                System.out.println("Switch ID is :" + args[3]);
                Switch aSwitch = new Switch(port,id,host,2999);
                aSwitch.startSwitch();

            } else {
                System.out.println("Invalid argument. Enter a switch or a Controller.");
            }

        } else {
            System.out.println("Invalid argument");
            return false;
        }
        return true;
    }

    public static boolean initLogging() {
        try {
            PatternLayout lyt = new PatternLayout("[%-5p] %d %c.class %t %m%n");
            RollingFileAppender rollingFileAppender = new RollingFileAppender(lyt, "SimpleSDN.log");
            rollingFileAppender.setLayout(lyt);
            rollingFileAppender.setName("LOGFILE");
            rollingFileAppender.setMaxFileSize("100MB");
            rollingFileAppender.activateOptions();
            Logger.getRootLogger().addAppender(rollingFileAppender);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
