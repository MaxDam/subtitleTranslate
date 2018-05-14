package com.maxdam.udemysubtitletranslator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.fredy.jsrt.api.SRT;
import org.fredy.jsrt.api.SRTInfo;
import org.fredy.jsrt.api.SRTReader;
import org.fredy.jsrt.api.SRTTimeFormat;
import org.fredy.jsrt.api.SRTWriter;
import org.fredy.jsrt.editor.SRTEditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.noop.subtitle.base.BaseSubtitleCue;
import fr.noop.subtitle.model.SubtitleCue;
import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleText;
import fr.noop.subtitle.util.SubtitlePlainText;
import fr.noop.subtitle.util.SubtitleTextLine;
import fr.noop.subtitle.util.SubtitleTimeCode;
import fr.noop.subtitle.vtt.VttCue;
import fr.noop.subtitle.vtt.VttObject;
import fr.noop.subtitle.vtt.VttParser;
import fr.noop.subtitle.vtt.VttWriter;

public class MainActivity extends AppCompatActivity {

    private Button translateBtn;
    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        translateBtn = (Button) findViewById(R.id.translate);
        log = (TextView) findViewById(R.id.log);

        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    translateBtn.setEnabled(false);

                    //chiama l'activity di log
                    Intent intent = new Intent(MainActivity.this, LogcatViewerActivity.class);
                    startActivity(intent);
                }
                finally {
                    translateBtn.setEnabled(true);
                }
            }
        });

        //receiver di log
        IntentFilter filterPredict = new IntentFilter();
        filterPredict.addAction(ServiceTranslate.LOG_TRANSLATE);
        registerReceiver(receiverLog, filterPredict);
    }

    //evento di log
    private BroadcastReceiver receiverLog = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            log.append(intent.getStringExtra("message"));
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        //register receiver di log
        IntentFilter filterTraining = new IntentFilter();
        filterTraining.addAction(ServiceTranslate.LOG_TRANSLATE);
        registerReceiver(receiverLog, filterTraining);
    }

    @Override
    protected void onPause() {
        //unregister dei receivers
        unregisterReceiver(receiverLog);

        super.onPause();
    }
}
