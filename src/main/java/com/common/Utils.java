package com.common;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    public static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);


    public static Logger getLogger() {
        return LOGGER;
    }


    public static void logInfo(String info) {
        LOGGER.log(Level.INFO,  info);
    }

    public static void logError(String err) {
        LOGGER.log(Level.SEVERE, err);
    }

    public static void logDebug(String msg) {
        LOGGER.log(Level.WARNING, msg);
    }

}
