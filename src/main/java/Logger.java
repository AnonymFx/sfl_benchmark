public class Logger {
    enum LogLevel {
        VERBOSE,
        DEBUG,
        NONE
    }

    private static LogLevel LOG_LEVEL = LogLevel.VERBOSE;

    public static void debugLog(String text) {
        if (LOG_LEVEL == LogLevel.VERBOSE || LOG_LEVEL == LogLevel.DEBUG) {
            System.out.println(text);
        }
    }

    public static void debugLog(String logTag, String text) {
        if (LOG_LEVEL == LogLevel.VERBOSE || LOG_LEVEL == LogLevel.DEBUG) {
            System.out.println(logTag + ": " + text);
        }
    }

    public static void verboseLog(String text) {
        if (LOG_LEVEL == LogLevel.VERBOSE) {
            System.out.println(text);
        }
    }

    public static void verboseLog(String logTag, String text) {
        if (LOG_LEVEL == LogLevel.VERBOSE) {
            System.out.println(logTag + ": " + text);
        }
    }

}
