package com.example.sps;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.sps.activity_recognizer.ActivityAlgorithm;
import com.example.sps.activity_recognizer.ActivityRecognizer;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_collection.DataCollectionActivity;
import com.example.sps.data_structure.PushOutList;
import com.example.sps.database.DatabaseService;
import com.example.sps.localization_method.ContinuousLocalization;
import com.example.sps.localization_method.KnnLocalizationMethod;
import com.example.sps.localization_method.LocalizationMethod;
import com.example.sps.localization_method.LocalizationAlgorithm;
import com.example.sps.localization_method.ParallelBayesianLocalizationMethod;
import com.example.sps.localization_method.Particle;
import com.example.sps.localization_method.ParticleFilterLocalization;
import com.example.sps.map.Cell;
import com.example.sps.map.WallPositions;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.example.sps.localization_method.ParticleFilterLocalization.NUM_PARTICLES;

//TODO: IMPLEMENT way of automatic finding statistics on measurements (auto measure save?)


//TODO: implement graphs of the different activities like "climbing the stairs", running
// walking with phone in pocket, walking with phone in the hand
//TODO: use another sensors to sense direction, like magnetometer/compass

//TODO (from old main):
//   - implement x ScansPerCell button to run all scans in one;
//   - implement just one broadcast catcher (like my example) BUT have it written to diff files;
//   - (if passing to the other file is needed, don't forget to carry the things that prevent crashing like pauses and resumes


//TODO: UI, duh


public class LocateMeActivity extends AppCompatActivity {

    public static final int NUM_ACC_READINGS = 20;


    private Canvas canvas;
    private int xOffSet = 700;
    private int yOffSet = 5;


    private Button initialBeliefButton;
    private Button locateMeButton;
    private Button collectDataButton;

    private TextView cellText;
    private TextView actText;
    private TextView miscText;

    private Spinner locSpin;
    private Spinner actSpin;

    private EditText currCellText;

    private ActivityRecognizer activityRecognizer;
    private LocalizationMethod localizationMethod;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private PushOutList<FloatTriplet> accelerometerData;

    private List<ScanResult> scanData;

    private AccelerometerListener accelerometerListener;

    private IntentFilter wifiIntentFilter;
    private BroadcastReceiver wifiBroadcastReceiver;

    private WifiManager wifiManager;

    private float[] cellProbabilities;

    private DatabaseService databaseService;

    private Sensor rotationSensor;
    private Sensor stepSensor;

    private float mAzimuth = 0;

    private int steps = 0;

    private boolean update = true;

