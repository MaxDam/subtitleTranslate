package com.maxdam.udemysubtitletranslator;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button translateBtn;
    private Button settingsBtn;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        //ottiene le preferenze
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //bottoni
        translateBtn = (Button) findViewById(R.id.translate);
        settingsBtn = (Button) findViewById(R.id.settings);

        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    translateBtn.setEnabled(false);

                    isReadStoragePermissionGranted();
                    isWriteStoragePermissionGranted();

                    //chiama l'activity di log
                    Intent intent = new Intent(MainActivity.this, LogcatViewerActivity.class);
                    startActivity(intent);
                }
                finally {
                    translateBtn.setEnabled(true);
                }
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //chiama l'activity di setting
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    public  boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(CommonStuff.LOG_TAG,"Permission is granted read");
                return true;
            }
            else {
                Log.v(CommonStuff.LOG_TAG,"Permission is revoked read");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(CommonStuff.LOG_TAG,"Permission is granted read");
            return true;
        }
    }

    public  boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(CommonStuff.LOG_TAG,"Permission is granted write");
                return true;
            }
            else {
                Log.v(CommonStuff.LOG_TAG,"Permission is revoked write");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(CommonStuff.LOG_TAG,"Permission is granted write");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2: {
                Log.d(CommonStuff.LOG_TAG, "Write external storage");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(CommonStuff.LOG_TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                }
                break;
            }
            case 3: {
                Log.d(CommonStuff.LOG_TAG, "Read external storage");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(CommonStuff.LOG_TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                }
                break;
            }
        }
    }
}
