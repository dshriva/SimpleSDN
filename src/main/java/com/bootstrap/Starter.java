package com.bootstrap;

import com.domain.Controller;
import com.domain.Switch;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import java.io.IOException;
import java.security.acl.LastOwnerException;

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
        // java -jar <snapshot_name.jar> <OwnPort> <s> <ControllerIp> <SwitchId> <-f> <1,2,4> <-l> <f/e/i/d>
        // java -jar <snapshot_name.jar> <OwnPort> <c> <-l> <f/e/i/d>
        if (!validateInput(args)) {
            System.out.println("Terminating");
            _logger.error("Terminating");
            return;
        }
        _logger.trace("The port number set for this machine is " + port);
        System.out.println("The port number set for this machine is " + port);
    }

    private static boolean validateInput(String[] args) throws IOException {
        boolean isLoggingInitiazed = false;
        if (args != null && args.length >= 2) {
            _logger.trace("Bootstrapping your machine");
            System.out.println("Bootstrapping your machine");
            _logger.trace("Port Number entered is : " + args[0]);
            System.out.println("Port Number entered is : " + args[0]);
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                _logger.error(nfe.getMessage());
                System.out.println("Enter a valid port number. Try again");
                return false;
            }

            // java -jar <snapshot_name.jar> <OwnPort> <c> <-l> <f/e/i/d>
            if (args[1].equals("c")) {
                if (args.length > 3
                        && args[2].equalsIgnoreCase("-l")
                        && args[3].length() == 1) {
                    char c = args[3].charAt(0);
                    if (c == 'f' || c == 'e' || c == 'i' || c == 'd' || c == 't') {
                        isLoggingInitiazed = initLogging("c", "0", c);
                    } else {
                        isLoggingInitiazed = initLogging("c", "0", 'e');
                    }

                }

                if(!isLoggingInitiazed) {
                    initLogging("c", "0", 'e');
                }

                System.out.println("It is a Controller");
                _logger.debug("It is a Controller");
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

                id = args[4];
                host = args[2];

                // java -jar <snapshot_name.jar> <OwnPort> <s> <ControllerIp> <controllerPort> <SwitchId> <-f> <1,2,4> <-l> <f/e/i/d>
                if (args.length > 6) {
                   if (args[5].equalsIgnoreCase("-l")) {
                        char c = args[6].charAt(0);
                        if (c == 'f' || c == 'e' || c == 'i' || c == 'd' || c == 't') {
                            isLoggingInitiazed = initLogging("s", id, c);
                        } else {
                            isLoggingInitiazed = initLogging("s", id, 'e');
                        }
                    }

                    if (args.length > 8
                            && args[7].equalsIgnoreCase("-l")
                            && args[8].length() == 1) {
                        char c = args[8].charAt(0);
                        if (c == 'f' || c == 'e' || c == 'i' || c == 'd' || c == 't') {
                            isLoggingInitiazed = initLogging("s", id, c);
                        } else {
                            isLoggingInitiazed = initLogging("s", id, 'e');
                        }

                    }
                }

                if(!isLoggingInitiazed) {
                    initLogging("s", id, 'e');
                }

                Switch aSwitch = new Switch(port, id, host, Integer.parseInt(args[3]));
                if (args.length > 6 && args[5].equalsIgnoreCase("-f")) {
                    String[] unreachableSwitches = args[6].split(",");
                    for (String unreachableSwitch : unreachableSwitches) {
                        aSwitch.getUnreachableSwitches().add(unreachableSwitch);
                    }
                }
                aSwitch.startSwitch();
                _logger.info("It is a Switch");
                _logger.info("Switch port is" + args[0]);
                System.out.println("Switch port is" + args[0]);
                System.out.println("Controller Ip entered is : " + args[2]);
                _logger.info("Controller Ip entered is : " + args[2]);
                _logger.info("Controller port entered is : " + args[3]);
                System.out.println("Switch ID is :" + args[4]);
                _logger.info("Switch ID is :" + args[4]);

            } else {
                System.out.println("Invalid argument. Enter a switch or a Controller.");
                _logger.error("Invalid argument. Enter a switch or a Controller.");
            }

        } else {
            System.out.println("Invalid argument");
            _logger.error("Invalid argument");
            return false;
        }
        return true;
    }

    public static boolean initLogging(String type, String id, char level) {
        try {
            PatternLayout lyt = new PatternLayout("[%-5p] %d %c.class %t %m%n");
            RollingFileAppender rollingFileAppender = new RollingFileAppender(lyt, "SimpleSDN_" + type + "-" + id + ".log");
            rollingFileAppender.setLayout(lyt);
            rollingFileAppender.setName("LOGFILE");
            rollingFileAppender.setMaxFileSize("100MB");
            rollingFileAppender.activateOptions();
            Logger.getRootLogger().addAppender(rollingFileAppender);
            _logger.info("Log level passed is "+level);
            switch (level) {
                case 'f':
                    Logger.getRootLogger().setLevel(Level.FATAL);
                    break;
                case 'e':
                    Logger.getRootLogger().setLevel(Level.ERROR);
                    break;
                case 'i':
                    Logger.getRootLogger().setLevel(Level.INFO);
                    break;
                case 'd':
                    Logger.getRootLogger().setLevel(Level.DEBUG);
                    break;
                case 't':
                    Logger.getRootLogger().setLevel(Level.TRACE);
                    break;
                default:
                    Logger.getRootLogger().setLevel(Level.ERROR);
                    break;
            }

            return true;
        } catch (Exception e) {
            _logger.error(e.getMessage());
            return false;
        }
    }
}
