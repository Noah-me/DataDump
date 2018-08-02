package edu.ashleytemple.datadump;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private SensorManager sensorMan;
    private Sensor sensorAcc;

    //https://stackoverflow.com/questions/17807777/simpledateformatstring-template-locale-locale-with-for-example-locale-us-for
    private SimpleDateFormat date = new SimpleDateFormat("MM-dd-yyyy_HH.mm", Locale.US);
    private SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private float[] accValues;                          //X, Y, Z
    private float[] gravity = new float[3];             //to estimate force of gravity

    private ArrayList<String> rawData;                  //Time,X,Y,Z
    private ArrayList<String> featureDataGeorgia;       //features from Georgia Tech
   // private ArrayList<String> featureDataCalifornia;    //features from UCLA
    private File rawDataFile;                           //file with raw data from accelerometer
    private File featureFileGeorgia;                    //file with Georgia features
 //   private File featureFileCalifornia;                 //file with UCLA features


    private DescriptiveStatistics xFeatures;
    private DescriptiveStatistics yFeatures;
    private DescriptiveStatistics zFeatures;

    /**
     * Handles the button on/off to start the process and tear down when finished
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();


        Switch mySwitch = findViewById(R.id.switch1);
        //https://stackoverflow.com/questions/11278507/android-widget-switch-on-off-event-listener
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //If set to on, start the process, else dump data and destroy
                if (isChecked) {
                    setUp();
                } else {
                    dumpData(rawDataFile, rawData);
                  //  dumpData(featureFileCalifornia, featureDataCalifornia);
                    dumpData(featureFileGeorgia, featureDataGeorgia);
                    tearDown();
                }
            }
        });

    }

    /**
     * When accelerometer has an event, factor out gravity, add data to ArrayList in CSV format
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (this.accValues == null) {
            this.accValues = new float[3];
        }

        String currentTime = time.format(System.currentTimeMillis());


        //Check which sensor, set appropriate attribute
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            this.sensorAcc = event.sensor;
            this.accValues = event.values;  //X,Y,Z values

            for (int i = 0; i < this.accValues.length; i++) {
                this.gravity[i] = (float) (0.9 * this.gravity[i] + 0.1 * this.accValues[i]);
                this.accValues[i] = ((this.accValues[i] - this.gravity[i]) / SensorManager.GRAVITY_EARTH);
                //Converting the acc value in m/s^2 to g
            }

            String line = currentTime + ",";
            line = line + accValues[0] + "," + accValues[1] + "," + accValues[2] + "\n";
            rawData.add(line);

            calculateFeatures(currentTime, accValues);

        }

    }

    /**
     * Writes the values stored in data to the file line by line
     */
    private void dumpData(File file, ArrayList<String> data) {
        try {
            FileWriter writer = new FileWriter(file);
            int size = data.size();
            for (int i = 0; i < size; i++) {
                writer.write(data.get(i));
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add x, y, z to window and calculate features
     * @param timestamp - time associated with event
     * @param values - x, y, z
     */
    private void calculateFeatures(String timestamp, float[] values) {
        xFeatures.addValue(values[0]);
        yFeatures.addValue(values[1]);
        zFeatures.addValue(values[2]);

        calculateGeorgia(timestamp);
        //calculateCalifornia(timestamp);

    }

    /**
     * Calculate mean, variance, skewness, kurtosis, RMS for each axis and add to ArrayList in CSV
     * format.
     * @param timestamp - time associated with event
     */
    private void calculateGeorgia(String timestamp) {
        String line = timestamp + "," + xFeatures.getMean() + "," + xFeatures.getVariance() + ","
                + xFeatures.getSkewness() + "," + xFeatures.getKurtosis() + ","
                + calculateRMS(xFeatures) + "," + yFeatures.getMean() + ","
                + yFeatures.getVariance() + "," + yFeatures.getSkewness() + ","
                + yFeatures.getKurtosis() + "," + calculateRMS(yFeatures) + ","
                + zFeatures.getMean() + "," + zFeatures.getVariance() + ","
                + zFeatures.getSkewness() + "," + zFeatures.getKurtosis() + ","
                + calculateRMS(zFeatures) + "\n";

        featureDataGeorgia.add(line);
    }

//    /**
//     * Features from UCLA paper
//     * @param timestamp
//     */
//    private void calculateCalifornia(String timestamp) {
//        double[] xSorted = xFeatures.getSortedValues();
//        double[] ySorted = yFeatures.getSortedValues();
//        double[] zSorted = zFeatures.getSortedValues();
//
//        double xMedian = xSorted[(int) xFeatures.getN() / 2];
//        double yMedian = ySorted[(int) yFeatures.getN() / 2];
//        double zMedian = zSorted[(int) zFeatures.getN() / 2];
//
//        String line = timestamp + "," + Math.abs(accValues[0]) + "," + xMedian + ","
//                + xFeatures.getMean() + "," + xFeatures.getMax() + "," + xFeatures.getMin() + ","
//                + (xFeatures.getMax() - xFeatures.getMin()) + "," + xFeatures.getVariance() + ","
//                + xFeatures.getStandardDeviation() + "," + calculateRMS(xFeatures) + ","
//                + xFeatures.getSkewness() + "," + Math.abs(accValues[1]) + "," + yMedian + "," + yFeatures.getMean() + ","
//                + yFeatures.getMax() + "," + yFeatures.getMin() + "," + (yFeatures.getMax()
//                - yFeatures.getMin()) + "," + yFeatures.getVariance() + ","
//                + yFeatures.getStandardDeviation() + "," + calculateRMS(yFeatures) + "," + yFeatures.getSkewness()
//                + "," + Math.abs(accValues[2]) + "," + zMedian + ","
//                + zFeatures.getMean() + "," + zFeatures.getMax() + "," + zFeatures.getMin() + ","
//                + (zFeatures.getMax() - zFeatures.getMin()) + "," + zFeatures.getVariance() + ","
//                + zFeatures.getStandardDeviation() + "," + calculateRMS(zFeatures) + "," + zFeatures.getSkewness() +"\n";
//
//        featureDataCalifornia.add(line);
//    }

    /**
     * Calculate RMS (sum of squares divided by n)
     * @param signal - window of axis
     * @return RMS value as a string
     */
    private String calculateRMS(DescriptiveStatistics signal) {
        return "" + (signal.getSumsq() / signal.getN());
    }


    /**
     *
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Set up sensors, register listener, SensorManager, file(s)
     */
    private void setUp() {

        try {
            //Create directory
            File dir = new File(MainActivity.this.getApplicationContext().getExternalFilesDir(null), "SensorData");

            if (!dir.exists()) {
                dir.mkdir();
            }

            //Create other files
            rawDataFile = new File(dir, "Moto_" + date.format((Calendar.getInstance()).getTime()) + ".csv");
            featureFileGeorgia = new File(dir, "Moto_GA_" + date.format((Calendar.getInstance()).getTime()) + ".csv");
            //featureFileCalifornia = new File(dir, "Moto_CA_" + date.format((Calendar.getInstance()).getTime()) + ".csv");
            System.out.println("file path of csv: " + rawDataFile.getPath());
            if (!rawDataFile.exists()) {
                rawDataFile.createNewFile();
            }
            if (!featureFileGeorgia.exists()) {
                featureFileGeorgia.createNewFile();
            }
//            if (!featureFileCalifornia.exists()) {
//                featureFileCalifornia.createNewFile();
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        rawData = new ArrayList<>();
        featureDataGeorgia = new ArrayList<>();
        //featureDataCalifornia = new ArrayList<>();
        rawData.add("Time,X,Y,Z\n");
        featureDataGeorgia.add("Time,Mean-X,Variance-X,Skewness-X,Kurtosis-X,RMS-X,Mean-Y,Variance-Y," +
                "Skewness-Y,Kurtosis-Y,RMS-Y,Mean-Z,Variance-Z,Skewness-Z,Kurtosis-Z,RMS-Z\n");
        /*featureDataCalifornia.add("Time,Amplitude-X,Median-X,Mean-X,Max-X,Min-X,Peak-to-Peak-X," +
                "Variance-X,Std Dev-X,RMS-X,Skewness-X,Amplitude-Y,Median-Y,Mean-Y,Max-Y,Min-Y," +
                "Peak-to-Peak-Y,Variance-Y,Std Dev-Y,RMS-Y,Skewness-Y,Amplitude-Z,Median-Z,Mean-Z," +
                "Max-Z,Min-Z,Peak-to-Peak-Z,Variance-Z,Std Dev-Z,RMS-Z,Skewness-Z\n");
*/
        xFeatures = new DescriptiveStatistics(25); //Window size about 1 second
        yFeatures = new DescriptiveStatistics(25);
        zFeatures = new DescriptiveStatistics(25);

        sensorMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAcc = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (sensorAcc != null) {
            sensorMan.registerListener(this, sensorAcc, SensorManager.SENSOR_DELAY_UI);     //look into other delays?
        }

    }

    /**
     * Unregister listener
     */
    private void tearDown() {
        try {
            sensorMan.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}