    private CopyOnWriteArrayList<Particle> particles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_me);


        databaseService = new DatabaseService(this);
        initialBeliefButton = findViewById(R.id.btn_initial_belief);
        locateMeButton = findViewById(R.id.btn_locate_me);
        collectDataButton = findViewById(R.id.btn_collect_data);

        cellText = findViewById(R.id.cell_guess);
        actText = findViewById(R.id.act_guess);
        miscText = findViewById(R.id.misc_info);
        currCellText = findViewById(R.id.currCell);

        locSpin = findViewById(R.id.localization_algorithm_spin);
        actSpin = findViewById(R.id.activity_detection_spin);
        setInitialBelief();


        //Set Adapter for the Localization Spinner
        ArrayAdapter<LocalizationAlgorithm> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LocalizationAlgorithm.values());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        locSpin.setAdapter(adapter);
        //Set Listener for Localization Spinner changes
        locSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                localizationMethod = ((LocalizationAlgorithm) adapterView.getItemAtPosition(i)).getMethod();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                localizationMethod = localizationMethod; //Do nothing..
            }
        });

        //Set Adapter for the Activity Spinner
        ArrayAdapter<ActivityAlgorithm> adapterAct = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, ActivityAlgorithm.values());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        actSpin.setAdapter(adapterAct);
        //Set Listener for Activity Spinner changes
        actSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                activityRecognizer = ((ActivityAlgorithm) adapterView.getItemAtPosition(i)).getMethod();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                activityRecognizer = activityRecognizer; //do nothing
            }
        });


        activityRecognizer = ActivityAlgorithm.NORMAL.getMethod();
        localizationMethod = LocalizationAlgorithm.KNN_RSSI.getMethod();


        accelerometerData = new PushOutList<>(NUM_ACC_READINGS);
        accelerometerListener = new AccelerometerListener(accelerometerData);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiBroadcastReceiver = new simpleScanBroadcastReceiver();
        wifiIntentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);

        initialBeliefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialBelief();
            }
        });

        // Set the sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        locateMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                cellText.setText("Loading...");
                actText.setText("");
                if (scanData != null)
                    scanData.removeAll(scanData);

                if (!(localizationMethod instanceof ContinuousLocalization))
                    new Thread(new singleLocalizationRunnable()).start();
                else
                    new Thread(new continuousLocalizationRunnable()).start();


            }
        });

        collectDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent((Activity) view.getContext(), DataCollectionActivity.class);
                startActivity(intent);
            }
        });

        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(new RotationListener(), rotationSensor, 100000);

        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (update) {

                                drawMap();
                                if (localizationMethod instanceof ContinuousLocalization){
                                    drawArrow();
                                    Paint p = new Paint();
                                    p.setTextSize(50);
                                    canvas.drawText("" + steps, 100, 500, p);
                                    if (steps > 0)
                                        System.out.println("Steps taken: " + steps);
                                    if(particles != null) {
                                        drawParticles();
                                    }

                                }
                                update = false;
                            }




                            //steps = 0;
                        }
                    });
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).

                start();


    }

    private class singleLocalizationRunnable implements Runnable {
        public void run() {
            update = true;

            //Start wifi scan
            wifiManager.startScan();
            accelerometerData.removeAll(accelerometerData);
            sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

            while (scanData == null || scanData.size() == 0 || accelerometerData.size() < NUM_ACC_READINGS) { //spin while data not ready
                try {
                    System.out.println("SIZE: " + accelerometerData.size());
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //when finished, compute location and activity and post to user. unregister accelorometer listener
            sensorManager.unregisterListener(accelerometerListener);

            final SubjectActivity activity = activityRecognizer.recognizeActivity(accelerometerData);

            cellProbabilities = localizationMethod.computeLocation(scanData, cellProbabilities, databaseService);
            for (int i = 0; i < cellProbabilities.length; i++)
                System.out.println("prob[" + i + "] = " + cellProbabilities[i]);
            final int cell = getIndexOfLargest(cellProbabilities) + 1;

            if (!currCellText.getText().toString().equals("CurrentCell (for stats)")) {
                int txtCell = Integer.parseInt(currCellText.getText().toString());

                try {
                    FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sps/stats.txt", true);
                    fw.append(txtCell + "," + cell + "," + Math.round(cellProbabilities[cell - 1] * 100) + "," + localizationMethod.getClass().getName() + "," + localizationMethod.getMiscInfo() + "\n");
                    fw.flush();
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            final float confidence = cellProbabilities[cell - 1];
            System.out.println("confidence is" + confidence);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    // Stuff that updates the UI
                    setLocalizationText(activity, cell, confidence);

                    highlightLocation(cell);
                }
            });
        }
    }


    private class continuousLocalizationRunnable implements Runnable {

        @Override
        public void run() {

            update = false;
            sensorManager.registerListener(new StepListener(), stepSensor, 100000);

            // Spread particles
            particles = ((ContinuousLocalization) localizationMethod).spreadParticles(cellProbabilities);

            while (localizationMethod instanceof ContinuousLocalization) {
                updateParticles();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            sensorManager.unregisterListener(accelerometerListener);

        }
    }

    private void updateParticles() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        update = true;
        return;
    }

    private void drawParticles() {

        int width = this.canvas.getWidth();

        WallPositions walls = new WallPositions();

        float xcale = width / walls.getMaxWidth();


        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
        drawable.getPaint().setColor(Color.RED);


        int radius = 3;

        for (int i = 0; i < NUM_PARTICLES; i++) {
            drawable.setBounds(xOffSet - Math.round((particles.get(i).getY()) * xcale) - radius,
                    yOffSet + Math.round((particles.get(i).getX()) * xcale) - radius,
                    xOffSet - Math.round((particles.get(i).getY()) * xcale) + radius,
                    yOffSet + Math.round((particles.get(i).getX()) * xcale) + radius);
            drawable.draw(canvas);
        }
        return;
    }

    private class StepListener implements SensorEventListener {


        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                if (sensorEvent.values.length > 0)
                    steps++;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            return;
        }
    }

    private class RotationListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float[] orientation = new float[3];
            float[] rMat = new float[9];

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // calculate th rotation matrix
                SensorManager.getRotationMatrixFromVector(rMat, sensorEvent.values);
                // get the azimuth value (orientation[0]) in degree
                mAzimuth = (float) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;

                //System.out.println("azi: " + mAzimuth);

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            return;
        }
    }

    private void drawArrow() {

        double stopX = 200 * Math.cos(mAzimuth / 180 * Math.PI);
        double stopY = 200 * Math.sin(mAzimuth / 180 * Math.PI);
        Paint p = new Paint();
        p.setStrokeWidth(15);
        int x_offset = 200;
        int y_offset = 200;
        canvas.drawLine(x_offset, y_offset, x_offset + (int) Math.round(stopX), y_offset + (int) Math.round(stopY), p);
    }

    private void highlightLocation(int current_cell) {
        WallPositions walls = new WallPositions();

        float xcale = canvas.getWidth() / walls.getMaxWidth();

        ShapeDrawable rectangle = new ShapeDrawable(new RectShape());

        /*
        //Delete highlight in the last cell
        Cell c = walls.getCells().get(last_cell - 1);

        rectangle.getPaint().setColor(Color.BLACK);
        rectangle.getPaint().setStyle(Paint.Style.STROKE);
        rectangle.getPaint().setStrokeWidth(10);

        rectangle.setBounds(xOffSet - Math.round(c.getBottomWall() * xcale), yOffSet + Math.round(c.getLefttWall() * xcale),
                xOffSet - Math.round(c.getTopWall() * xcale), yOffSet + Math.round(c.getRightWall() * xcale));
        rectangle.draw(canvas);
        */

        //Highlight current cell
        Cell c = walls.getCells().get(current_cell - 1);

        rectangle.getPaint().setColor(Color.GREEN);
        rectangle.getPaint().setStrokeWidth(10);

        rectangle.setBounds(xOffSet - Math.round(c.getBottomWall() * xcale), yOffSet + Math.round(c.getLefttWall() * xcale),
                xOffSet - Math.round(c.getTopWall() * xcale), yOffSet + Math.round(c.getRightWall() * xcale));
        rectangle.draw(canvas);


    }

    public int getIndexOfLargest(float[] array) {
        if (array == null || array.length == 0)
            return -1;

        int largest = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[largest]) largest = i;
        }
        return largest;
    }


    private void setLocalizationText(SubjectActivity activity, int cell, float confidence) {
        this.actText.setText("You are " + activity.toString());
        this.cellText.setText("You are at cell " + cell + " with confidence " + Math.round((confidence * 100) * 100) / 100 + "%");
        if (this.localizationMethod instanceof KnnLocalizationMethod)
            miscText.setText("Number of Neighbours: " + ((KnnLocalizationMethod) localizationMethod).getNumNeighbours());
        if (this.localizationMethod instanceof ParallelBayesianLocalizationMethod)
            miscText.setText("Number of BSSIDs considered: " + localizationMethod.getMiscInfo());
    }

    @Override
    public void onPause() {
        super.onPause();
        this.unregisterReceiver(wifiBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);


    }

    protected void setInitialBelief() {
        int numCells = databaseService.getNumberOfCells();
        cellProbabilities = new float[numCells];
        for (int i = 0; i < numCells; i++)
            cellProbabilities[i] = 1.0f / numCells;
    }

    public class simpleScanBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            scanData = wifiManager.getScanResults();
        }

    }


    private void drawMap() {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        ImageView canvasView = (ImageView) findViewById(R.id.canvas);

        Bitmap blankBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);

        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);


        int width = this.canvas.getWidth();

        WallPositions walls = new WallPositions();

        float xcale = width / walls.getMaxWidth();


        ShapeDrawable rectangle = new ShapeDrawable(new RectShape());

        rectangle.getPaint().setColor(Color.BLACK);
        rectangle.getPaint().setStyle(Paint.Style.STROKE);
        rectangle.getPaint().setStrokeWidth(10);


        // draw the objects

        int rot = 2;

        /*normal
        if (rot == 0)
            for (Cell c : walls.getCells()) {
                rectangle.setBounds(Math.round(c.getLefttWall() * xcale) + xOffSet, Math.round(c.getTopWall() * xcale) + yOffSet,
                        Math.round(c.getRightWall() * xcale) + xOffSet, Math.round(c.getBottomWall() * xcale) + yOffSet);
                rectangle.draw(canvas);
            }


        if (rot == 1)
            for (Cell c : walls.getCells()) {
                rectangle.setBounds(Math.round(c.getTopWall() * xcale) + xOffSet, Math.round(c.getLefttWall() * xcale) + yOffSet,
                        Math.round(c.getBottomWall() * xcale) + xOffSet, Math.round(c.getRightWall() * xcale) + yOffSet);
                rectangle.draw(canvas);
            }
        */
        //PERFECT
        if (rot == 2)
            for (Cell c : walls.getCells()) {
                rectangle.setBounds(xOffSet - Math.round(c.getBottomWall() * xcale), yOffSet + Math.round(c.getLefttWall() * xcale),
                        xOffSet - Math.round(c.getTopWall() * xcale), yOffSet + Math.round(c.getRightWall() * xcale));
                rectangle.draw(canvas);
            }
    }
}
