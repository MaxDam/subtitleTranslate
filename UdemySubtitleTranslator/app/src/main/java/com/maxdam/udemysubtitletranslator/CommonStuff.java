package com.maxdam.udemysubtitletranslator;


public class CommonStuff {

    public static String LOG_TAG = "TRANSLATE_LOG";
    public static final String SUBTITLE_PATH = "/storage/emulated/0/Android/data/com.udemy.android/files/udemy-subtitle-downloads";
    public static final String FILE_SRT_ENGLISH_NAME = "en_US.srt";
    public static final String FILE_SRT_ENGLISH_BACKUP_NAME = "en_US.srt.old";
    public static final String FILE_SRT_ITALIAN_NAME = "it_IT.srt";
    public static final String INPUT_LANGUAGE = "en";
    public static final String OUTPUT_LANGUAGE = "it";

    public static final int FOLDER_MONITORING_INTERVAL = 1000 * 60 * 2; //ogni 2 minuti

    public static final int MAX_ERROR_COUNT = 5;
}
