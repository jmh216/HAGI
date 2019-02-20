package com.iot.hagi.mqtthope;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import helpers.MqttHelper;
import helpers.ChartHelper;
import org.json.JSONObject;
import android.os.Vibrator;

public class MainActivity extends AppCompatActivity {

    MqttHelper mqttHelper;
    ChartHelper mChart;
    ChartHelper mChart2;
    ChartHelper mChart3;
    LineChart chart;
    LineChart chart2;
    LineChart chart3;

    TextView objtemp;
    TextView shake;
    TextView distance;
    TextView Alarmmessage;

    Button button_connection;
    boolean button_clicked = false;

    int startFrom = 0;
    int endAt = 1500;

    int shake_counter = 0;

    MediaPlayer mp;
    Vibrator v;

//Stop the music player
    Runnable stopPlayerTask = new Runnable(){
        @Override
        public void run() {
            mp.stop();
            mp.reset();
        }};

//enable or disable receiving data from sensors transmitted through mqtt
    public void ButtonClick(View view){
        if(button_clicked){
            Log.d("myTag", "button clicked. Quit MQTT");
            button_connection.setText("Connect");

            button_clicked = false;
            mqttHelper.disconnect();

            shake_counter = 0;
            Alarmmessage.setText("   ");
        }else{
            startMqtt();
            Log.d("myTag", "connection_status "+mqttHelper.queryConnectionStatus());
            button_clicked = true;
            button_connection.setText("Disconnect");

            shake_counter = 0;
            Alarmmessage.setText("   ");
        }
    }
//alarm used with accelerometer (triggered when hair drying progress is finished)
    public void Alarmfinish(){
        mp = MediaPlayer.create(this, R.raw.cde);

        mp.seekTo(startFrom);
        mp.start();

        Handler handler = new Handler();
        handler.postDelayed(stopPlayerTask, endAt);
    }
//alarm used with distance sensor (triggered when distance too close or too far)
    public void Alarm(){
        mp = MediaPlayer.create(this, R.raw.abc);

        mp.seekTo(startFrom);
        mp.start();

        Handler handler = new Handler();
        handler.postDelayed(stopPlayerTask, endAt);
    }
//alarm used with temperature sensor (triggered when hair temperature too high)
    public void AlarmTemp(){
        mp = MediaPlayer.create(this, R.raw.efg);

        mp.seekTo(startFrom);
        mp.start();

        Handler handler = new Handler();
        handler.postDelayed(stopPlayerTask, endAt);
    }
//alarm used with temperature sensor (triggered when surrounding temperature too high)
    public void AlarmTempsurr(){
        mp = MediaPlayer.create(this, R.raw.ghi);

        mp.seekTo(startFrom);
        mp.start();

        Handler handler = new Handler();
        handler.postDelayed(stopPlayerTask, endAt);
    }

// triggered along with alarms above
    public void VibrateCycle(){

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(500);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//link the variables in xml file into java file
        distance = (TextView) findViewById(R.id.distance);
        Alarmmessage = (TextView) findViewById(R.id.Alarmmessage);
        objtemp = (TextView) findViewById(R.id.objtemp);
        shake = (TextView) findViewById(R.id.shake);

        button_connection = (Button) findViewById(R.id.button_connection);

        chart = (LineChart) findViewById(R.id.chart);
        chart2 = (LineChart) findViewById(R.id.chart2);
        chart3 = (LineChart) findViewById(R.id.chart3);
        mChart = new ChartHelper(chart);
        mChart2 = new ChartHelper(chart2);
        mChart3 = new ChartHelper(chart3);
    }
/*
Make connection to MQTT server and receive data;
Decode JSON format data into "string" type;
Upload the data into android device simultaneously;
Send alarm if threshold is met.
 */
    private void startMqtt() {
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String jsonString = mqttMessage.toString();

                JSONObject objson = new JSONObject(jsonString);
/*
we receive 2 types of temperature for surrounding and object(hair);
 2 types of alert
 */
                JSONObject obtemp = objson.getJSONObject("temperature");
                String object = obtemp.getString("object");
                String die = obtemp.getString("die");
                String alert = obtemp.getString("alert");
                String die_alert = obtemp.getString("die_alert");

                mChart2.addEntry( Float.valueOf(object));
                objtemp.setText("Object temperature: "+ object);

                if(Integer.valueOf(alert) == 1){
                    VibrateCycle();
                    AlarmTemp();
                    Alarmmessage.setText("Alarm: Hair temperature too high!");
                }
                if(Integer.valueOf(die_alert) == 1){
                    VibrateCycle();
                    AlarmTempsurr();
                    Alarmmessage.setText("Alarm: Surrounding temperature too high!");
                }
/*
decode distance
and set maximum distance to 130 cm due to the limit of the distance sensor
 */
                String dist = objson.getString("distance");
                float fdist = Math.min(Float.valueOf(dist), 130.0f);

                if( fdist == 130f)
                {
                    distance.setText("Distance: out of range ");

                }
                else if( fdist == 0f)
                {
                    distance.setText("Distance: too close.");
                    VibrateCycle();
                    Alarm();
                    Alarmmessage.setText("Alarm: Too close!");
                }
                else
                {
                    distance.setText("Distance: " + fdist);

                }
                mChart.addEntry(fdist);

/*
shake measures the hair drying progress
for convenience on demo, we set finish point when shake counter equal to 30;
 */
                String shake_str = objson.getString("shake");
                int shake_int = Integer.valueOf(shake_str);

                if(shake_int == 1){
                    shake_counter = shake_counter + shake_int;
                }

                if(shake_counter > 30){
                    VibrateCycle();
                    Alarmfinish();
                    Alarmmessage.setText("Finished!");
                }

                mChart3.addEntry( Math.min(Float.valueOf(shake_counter),30.0f));
                shake.setText("Progress: "+ shake_counter);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }
}





