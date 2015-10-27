package com.itnstudios.wagewatchers;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
    EditText mJobTitle, mHourlyRate;
    TextView mMoneyClock;
    Button mStartStop;
    double hourlyRate;
    Handler handler;
    Thread clockThread;
    boolean clockRun = false, mainRun = true;
    SharedPreferences prefs;
    SharedPreferences.Editor prefsEditor;
    long timeClockStarted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mJobTitle = (EditText)findViewById(R.id.et_job_title);
        mHourlyRate = (EditText)findViewById(R.id.et_hourly_rate);
        mMoneyClock = (TextView)findViewById(R.id.money_clock);
        mStartStop = (Button)findViewById(R.id.bt_start);
        clockThread = new Thread(new ClockRun());
        prefs = getSharedPreferences("com.itnstudios.mainPrefs", MODE_PRIVATE);
        prefsEditor = prefs.edit();


        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                mMoneyClock.setText(String.format("$%.2f",
                        ( System.currentTimeMillis() - timeClockStarted) * (hourlyRate/3600000)) );

            }
        };
        clockThread.start();
    }

    public void validateFields(View view){
        String jobTitle = mJobTitle.getText().toString();
        if(jobTitle.equalsIgnoreCase("")){
            Notify.viaToast(this, "Please enter a job title");
        }else {
            try{
                Double.parseDouble(mHourlyRate.getText().toString());
                startStopClock();
            }catch (Exception e){
                Notify.viaToast(this, "Please enter an hourly rate");

            }
        }

    }
    private void startStopClock() {
        if(clockRun){
            //stop clock
            clockRun = false;
            mStartStop.setText("Start");
        }else if (!clockRun){
            hourlyRate = Double.parseDouble(mHourlyRate.getText().toString());
            //start clock
            clockRun = true;
            mStartStop.setText("Stop");
            timeClockStarted = System.currentTimeMillis();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefsEditor.putString("jobTitle", mJobTitle.getText().toString());
        prefsEditor.putString("hourlyRate", mHourlyRate.getText().toString());
        prefsEditor.commit();
        mainRun = false;

    }

    @Override
    protected void onResume() {
        super.onResume();
        String jobTitle = prefs.getString("jobTitle", "");
        String hourlyRate = prefs.getString("hourlyRate", "");
        mJobTitle.setText(jobTitle);
        mHourlyRate.setText(hourlyRate);
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

//            double elapsedTime = System.currentTimeMillis() - timeClockStarted;
            do {
                while(clockRun){
//                    double amountPerMs = hourlyRate / 360000;
//                    Bundle clockData = new Bundle();
//                    clockData.putString("value", String.format("$%.2f", amount));

                    Message message = Message.obtain();
//                    message.setData(clockData);


                    handler.sendMessage(message);

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }while (mainRun);
        }
    }

}
