package edu.ashleytemple.datadump;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private SensorManager sensorMan;
    private Sensor sensorAcc;
    private Sensor sensorGyro;

    //https://stackoverflow.com/questions/17807777/simpledateformatstring-template-locale-locale-with-for-example-locale-us-for
    private SimpleDateFormat date = new SimpleDateFormat("MM-dd-yyyy HH:mm", Locale.US);
    private SimpleDateFormat time = new SimpleDateFormat( "HH:mm:ss.SSS", Locale.US); //How many digits for millisec?

    private float[] accValues;
    private float[] gyroValues;
    private float[] orient;
    private float[] rotationMatrix;

    private BufferedWriter writer;
    private ArrayList<String> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        try {
            //Create directory
            File dir = new File(MainActivity.this.getApplicationContext().getFilesDir(), "Data");

            System.out.println("Made directory? " + dir.mkdir());

            File file = new File(dir, date.format((Calendar.getInstance()).getTime()) + ".csv");

            //Uncomment when file exists
            writer = new BufferedWriter(new FileWriter(file, true));
        } catch (Exception e) {
            e.printStackTrace();
        }

        data = new ArrayList<>();

        Switch mySwitch = findViewById(R.id.switch1);
        //https://stackoverflow.com/questions/11278507/android-widget-switch-on-off-event-listener
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    setUp();
                } else {
                    dumpData();
                    tearDown();
                }
            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        this.accValues = new float[3];
        this.gyroValues = new float[3];
        this.orient = new float[3];
        String currentTime = time.format(System.currentTimeMillis());


        //Check which sensor, set appropriate attribute
        //Need to add some math (subtract gravity, etc.)
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            this.sensorAcc = event.sensor;
            this.accValues = event.values;  //X,Y,Z values
        }
        //What's the difference between gyroscope and rotation vector? Which one is triggered?
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            this.sensorGyro = event.sensor;
            this.gyroValues = event.values;

            // If this doesn't work, try adding math (the purpose of which is unknown to me...)
            //https://stuff.mit.edu/afs/sipb/project/android/docs/guide/topics/sensors/sensors_motion.html
            SensorManager.getRotationMatrixFromVector(rotationMatrix, gyroValues);
            //So if this works, now I have a rotation matrix
            SensorManager.getOrientation(rotationMatrix, orient); //And now I have orientation values
            String o = "" + (orient == null);
            Log.d("MainActivity",o);
        }

        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            String line = currentTime + ",";
            line = line + accValues[0] + "," + accValues[1] + "," + accValues[2] + ",";
            line = line + orient[0] + "," + orient[1] + "," + orient[2] + "\n";
            data.add(line);
        }
    }

    /**
     * Writes the values stored in data to the file line by line
     */
    private void dumpData() {
        for (int i = 0; i < data.size(); i++) {
            try {
                this.writer.append(data.get(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Set up sensors, register listener, SensorManager
     */
    private void setUp() {

        sensorMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAcc = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGyro = sensorMan.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (sensorAcc != null) {
            sensorMan.registerListener(this, sensorAcc, SensorManager.SENSOR_DELAY_NORMAL);     //look into other delays?
        }
        if (sensorGyro != null) {
            sensorMan.registerListener(this, sensorGyro, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * Unregister listener, close writer
     */
    private void tearDown() {
        try {
            sensorMan.unregisterListener(this);
           // writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
