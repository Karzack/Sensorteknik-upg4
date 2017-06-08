package labbar.bjaern.sensorp4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import static labbar.bjaern.sensorp4.R.id.btnReset;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private boolean isFirstValue;
    private float last_x;
    private float last_y;
    private float last_z;
    private ImageView mCompass;
    private Sensor mAccelerometerSensor;
    private Sensor mMagnetometerSensor;
    private Sensor mOrientationSensor;
    private float[] mLastAccelerometer;
    private float[] mLastMagnetometer;
    private boolean mLastAccelerometerSet;
    private boolean mLastMagnetometerSet;
    private float shakeThreshold = 20;
    private long lastUpdateTime;
    private float[] mRotationMatrix;
    private float[] mOrientation;
    private boolean useOrientationAPI = false;
    private float mCurrentDegree = 0;
    private Sensor mStepCounter;
    private Button btnStop;
    private Button btnStart;
    private Button btnReset;
    private StepService service;
    private TextView tvStepsPerSecond;
    private TextView tvSteps;
    private int steps;
    private BroadcastReceiver reciever;
    private Intent intent;
    FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("Hello! " + getIntent().getExtras().get("name"));
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mCompass = (ImageView) findViewById(R.id.ivCompass);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnReset = (Button) findViewById(R.id.btnReset);
        tvSteps = (TextView) findViewById(R.id.tvSteps);
        tvStepsPerSecond = (TextView) findViewById(R.id.tvStepsPerSeconds);
        initClickListeners();
        reciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tvSteps.setText(intent.getExtras().getString("steps"));
            }
        };

    }

    private void initClickListeners() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent = new Intent(MainActivity.this, StepService.class);
                intent.putExtra("steps", steps);
                startService(intent);
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(intent);
            }
        });
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(intent);
                intent = new Intent(MainActivity.this, StepService.class);
            }
        });
    }

    protected void onResume() {
        super.onResume();
        this.isFirstValue = false;
        if (useOrientationAPI) {
            mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mMagnetometerSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            mSensorManager.registerListener(this, mOrientationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    protected void onPause() {
        super.onPause();
        if (useOrientationAPI) {
            mSensorManager.unregisterListener(this, mAccelerometerSensor);
            mSensorManager.unregisterListener(this, mMagnetometerSensor);
        } else {
            mSensorManager.unregisterListener(this, mOrientationSensor);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                if (isFirstValue) {
                    float deltaX = Math.abs(last_x - x);
                    float deltaY = Math.abs(last_y - y);
                    float deltaZ = Math.abs(last_z - z);
                    if ((deltaX > shakeThreshold && deltaY > shakeThreshold) || (deltaX > shakeThreshold && deltaZ > shakeThreshold) || (deltaY > shakeThreshold && deltaZ > shakeThreshold)) {
                        //Don't play sound, if it is already being played
                    }
                }
                last_x = x;
                last_y = y;
                last_z = z;
                isFirstValue = true;
                if (useOrientationAPI)
                    rotateUsingOrientationAPI(sensorEvent);
                break;
            case Sensor.TYPE_ORIENTATION:
                if (!useOrientationAPI) {
                    rotateUsingOrientationSensor(sensorEvent);
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                if (useOrientationAPI) {
                    rotateUsingOrientationAPI(sensorEvent);
                }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void rotateUsingOrientationSensor(SensorEvent event) {
        //only 4 times in 1 second
        if (System.currentTimeMillis() - lastUpdateTime > 0) {
            float angleInDegress = event.values[0];
            RotateAnimation mRotateAnimation = new RotateAnimation(
                    mCurrentDegree, -angleInDegress, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            //250 milliseconds
            mRotateAnimation.setDuration(250);
            mRotateAnimation.setFillAfter(true);
            mCompass.startAnimation(mRotateAnimation);
            mCurrentDegree = -angleInDegress;
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    public void rotateUsingOrientationAPI(SensorEvent event) {
        if (event.sensor == mAccelerometerSensor) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometerSensor) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        //only 4 times in 1 second
        if (mLastAccelerometerSet && mLastMagnetometerSet && System.currentTimeMillis() - lastUpdateTime > 0) {
            SensorManager.getRotationMatrix(mRotationMatrix, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;
            RotateAnimation mRotateAnimation = new RotateAnimation(mCurrentDegree, -azimuthInDegress, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            mRotateAnimation.setDuration(250);
            mRotateAnimation.setFillAfter(true);
            mCompass.startAnimation(mRotateAnimation);
            mCurrentDegree = -azimuthInDegress;
            lastUpdateTime = System.currentTimeMillis();
        }
    }
}



