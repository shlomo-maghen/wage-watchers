package com.itnstudios.wagewatchers;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends Activity {
    boolean clockRun = false, mainRun = true;
    Button mStartStop, setDateStartedButton, setTimeStartedButton;
    Calendar tempTime;
    double hourlyRate;
    EditText mJobTitle, mHourlyRate;
    Handler handler;
    long timeClockStarted;
    SharedPreferences.Editor prefsEditor;
    SharedPreferences prefs;
    TextView mMoneyClock, mWorkLog;
    Thread clockThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mJobTitle = (EditText)findViewById(R.id.et_job_title);
        mHourlyRate = (EditText)findViewById(R.id.et_hourly_rate);
        mMoneyClock = (TextView)findViewById(R.id.money_clock);
        mWorkLog = (TextView) findViewById(R.id.tv_log);
        mStartStop = (Button)findViewById(R.id.bt_start);
        setDateStartedButton = (Button)findViewById(R.id.bt_set_date);
        setTimeStartedButton = (Button)findViewById(R.id.bt_set_time);
        clockThread = new Thread(new ClockRun());
        prefs = getSharedPreferences("com.itnstudios.mainPrefs", MODE_PRIVATE);
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                mMoneyClock.setText(String.format("$%.2f",
                        ( System.currentTimeMillis() - timeClockStarted) * (hourlyRate/3600000)) );

            }
        };
        clockThread.start();

        String jobTitle = prefs.getString("jobTitle", "");
        mJobTitle.setText(jobTitle);

        String hourlyRate = prefs.getString("hourlyRate", "");
        mHourlyRate.setText(hourlyRate);

        String workLog = prefs.getString("workLog", "");
        mWorkLog.setText(workLog);
        if(mJobTitle.getText().toString().equalsIgnoreCase(""))mJobTitle.requestFocus();

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefsEditor = prefs.edit();
        prefsEditor.putString("jobTitle", mJobTitle.getText().toString());
        prefsEditor.putString("hourlyRate", mHourlyRate.getText().toString());

        if(clockRun)prefsEditor.putLong("timeClockStarted", timeClockStarted);
        else prefsEditor.remove("timeClockStarted");
        prefsEditor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.getLong("timeClockStarted", 0) > 0 && !clockRun) {
            startStopClock(null);
        }
    }

    public void startStopClock(View v) {
        if(clockRun){
            //stop clock
            //get total time worked
            long timeWorked = System.currentTimeMillis() - timeClockStarted;
            clockRun = false;
            mStartStop.setText("Start");

            String realMoneyEarned = String.format("$%.2f", hourlyRate/3600000*timeWorked);
            mMoneyClock.setText(realMoneyEarned);

            String workRecord = getWorkRecord(timeWorked, realMoneyEarned);
            mWorkLog.setText(workRecord);

            //reset the clock
            prefsEditor = prefs.edit();
            prefsEditor.remove("timeClockStarted");
            prefsEditor.apply();
            mMoneyClock.setText("$0.00");
            //disable the date/time buttons
            setDateStartedButton.setEnabled(false);
            setTimeStartedButton.setEnabled(false);

        }else if (!clockRun){
            tempTime = Calendar.getInstance();
            if (fieldsOk()){
                hourlyRate = Double.parseDouble(mHourlyRate.getText().toString());
                //start clock
                clockRun = true;
                timeClockStarted = prefs.getLong("timeClockStarted", System.currentTimeMillis());
                mStartStop.setText("Stop");
                //hide keyboard
                InputMethodManager manager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                try{
                    manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }catch(NullPointerException e){
                    e.printStackTrace();
                }
            }
            //enable the date/time buttons
            setDateStartedButton.setEnabled(true);
            setTimeStartedButton.setEnabled(true);

        }
    }

    public void setTimeStarted(View v){
        TimePickerDialog timePicker = new TimePickerDialog(this, new TimeSetListener(),
                tempTime.get(Calendar.HOUR),
                tempTime.get(Calendar.MINUTE),
                false);
        timePicker.show();
    }
    public void setDateStarted(View v){
        DatePickerDialog datePicker = new DatePickerDialog(this, new DateSetListener(),
                tempTime.get(Calendar.YEAR),
                tempTime.get(Calendar.MONTH),
                tempTime.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }
    private String getWorkRecord(long timeWorked, String realMoneyEarned) {
        StringBuilder builder = new StringBuilder();
        //append date MM/DD/YY
        Date today = new Date(System.currentTimeMillis());
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        String date = dateFormat.format(today);
        builder.append(date);
        builder.append("\n");
        //append job title
        builder.append(mJobTitle.getText() +"//");
        //append time worked
        long seconds = timeWorked / 1000 % 60;
        long minutes = timeWorked / 60000 % 60;
        long hours = timeWorked / 3600000 % 24;
        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        builder.append(time+ "//");
        //append amount earned
        builder.append(realMoneyEarned + "\n");

        String prefsLog = prefs.getString("workLog", "");
        builder.append(prefsLog);
        prefsEditor = prefs.edit();
        prefsEditor.putString("workLog", builder.toString());
        prefsEditor.apply();

        return builder.toString();
    }

    private boolean fieldsOk(){
        String jobTitle = mJobTitle.getText().toString();
        if(jobTitle.equalsIgnoreCase("")){
            Notify.viaToast(this, "Please enter a job title");
        }else {
            try{
                Double.parseDouble(mHourlyRate.getText().toString());
                return true;
            }catch (Exception e){
                Notify.viaToast(this, "Please enter an hourly rate");
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    class ClockRun implements Runnable {

        @Override
        public void run() {
            do {
                while(clockRun){
                    Message message = Message.obtain();
                    handler.sendMessage(message);

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }while (mainRun);
        }
    }
    class TimeSetListener implements TimePickerDialog.OnTimeSetListener{
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            tempTime.set(Calendar.HOUR, hourOfDay);
            tempTime.set(Calendar.MINUTE, minute);
            timeClockStarted = tempTime.getTimeInMillis();
        }
    }


    class DateSetListener implements DatePickerDialog.OnDateSetListener{
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            tempTime.set(Calendar.YEAR, year);
            tempTime.set(Calendar.MONTH, monthOfYear);
            tempTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                timeClockStarted = tempTime.getTimeInMillis();
        }

    }
}
