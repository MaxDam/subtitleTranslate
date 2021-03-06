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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

            File mainDir = new File(prefs.getString("subtitle_path", CommonStuff.SUBTITLE_PATH));
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
        String logType = "I";
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LOG_TRANSLATE);
        broadcastIntent.putExtra("message", logType + message);
        this.sendBroadcast(broadcastIntent);

        Log.i(CommonStuff.LOG_TAG, message);
    }

    private void logDebug(String message) {
        //invia il broadcast per notificare il logInfo
        String logType = "D";
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LOG_TRANSLATE);
        broadcastIntent.putExtra("message", logType + message);
        this.sendBroadcast(broadcastIntent);

        //Log.d(CommonStuff.LOG_TAG, message);
    }

    private void logWarning(String message) {
        //invia il broadcast per notificare il logInfo
        String logType = "W";
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LOG_TRANSLATE);
        broadcastIntent.putExtra("message", logType + message);
        this.sendBroadcast(broadcastIntent);

        //Log.w(CommonStuff.LOG_TAG, message);
    }

    private void logError(String message) {
        //invia il broadcast per notificare il logInfo
        String logType = "E";
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LOG_TRANSLATE);
        broadcastIntent.putExtra("message", logType + message);
        this.sendBroadcast(broadcastIntent);

        //Log.e(CommonStuff.LOG_TAG, message);
    }

    //scansiona la directory dei sottotitoli
    public void exploreDirFiles(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    exploreDirFiles(file);
                } else {
                    if(file.getName().equals(CommonStuff.FILE_SRT_ENGLISH_NAME)) {

                        //ottiene il file di backup ed il file in italiano
                        File fileEnglishBackup = new File(file.getPath().replace(CommonStuff.FILE_SRT_ENGLISH_NAME, CommonStuff.FILE_SRT_ENGLISH_BACKUP_NAME));
                        File fileItalian = new File(file.getPath().replace(CommonStuff.FILE_SRT_ENGLISH_NAME, CommonStuff.FILE_SRT_ITALIAN_NAME));

                        //per elaborare il file non devono esistere sia il file di backup che il file in italiano
                        //oppure il file backup ed il file esaminato devono avere almeno un minuto di differenza nell'ultima modifica
                        if( (!fileEnglishBackup.exists() && !fileItalian.exists())
                        ||  (fileEnglishBackup.exists() && Math.abs(file.lastModified() - fileEnglishBackup.lastModified()) > (1*60*1000)) ) {
                            //logInfo("file: " + file.lastModified()+"\n"+"file bak: " + fileEnglishBackup.lastModified()+"\n"+"diff: " + Math.abs(file.lastModified() - fileEnglishBackup.lastModified()));

                            boolean     result = translateWebvtt(file, false);
                            if(!result) result = translateWebvtt(file, true);
                            if(!result) result = translateSrt(file);
                        }
                    }
                }
            }
        }
    }

    //translate webvtt file
    private boolean translateWebvtt(File fileInput, boolean withHours) {
        try {
            logWarning("read file: " + fileInput.getParentFile().getName() + "/" + fileInput.getName());
            VttObject vttObjOutput = new VttObject();
            VttParser parser = new VttParser("utf-8");
            VttObject subtitleInput = parser.parse(new FileInputStream(fileInput), false, withHours);
            int errorCount = 0;
            for (SubtitleCue subtitleCueInput : subtitleInput.getCues()) {

                //effettua la traduzione
                String translatedText = subtitleCueInput.getText();
                try {
                    translatedText = translateFromGoogle(CommonStuff.INPUT_LANGUAGE, CommonStuff.OUTPUT_LANGUAGE, subtitleCueInput.getText());
                    logInfo("input: " + subtitleCueInput.getText()+"\n"+"output: " + translatedText);
                }
                catch (Exception e) {
                    logError("error translate: " + e.getMessage());
                    errorCount++;
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

            //se si e' raggiunto il massimo degli errori ammessi non scrive il file
            if(errorCount > CommonStuff.MAX_ERROR_COUNT) {
                logError("error: raggiunto il numero massimo di errori ammessi" + "\n\n");
                return false;
            }

            //effettua una copia di backup del file originale
            File fileInputBackup = new File(fileInput.getParent(), CommonStuff.FILE_SRT_ENGLISH_BACKUP_NAME);
            copyFile(fileInput, fileInputBackup);

            //crea i file di output
            File fileOutputItalian = new File(fileInput.getParent(), CommonStuff.FILE_SRT_ITALIAN_NAME);
            File fileOutputEnglish = new File(fileInput.getParent(), fileInput.getName());

            //scrive i file in uscita
            VttWriter writer = new VttWriter("utf-8");
            writer.write(vttObjOutput, new FileOutputStream(fileOutputItalian));
            writer.write(vttObjOutput, new FileOutputStream(fileOutputEnglish));
            logWarning("write file: " + fileOutputEnglish.getCanonicalPath() + "\n\n");
            return true;
        }
        catch(Exception e) {
            logError("error: " + e.getMessage() + "\n\n");
            return false;
        }
    }

    //translate webvtt file v2 //TODO da rivedere completamente
    private boolean translateWebvtt_v2(File fileInput, boolean withHours) {

        int MAX_WORD_COUNT = 5000;
        String TEXT_SEPARATOR = "...";

        String textToTranslate = "";
        List<String> txtList = new ArrayList<String>();
        List<VttCue> objList = new ArrayList<VttCue>();

        try {
            logWarning("read file: " + fileInput.getParentFile().getName() + "/" + fileInput.getName());
            VttObject vttObjOutput = new VttObject();
            VttParser parser = new VttParser("utf-8");
            VttObject subtitleInput = parser.parse(new FileInputStream(fileInput), false, withHours);

            //scorre tutti i sottotitoli
            for (SubtitleCue subtitleCueInput : subtitleInput.getCues()) {

                //aggiunge il testo alla lista
                txtList.add(subtitleCueInput.getText());

                //crea l'oggetto VttCue e lo aggiunge alla lista
                VttCue subtitleCueOutput = new VttCue();
                subtitleCueOutput.setStartTime(subtitleCueInput.getStartTime());
                subtitleCueOutput.setEndTime(subtitleCueInput.getEndTime());
                SubtitleTextLine subtitleTextLineOutput = new SubtitleTextLine();
                objList.add(subtitleCueOutput);

                //aggiunge l'oggetto all'output
                vttObjOutput.addCue(subtitleCueOutput);
            }

            //scorre la lista di testi da tradurre
            int lastTranslatedIndex = 0;
            for(int i = 0; i < txtList.size(); i++) {
                String newTextToTranslate = textToTranslate + (i > 0 ? TEXT_SEPARATOR : "") + txtList.get(i);

                //se supera il numero massimo di parole ..effettua la traduzione
                if(newInputText.length() > MAX_WORD_COUNT) {
                    lastTranslatedIndex = i;
                    boolean esito = translate(textToTranslate);
                    if(!esito) break;
                }

                textToTranslate = newTextToTranslate;
            }
            //effetua la traduzione dell'inputText rimanente
            boolean esito = translate(textToTranslate);

            //effettua una copia di backup del file originale
            File fileInputBackup = new File(fileInput.getParent(), CommonStuff.FILE_SRT_ENGLISH_BACKUP_NAME);
            copyFile(fileInput, fileInputBackup);

            //crea i file di output
            File fileOutputItalian = new File(fileInput.getParent(), CommonStuff.FILE_SRT_ITALIAN_NAME);
            File fileOutputEnglish = new File(fileInput.getParent(), fileInput.getName());

            //scrive i file in uscita
            VttWriter writer = new VttWriter("utf-8");
            writer.write(vttObjOutput, new FileOutputStream(fileOutputItalian));
            writer.write(vttObjOutput, new FileOutputStream(fileOutputEnglish));
            logWarning("write file: " + fileOutputEnglish.getCanonicalPath() + "\n\n");
            return true;
        }
        catch(Exception e) {
            logError("error: " + e.getMessage() + "\n\n");
            return false;
        }

        //metodo translate
        boolean translate(String textToTranslate) {
            //se invece il nuovo testo supera la lunghezza totale traduce quello accumulato fino ad ora
            try {
                translatedText = translateFromGoogle(CommonStuff.INPUT_LANGUAGE, CommonStuff.OUTPUT_LANGUAGE, textToTranslate);
                logInfo("input: " + subtitleCueInput.getText()+"\n"+"output: " + translatedText);
            }
            catch (Exception e) {
                logError("error translate: " + e.getMessage());
                return false;
            }

            //splitta il testo traslato per il separatore
            String[] translatedTextTokens = translatedText.split(TEXT_SEPARATOR);

            //se la lunghezza delle linee è diversa dal buffer, c'è un errore
            if(buffer.size() != translatedTextTokens.lenght()) {
                logError("error translate: size discrepance");
                return false;
            }

            //scorre i token del testo traslato e li aggiunge al buffer dell'srt
            for(int i = 0; i < translatedTextTokens.lenght(); i++) {
                VttCue subtitleCueOutput = objList.get(i + lastTranslatedIndex);
                subtitleTextLineOutput.addText(new SubtitlePlainText(translatedTextTokens[i]));
                subtitleCueOutput.addLine(subtitleTextLineOutput);
            }

            return true;
        }
    }

    //translate srt file
    private boolean translateSrt(File fileInput) {
        try {
            logWarning("read file: " + fileInput.getParentFile().getName() + "/" + fileInput.getName());
            SRTInfo infoOutput = new SRTInfo();
            SRTInfo infoInput = SRTReader.read(fileInput);
            int errorCount = 0;
            for (SRT s : infoInput) {
                StringBuffer outputText = new StringBuffer();
                for (String lineInput : s.text) {
                    logInfo("input: " + lineInput);
                    try {
                        String lineOutput = translateFromGoogle(CommonStuff.INPUT_LANGUAGE, CommonStuff.OUTPUT_LANGUAGE, lineInput);
                        outputText.append(lineOutput);
                        logInfo("output: " + lineOutput);
                    }
                    catch (Exception e) {
                        logError("error translate: " + e.getMessage());
                        errorCount++;
                    }
                }
                infoOutput.add(new SRT(s.number, s.startTime, s.endTime, outputText.toString()));
            }

            //se si e' raggiunto il massimo degli errori ammessi non scrive il file
            if(errorCount > CommonStuff.MAX_ERROR_COUNT) {
                logError("error: raggiunto il numero massimo di errori ammessi" + "\n\n");
                return false;
            }

            //effettua una copia di backup del file originale
            File fileInputBackup = new File(fileInput.getParent(), CommonStuff.FILE_SRT_ENGLISH_BACKUP_NAME);
            copyFile(fileInput, fileInputBackup);

            //crea i file di output
            File fileOutputItalian = new File(fileInput.getParent(), CommonStuff.FILE_SRT_ITALIAN_NAME);
            File fileOutputEnglish = new File(fileInput.getParent(), fileInput.getName());

            //scrive i file in uscita
            SRTWriter.write(fileOutputItalian, infoOutput);
            SRTWriter.write(fileOutputEnglish, infoOutput);
            logWarning("write file: " + fileOutputEnglish.getCanonicalPath() + "\n\n");
            return true;
        }
        catch(Exception e) {
            logError("error: " + e.getMessage() + "\n\n");
            return false;
        }
    }

    //translate srt file v2 //TODO modiicato ma da rivedere
    private boolean translateSrt_v2(File fileInput) {

        int MAX_WORD_COUNT = 5000;
        String TEXT_SEPARATOR = "...";

        String textToTranslate = "";
        List<String> txtList = new ArrayList<String>();
        List<SRT> objList = new ArrayList<SRT>();

        try {
            logWarning("read file: " + fileInput.getParentFile().getName() + "/" + fileInput.getName());
            SRTInfo infoOutput = new SRTInfo();
            SRTInfo infoInput = SRTReader.read(fileInput);
            int errorCount = 0;

            //scorre tutte le linee del SRT
            for (SRT s : infoInput) {

                //accumula il testo per le varie linee
                String txt = "";
                for (String lineInput : s.text)
                    txt += " " + lineInput;

                //aggiunge il testo alla lista
                txtList.add(txt);

                //crea l'oggetto STR e lo aggiunge alla lista
                SRT srt = new SRT(s.number, s.startTime, s.endTime, txt);
                objList.add(srt);

                //aggiunge l'oggetto all'output
                infoOutput.add(srt);
            }

            //scorre la lista di testi da tradurre
            int lastTranslatedIndex = 0;
            for(int i = 0; i < txtList.size(); i++) {
                String newTextToTranslate = textToTranslate + (i > 0 ? TEXT_SEPARATOR : "") + txtList.get(i);

                //se supera il numero massimo di parole ..effettua la traduzione
                if(newInputText.length() > MAX_WORD_COUNT) {
                    lastTranslatedIndex = i;
                    boolean esito = translate(textToTranslate);
                    if(!esito) break;
                }

                textToTranslate = newTextToTranslate;
            }
            //effetua la traduzione dell'inputText rimanente
            boolean esito = translate(textToTranslate);

            //effettua una copia di backup del file originale
            File fileInputBackup = new File(fileInput.getParent(), CommonStuff.FILE_SRT_ENGLISH_BACKUP_NAME);
            copyFile(fileInput, fileInputBackup);

            //crea i file di output
            File fileOutputItalian = new File(fileInput.getParent(), CommonStuff.FILE_SRT_ITALIAN_NAME);
            File fileOutputEnglish = new File(fileInput.getParent(), fileInput.getName());

            //scrive i file in uscita
            SRTWriter.write(fileOutputItalian, infoOutput);
            SRTWriter.write(fileOutputEnglish, infoOutput);
            logWarning("write file: " + fileOutputEnglish.getCanonicalPath() + "\n\n");
            return true;
        }
        catch(Exception e) {
            logError("error: " + e.getMessage() + "\n\n");
            return false;
        }

        //metodo translate
        boolean translate(String textToTranslate) {
            //se invece il nuovo testo supera la lunghezza totale traduce quello accumulato fino ad ora
            try {
                translatedText = translateFromGoogle(CommonStuff.INPUT_LANGUAGE, CommonStuff.OUTPUT_LANGUAGE, textToTranslate);
                logInfo("input: " + subtitleCueInput.getText()+"\n"+"output: " + translatedText);
            }
            catch (Exception e) {
                logError("error translate: " + e.getMessage());
                return false;
            }

            //splitta il testo traslato per il separatore
            String[] translatedTextTokens = translatedText.split(TEXT_SEPARATOR);

            //se la lunghezza delle linee è diversa dal buffer, c'è un errore
            if(buffer.size() != translatedTextTokens.lenght()) {
                logError("error translate: size discrepance");
                return false;
            }

            //scorre i token del testo traslato e li aggiunge al buffer dell'srt
            for(int i = 0; i < translatedTextTokens.lenght(); i++) {
                STR str = objList.get(i + lastTranslatedIndex);
                str.setText(translatedTextTokens[i]);
            }

            return true;
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
        String result = text;
        Matcher m =  Pattern.compile(expression).matcher(responseFlat);
        if (m.find()) {
            result = m.group(1);
        }
        return result;
    }

    //copia un file
    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }


}
