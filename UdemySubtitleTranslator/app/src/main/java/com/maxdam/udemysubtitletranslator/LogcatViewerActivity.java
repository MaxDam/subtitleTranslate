package com.maxdam.udemysubtitletranslator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LogcatViewerActivity extends ListActivity {

    public static String LOG_TAG = "TRANSLATE_LOG";

    private LogStringAdaptor adaptor = null;
	private ArrayList<String> logarray = null;
	private LogReaderTask logReaderTask = null;

    public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log);

		logarray = new ArrayList<String>();
		adaptor = new LogStringAdaptor(this, R.id.txtLogString, logarray);

		setListAdapter(adaptor);

		//logReaderTask = new LogReaderTask();
		//logReaderTask.execute();

        //receiver di log
        IntentFilter filterPredict = new IntentFilter();
        filterPredict.addAction(ServiceTranslate.LOG_TRANSLATE);
        registerReceiver(receiverLog, filterPredict);

		//richiama il servizio di translate
		Intent translateServiceIntent = new Intent(LogcatViewerActivity.this, ServiceTranslate.class);
		startService(translateServiceIntent);
	}

    //evento di log
    private BroadcastReceiver receiverLog = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adaptor.add(intent.getStringExtra("message"));
        }
    };

    @Override
	protected void onDestroy() {
		
		logReaderTask.stopTask();

		super.onDestroy();
	}

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
    @Override
    public void onBackPressed() {

        logReaderTask.stopTask();

        super.onBackPressed();
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		super.onListItemClick(l, v, position, id);

		final AlertDialog.Builder builder = new AlertDialog.Builder(LogcatViewerActivity.this);
		String text = ((String) ((TextView) v).getText());

		builder.setMessage(text);

		builder.show();
	}

	private int getLogColor(String type) {
		
		int color = Color.BLUE;

		if (type.equals("D")) {
			color = Color.rgb(0, 0, 200);
		} else if (type.equals("W")) {
			color = Color.rgb(128, 0, 0);
		} else if (type.equals("E")) {
			color = Color.rgb(255, 0, 0);
			;
		} else if (type.equals("I")) {
			color = Color.rgb(0, 128, 0);
			;
		}

		return color;
	}

	private class LogStringAdaptor extends ArrayAdapter<String> {
		
		private List<String> objects = null;

		public LogStringAdaptor(Context context, int textviewid, List<String> objects) {
			super(context, textviewid, objects);

			this.objects = objects;
		}

		@Override
		public int getCount() {
			return ((null != objects) ? objects.size() : 0);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public String getItem(int position) {
			return ((null != objects) ? objects.get(position) : null);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			
			View view = convertView;

			if (null == view) {
				LayoutInflater vi = (LayoutInflater) LogcatViewerActivity.this
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.logitem, null);
			}

			String data = objects.get(position);

			if (null != data) {// && data.length() > 31 && data.contains(LogcatViewerActivity.LOG_TAG)) {

                TextView textview = (TextView) view.findViewById(R.id.txtLogString);
				//String type = data.substring(31, 32);
				//String line = data.substring(32);
                String type = data.substring(0, 1);
                String line = data.substring(1);

                int start = line.indexOf(LogcatViewerActivity.LOG_TAG+":");
                if(start > 0) {
                    start += LogcatViewerActivity.LOG_TAG.length() + 2;
                    line = line.substring(start);
                }

				textview.setText(line);
				textview.setTextColor(getLogColor(type));
			}

			return view;
		}
	}

	private class LogReaderTask extends AsyncTask<Void, String, Void> {
		
		//private final String[] LOGCAT_CMD = new String[] { "logcat", "--regex=TRANSLATE_LOG" };
		private final String LOGCAT_CMD = "logcat";

		private final int BUFFER_SIZE = 1024;

		private boolean isRunning = true;
		private Process logprocess = null;
		private BufferedReader reader = null;
		private String[] line = null;

		@Override
		protected Void doInBackground(Void... params) {
			
			try {
				logprocess = Runtime.getRuntime().exec(LOGCAT_CMD);
			} catch (IOException e) {
				//e.printStackTrace();

				isRunning = false;
			}

			try {
				reader = new BufferedReader(new InputStreamReader(logprocess.getInputStream())); //, BUFFER_SIZE
			} catch (IllegalArgumentException e) {
				//e.printStackTrace();

				isRunning = false;
			}

			line = new String[1];

			try {
				while (isRunning) {
					line[0] = reader.readLine();

                    //if(!line[0].contains(LogcatViewerActivity.LOG_TAG)) continue;
                    //if(line[0].length() < 31) continue;

                    publishProgress(line);
				}
			} catch (Throwable e) {
				//e.printStackTrace();

				isRunning = false;
			}

			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);

			adaptor.add(values[0]);
		}

		public void stopTask() {
			isRunning = false;
			logprocess.destroy();
		}
	}
}