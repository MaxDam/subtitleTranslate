package com.maxdam.udemysubtitletranslator;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.text.StringEscapeUtils;
import org.fredy.jsrt.api.SRT;
import org.fredy.jsrt.api.SRTInfo;
import org.fredy.jsrt.api.SRTReader;
import org.fredy.jsrt.api.SRTWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.noop.subtitle.model.SubtitleCue;
import fr.noop.subtitle.util.SubtitlePlainText;
import fr.noop.subtitle.util.SubtitleTextLine;
import fr.noop.subtitle.vtt.VttCue;
import fr.noop.subtitle.vtt.VttObject;
import fr.noop.subtitle.vtt.VttParser;
import fr.noop.subtitle.vtt.VttWriter;

public class ServiceTranslate extends IntentService {

    private String TAG = ServiceTranslate.class.getName();

    public static final String LOG_TRANSLATE = "broadcast.LOG_TRANSLATE";

    private SharedPreferences prefs;

    //semaforo che indica esserci una sincronizzazione in atto
    public static final AtomicBoolean inProgress = new AtomicBoolean(false);

    public ServiceTranslate() {
        super("ServiceTranslate");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        //ottiene le preferenze
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //se e' gia attivo un tracing.. esce
        if (inProgress.get()) return;

        try {
            //imposta ad on il semaforo
            inProgress.set(true);

            File mainDir = new File(SUBTITLE_PATH);
            logInfo("main dir: " + mainDir.getPath() + "\n\n");
            exploreDirFiles(mainDir);
        }
        finally {
            //reimposta a off il semaforo
            inProgress.set(false);
        }
    }

    private void logInfo(String message) {
        //invia il broadcast per notificare il logInfo
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LOG_TRANSLATE);
        broadcastIntent.putExtra("message", message);
        this.sendBroadcast(broadcastIntent);*/

