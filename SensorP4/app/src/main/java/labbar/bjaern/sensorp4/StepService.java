package labbar.bjaern.sensorp4;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import static android.content.ContentValues.TAG;

/**
 * Created by Bjaern on 2017-06-08.
 */

public class StepService extends Service implements SensorEventListener{

    private int steps = 0;
    private Sensor stepCounter = null;
    private SensorManager sensorManager =null;
    private TextView tvSteps;

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(this,stepCounter,SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onStartCommand: service active");
        steps = 15;
        Log.d(TAG, "onStartCommand: "+steps);
        sendMessageToActivity(steps);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType()==Sensor.TYPE_STEP_COUNTER){

            steps++;
            Log.d(TAG, "onSensorChanged: " + steps);
            sendMessageToActivity(steps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void sendMessageToActivity(int steps) {
        Intent intent = new Intent("steps");
        // You can also include some extra data.
        intent.putExtra("steps", steps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
