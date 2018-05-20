package com.maxdam.udemysubtitletranslator;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

//https://stackoverflow.com/questions/12692103/read-logcat-programmatically-within-application
public class LogcatLogger {

    private static final String ANDROID_LOG_TIME_FORMAT = "MM-dd kk:mm:ss.SSS";
    private static SimpleDateFormat logCatDate = new SimpleDateFormat(ANDROID_LOG_TIME_FORMAT);

    public static String lineEnding = "\n";
    private final String logKey;

    private static List<String> logKeys = new ArrayList<String>();

    LogcatLogger(String tag) {
        logKey = tag;
        if (! logKeys.contains(tag)) logKeys.add(logKey);
    }

    public static class LogCapture {
        private String lastLogTime = null;
        public final String buffer;
        public final List<String> log, keys;
        LogCapture(String oLogBuffer, List<String>oLogKeys) {
            this.buffer = oLogBuffer;
            this.keys = oLogKeys;
            this.log = new ArrayList<>();
        }
        private void close() {
            if (isEmpty()) return;
            String[] out = log.get(log.size() - 1).split(" ");
            lastLogTime = (out[0]+" "+out[1]);
        }
        private boolean isEmpty() {
            return log.size() == 0;
        }
        public LogCapture getNextCapture() {
            LogCapture capture = getLogCat(buffer, lastLogTime, keys);
            if (capture == null || capture.isEmpty()) return null;
            return capture;
        }
        public String toString() {
            StringBuilder output = new StringBuilder();
            for (String data : log) {
                output.append(data+lineEnding);
            }
            return output.toString();
        }
    }

    /**
     * Get a list of the known log keys
     * @return copy only
     */
    public static List<String> getLogKeys() {
        return logKeys.subList(0, logKeys.size() - 1);
    }

    /**
     * Platform: Android
     * Get the logcat output in time format from a buffer for this set of static logKeys.
     * @param oLogBuffer logcat buffer ring
     * @return A log capture which can be used to make further captures.
     */
    public static LogCapture getLogCat(String oLogBuffer) { return getLogCat(oLogBuffer, null, getLogKeys()); }

    /**
     * Platform: Android
     * Get the logcat output in time format from a buffer for a set of log-keys; since a specified time.
     * @param oLogBuffer logcat buffer ring
     * @param oLogTime time at which to start capturing log data, or null for all data
     * @param oLogKeys logcat tags to capture
     * @return A log capture; which can be used to make further captures.
     */
    public static LogCapture getLogCat(String oLogBuffer, String oLogTime, List<String> oLogKeys) {
        try {

            List<String>sCommand = new ArrayList<String>();
            sCommand.add("logcat");
            sCommand.add("-bmain");
            sCommand.add("-vtime");
            sCommand.add("-s");
            sCommand.add("-d");

            sCommand.add("-T"+oLogTime);

            for (String item : oLogKeys) sCommand.add(item+":V"); // log level: ALL
            sCommand.add("*:S"); // ignore logs which are not selected

            Process process = new ProcessBuilder().command(sCommand).start();

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            LogCapture mLogCapture = new LogCapture(oLogBuffer, oLogKeys);
            String line = "";

            long lLogTime = logCatDate.parse(oLogTime).getTime();
            if (lLogTime > 0) {
                // Synchronize with "NO YEAR CLOCK" @ unix epoch-year: 1970
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date(oLogTime));
                calendar.set(Calendar.YEAR, 1970);
                Date calDate = calendar.getTime();
                lLogTime = calDate.getTime();
            }

            while ((line = bufferedReader.readLine()) != null) {
                long when = logCatDate.parse(line).getTime();
                if (when > lLogTime) {
                    mLogCapture.log.add(line);
                    break; // stop checking for date matching
                }
            }

            // continue collecting
            while ((line = bufferedReader.readLine()) != null) mLogCapture.log.add(line);

            mLogCapture.close();
            return mLogCapture;
        } catch (Exception e) {
            // since this is a log reader, there is nowhere to go and nothing useful to do
            return null;
        }
    }

    /**
     * "Error"
     * @param e
     */
    public void failure(Exception e) {
        Log.e(logKey, Log.getStackTraceString(e));
    }

    /**
     * "Error"
     * @param message
     * @param e
     */
    public void failure(String message, Exception e) {
        Log.e(logKey, message, e);
    }

    public void warning(String message) {
        Log.w(logKey, message);
    }

    public void warning(String message, Exception e) {
        Log.w(logKey, message, e);
    }

    /**
     * "Information"
     * @param message
     */
    public void message(String message) {
        Log.i(logKey, message);
    }

    /**
     * "Debug"
     * @param message a Message
     */
    public void examination(String message) {
        Log.d(logKey, message);
    }

    /**
     * "Debug"
     * @param message a Message
     * @param e An failure
     */
    public void examination(String message, Exception e) {
        Log.d(logKey, message, e);
    }

}