        Log.i(LogcatViewerActivity.LOG_TAG, message);
    }

    private void logDebug(String message) {
        //invia il broadcast per notificare il logInfo
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LOG_TRANSLATE);
        broadcastIntent.putExtra("message", message);
        this.sendBroadcast(broadcastIntent);*/

        Log.d(LogcatViewerActivity.LOG_TAG, message);
    }

    private void logWarning(String message) {
        //invia il broadcast per notificare il logInfo
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LOG_TRANSLATE);
        broadcastIntent.putExtra("message", message);
        this.sendBroadcast(broadcastIntent);*/

        Log.w(LogcatViewerActivity.LOG_TAG, message);
    }

    private void logError(String message) {
        //invia il broadcast per notificare il logInfo
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LOG_TRANSLATE);
        broadcastIntent.putExtra("message", message);
        this.sendBroadcast(broadcastIntent);*/

        Log.e(LogcatViewerActivity.LOG_TAG, message);
    }

    private static final String SUBTITLE_PATH = "/storage/emulated/0/Android/data/com.udemy.android/files/udemy-subtitle-downloads";
    private static final String FILE_SRT_ENGLISH_NAME = "en_US.srt";
    private static final String FILE_SRT_ENGLISH_BACKUP_NAME = "en_US.srt.old";
    private static final String FILE_SRT_ITALIAN_NAME = "it_IT.srt";
    private static final String INPUT_LANGUAGE = "en";
    private static final String OUTPUT_LANGUAGE = "it";

    //scansiona la directory dei sottotitoli
    public void exploreDirFiles(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    exploreDirFiles(file);
                } else {
                    if(file.getName().equals(FILE_SRT_ENGLISH_NAME)) {
                        if(!new File(file.getPath().replace(FILE_SRT_ENGLISH_NAME, FILE_SRT_ENGLISH_BACKUP_NAME)).exists()) {
                            boolean result = translateWebvtt(file);
                            if(!result) translateSrt(file);
                        }
                    }
                }
            }
        }
    }

    //translate webvtt file
    private boolean translateWebvtt(File fileInput) {
        try {
            logWarning("read file: " + fileInput.getParentFile().getName() + "/" + fileInput.getName() + "\n");
            VttObject vttObjOutput = new VttObject();
            VttParser parser = new VttParser("utf-8");
            VttObject subtitleInput = parser.parse(new FileInputStream(fileInput), false, false);
            for (SubtitleCue subtitleCueInput : subtitleInput.getCues()) {

                //effettua la traduzione
                String translatedText = subtitleCueInput.getText();
                try {
                    logInfo("input: " + subtitleCueInput.getText() + "\n");
                    translatedText = translateFromGoogle(INPUT_LANGUAGE, OUTPUT_LANGUAGE, subtitleCueInput.getText());
                    logInfo("output: " + translatedText + "\n");
                }
                catch (Exception e) {
                    e.printStackTrace();
                    logError("error translate: " + e.getMessage() + "\n");
                }

                //scrive il Cue di output e lo accoda all'oggetto di output
                VttCue subtitleCueOutput = new VttCue ();
                subtitleCueOutput.setStartTime(subtitleCueInput.getStartTime());
                subtitleCueOutput.setEndTime(subtitleCueInput.getEndTime());
                SubtitleTextLine subtitleTextLineOutput = new SubtitleTextLine();
                subtitleTextLineOutput.addText(new SubtitlePlainText(translatedText));
                subtitleCueOutput.addLine(subtitleTextLineOutput);
                vttObjOutput.addCue(subtitleCueOutput);
            }
            fileInput.renameTo(new File(fileInput.getName().replace(FILE_SRT_ENGLISH_NAME, FILE_SRT_ENGLISH_BACKUP_NAME)));
            //File fileOutput = new File(fileInput.getPath(), FILE_SRT_ITALIAN_NAME);
            File fileOutput = new File(fileInput.getPath(), fileInput.getName());
            //fileOutput.deleteOnExit();
            VttWriter writer = new VttWriter("utf-8");
            writer.write(vttObjOutput, new FileOutputStream(fileOutput));
            logWarning("write file: " + fileOutput.getCanonicalPath() + "\n\n");
            return true;
        }
        catch(Exception e) {
            e.printStackTrace();
            logError("error: " + e.getMessage() + "\n\n");
            return false;
        }
    }

    //translate srt file
    private boolean translateSrt(File fileInput) {
        try {
            logWarning("read file: " + fileInput.getParentFile().getName() + "/" + fileInput.getName() + "\n");
            SRTInfo infoOutput = new SRTInfo();
            SRTInfo infoInput = SRTReader.read(fileInput);
            for (SRT s : infoInput) {
                StringBuffer outputText = new StringBuffer();
                for (String lineInput : s.text) {
                    logInfo("input: " + lineInput + "\n");
                    try {
                        String lineOutput = translateFromGoogle(INPUT_LANGUAGE, OUTPUT_LANGUAGE, lineInput);
                        outputText.append(lineOutput);
                        logInfo("output: " + lineOutput + "\n");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        logError("error translate: " + e.getMessage() + "\n");
                    }
                }
                infoOutput.add(new SRT(s.number, s.startTime, s.endTime, outputText.toString()));
            }
            fileInput.renameTo(new File(fileInput.getName().replace(FILE_SRT_ENGLISH_NAME, FILE_SRT_ENGLISH_BACKUP_NAME)));
            //File fileOutput = new File(fileInput.getPath(), FILE_SRT_ITALIAN_NAME);
            File fileOutput = new File(fileInput.getPath(), fileInput.getName());
            //fileOutput.deleteOnExit();
            SRTWriter.write(fileOutput, infoOutput);
            logWarning("write file: " + fileOutput.getCanonicalPath() + "\n\n");
            return true;
        }
        catch(Exception e) {
            e.printStackTrace();
            logError("error: " + e.getMessage() + "\n\n");
            return false;
        }
    }

    //invoca google translate per tradurre il file
    private String translateFromGoogle(String langFrom, String langTo, String text) throws IOException {
        String urlStr = String.format("https://translate.google.com/m?hl=%s&sl=%s&q=%s", langTo, langFrom, URLEncoder.encode(text, "UTF-8"));
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        String responseFlat = StringEscapeUtils.unescapeHtml4(response.toString());
        String expression = "class=\"t0\">(.*?)<";
        //String expression = "<div dir=\"ltr\" class=\"t0\">(.*?)</div>";
        String result = text;
        Matcher m =  Pattern.compile(expression).matcher(responseFlat);
        if (m.find()) {
            result = m.group(1);
        }
        return result;
    }
}
