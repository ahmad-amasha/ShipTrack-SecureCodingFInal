package shiptrack;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MyLogger {

    private static final Logger LOGGER = Logger.getLogger("ShipTrackLogger");

    static {
        try {
            String logPath = "data/logfile.log";
            File logFile = new File(logPath);
            if (logFile.getParentFile() != null) {
                logFile.getParentFile().mkdirs();
            }

            FileHandler fileHandler = new FileHandler(logPath, true);
            fileHandler.setFormatter(new SimpleFormatter());

            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException | SecurityException e) {
            System.out.println("Logger initialization failed: " + e.getMessage());
        }
    }

    public static void writeToLog(String msg) {
        LOGGER.log(Level.INFO, msg);
    }

    public static void writeToWarning(String msg) {
        LOGGER.log(Level.WARNING, msg);
    }
}
