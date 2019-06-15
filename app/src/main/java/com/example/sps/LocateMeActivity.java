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
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
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
import com.example.sps.database.DatabaseService;
import com.example.sps.localization_method.CountParticleWeightsThread;
import com.example.sps.localization_method.ContinuousLocalization;
import com.example.sps.localization_method.LocalizationMethod;
import com.example.sps.localization_method.LocalizationAlgorithm;
import com.example.sps.localization_method.Particle;
import com.example.sps.map.Cell;
import com.example.sps.map.WallPositions;

import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static final int NUM_ACC_READINGS = 200;
    public static final int NUM_CELLS = 16;

    public static final int DRAW_FRAMES_PER_SECOND = 10;
    public static final int SKIP_TICKS_DRAW = Math.round(1000.0f / DRAW_FRAMES_PER_SECOND);

    public static final int UPDATE_FRAMES_PER_SECOND = 40;
    public static final int SKIP_TICKS_UPDATE = Math.round(1000.0f / UPDATE_FRAMES_PER_SECOND);

    private Canvas canvas;
    private int xOffSet = 700;
    private int yOffSet = 5;
    int particleRadius = 4;

    private Button initialBeliefButton;
    private Button locateMeButton;
    private Button collectDataButton;
    private Button takeStepButton;

    private TextView cellText;
    private TextView actMiscText;

    private Spinner locSpin;
    private Spinner actSpin;

    private EditText currCellText;

    private ActivityRecognizer activityRecognizer;
    private LocalizationMethod localizationMethod;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private LinkedBlockingQueue<FloatTriplet> accelerometerData;

    private List<ScanResult> scanData;

    private AccelerometerListener accelerometerListener;

    private IntentFilter wifiIntentFilter;
    private BroadcastReceiver wifiBroadcastReceiver;

    private WifiManager wifiManager;

    private float[] cellProbabilities;

    private DatabaseService databaseService;

    private Sensor rotationSensor;

    private float mAzimuth = 0;

    private float distanceCumulative = 0;


    private boolean update = true;

    private CopyOnWriteArrayList<Particle> particles;

    private WallPositions walls = new WallPositions();

    private Runnable activeLocalizationRunnable;
    private CountParticleWeightsThread activeCountParticlesThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_me);


        databaseService = new DatabaseService(this);
        initialBeliefButton = findViewById(R.id.btn_initial_belief);
        locateMeButton = findViewById(R.id.btn_locate_me);
        collectDataButton = findViewById(R.id.btn_collect_data);
        takeStepButton = findViewById(R.id.btn_step);
        takeStepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                distanceCumulative = 0;

            }
        });

        cellText = findViewById(R.id.cell_guess);
        actMiscText = findViewById(R.id.act_guess);
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


        activityRecognizer = ActivityAlgorithm.NORMAL_STD.getMethod();
        localizationMethod = LocalizationAlgorithm.KNN_RSSI.getMethod();


        accelerometerData = new LinkedBlockingQueue<>();
        accelerometerListener = new AccelerometerListener(accelerometerData, null);

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
                actMiscText.setText("");
                if (scanData != null)
                    scanData.removeAll(scanData);

                if (!(localizationMethod instanceof ContinuousLocalization))
                    new Thread(new singleLocalizationRunnable()).start();
                else {
                    if(activeLocalizationRunnable != null && activeLocalizationRunnable instanceof continuousLocalizationRunnable) {
                        ((continuousLocalizationRunnable) activeLocalizationRunnable).setRunning(false);
                        activeCountParticlesThread.setRunning(false);
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    activeLocalizationRunnable = new continuousLocalizationRunnable();
                    new Thread(activeLocalizationRunnable).start();
                }

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


        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        new Thread(new Runnable() {
            @Override
            public void run() {
                long nextTick = System.currentTimeMillis();
                long sleepTime = 0;
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
                                    canvas.drawText("Distance: " + Math.round(distanceCumulative*100)/100.0f, 20, 600, p);
                                    if(particles != null) {
                                        drawParticles();
                                    }

                                }
                                update = false;
                            }


                        }
                    });
                    nextTick += SKIP_TICKS_DRAW;
                    sleepTime = nextTick -System.currentTimeMillis();
                    if(sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }).

                start();


    }

    public class singleLocalizationRunnable implements Runnable {
        public void run() {
            update = true;

            //Start wifi scan
            wifiManager.startScan();

            accelerometerData.removeAll(accelerometerData);

            sensorManager.registerListener(accelerometerListener, accelerometer, 20000);

            while (scanData == null || scanData.size() == 0 || accelerometerData.size() < NUM_ACC_READINGS) { //spin while data not ready
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //when finished, compute location and activity and post to user. unregister accelorometer listener
            sensorManager.unregisterListener(accelerometerListener);

            final SubjectActivity activity = activityRecognizer.recognizeActivity(accelerometerData, databaseService);
;
            cellProbabilities = localizationMethod.computeLocation(scanData, cellProbabilities, databaseService);

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


    public class continuousLocalizationRunnable implements Runnable {

        private boolean running = true;

        private int stepsSinceLastUpdate = 0;

        private AtomicInteger accReadingsSinceLastUpdate = new AtomicInteger(0);

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {

            update = false;
            sensorManager.registerListener(new AccelerometerListener(accelerometerData, accReadingsSinceLastUpdate), accelerometer, 20000);
            // Spread particles
            particles = ((ContinuousLocalization) localizationMethod).spreadParticles(cellProbabilities);

            activeCountParticlesThread = new CountParticleWeightsThread(particles, walls, getLocateMeActivity());
            activeCountParticlesThread.start();

            long nextTick = System.currentTimeMillis();
            long sleepTime = 0;

            SubjectActivity currentActivityState = SubjectActivity.LOADING;

            int steps;
            while (localizationMethod instanceof ContinuousLocalization && running) {
                steps = 0;

                if(currentActivityState == SubjectActivity.LOADING)
                    accReadingsSinceLastUpdate.set(0);

                if (accelerometerData.size() == NUM_ACC_READINGS) {
                    SubjectActivity newActivity = activityRecognizer.recognizeActivity(accelerometerData, databaseService);
                    currentActivityState = newActivity;
                    steps = activityRecognizer.getSteps(accelerometerData, databaseService, currentActivityState, accReadingsSinceLastUpdate);
                }
                updateParticles(steps);


                final SubjectActivity currentActivityStatefinal = currentActivityState;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        actMiscText.setText("Activity: " + currentActivityStatefinal.name() + ". "+ localizationMethod.getMiscInfo());
                    }
                });


                nextTick += SKIP_TICKS_UPDATE;
                sleepTime = nextTick - System.currentTimeMillis();
                if(sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

            sensorManager.unregisterListener(accelerometerListener);

        }
    }

    private void updateParticles(int steps) {

        float distance = steps * 0.76f;

        ((ContinuousLocalization) localizationMethod).updateParticles(mAzimuth, distance, particles);


        ((ContinuousLocalization) localizationMethod).collideAndResample(particles, walls);

        update = true;
        return;
    }


    private void drawParticles() {

        int width = this.canvas.getWidth();

        float xcale = width / walls.getMaxWidth();


        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
        drawable.getPaint().setColor(Color.RED);

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()){
            Particle p = it.next();
            drawable.setBounds(xOffSet - Math.round((p.getY()) * xcale) - particleRadius,
                    yOffSet + Math.round((p.getX()) * xcale) - particleRadius,
                    xOffSet - Math.round((p.getY()) * xcale) + particleRadius,
                    yOffSet + Math.round((p.getX()) * xcale) + particleRadius);
            drawable.draw(canvas);
        }
        return;
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
                mAzimuth = (float) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 180) % 360;

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            return;
        }
    }

    private void drawArrow() {

        double stopX = 200 * Math.cos((mAzimuth + 90) / 180 * Math.PI); // + 90 to compensate for rotation
        double stopY = 200 * Math.sin((mAzimuth + 90) / 180 * Math.PI);
        Paint p = new Paint();
        p.setStrokeWidth(15);
        int x_offset = 200;
        int y_offset = 300;
        canvas.drawLine(x_offset, y_offset, x_offset + (int) Math.round(stopX), y_offset + (int) Math.round(stopY), p);
    }

    private void highlightLocation(int current_cell) {

        float xcale = canvas.getWidth() / walls.getMaxWidth();

        ShapeDrawable rectangle = new ShapeDrawable(new RectShape());

        /*
        //Delete highlight in the last cell
        Cell c = walls.getCells().get(last_cell - 1);

        rectangle.getPaint().setColor(Color.BLACK);
        rectangle.getPaint().setStyle(Paint.Style.STROKE);
        rectangle.getPaint().setStrokeWidth(10);

        rectangle.setBounds(xOffSet - Math.round(c.getBottomWall() * xcale), yOffSet + Math.round(c.getLeftWall() * xcale),
                xOffSet - Math.round(c.getTopWall() * xcale), yOffSet + Math.round(c.getRightWall() * xcale));
        rectangle.draw(canvas);
        */

        //Highlight current cell
        Cell c = walls.getCells().get(current_cell - 1);

        rectangle.getPaint().setColor(Color.GREEN);
        rectangle.getPaint().setStrokeWidth(10);

        rectangle.setBounds(xOffSet - Math.round(c.getBottomWall() * xcale), yOffSet + Math.round(c.getLeftWall() * xcale),
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
        this.actMiscText.setText("Activity: " + activity.name() + ". " + localizationMethod.getMiscInfo());
        this.cellText.setText("You are at cell " + cell + " with confidence " + Math.round((confidence * 100) * 100) / 100 + "%");
    }


    public void setLocationTextForParticleFilter(int cell) {
        this.cellText.setText("You are most likely at cell " + cell);
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


        float xcale = width / walls.getMaxWidth();


        ShapeDrawable rectangle = new ShapeDrawable(new RectShape());

        rectangle.getPaint().setColor(Color.BLACK);
        rectangle.getPaint().setStyle(Paint.Style.STROKE);
        rectangle.getPaint().setStrokeWidth(10);


        // draw the objects

        int rot = 2;

        /*normalp.setWeight(((float) p.getTimeAlive()) / totalTimeAlive);
        if (rot == 0)
            for (Cell c : walls.getCells()) {
                rectangle.setBounds(Math.round(c.getLeftWall() * xcale) + xOffSet, Math.round(c.getTopWall() * xcale) + yOffSet,
                        Math.round(c.getRightWall() * xcale) + xOffSet, Math.round(c.getBottomWall() * xcale) + yOffSet);
                rectangle.draw(canvas);
            }


        if (rot == 1)
            for (Cell c : walls.getCells()) {
                rectangle.setBounds(Math.round(c.getTopWall() * xcale) + xOffSet, Math.round(c.getLeftWall() * xcale) + yOffSet,
                        Math.round(c.getBottomWall() * xcale) + xOffSet, Math.round(c.getRightWall() * xcale) + yOffSet);
                rectangle.draw(canvas);
            }
        */
        //PERFECT
        if (rot == 2)
            for (Cell c : walls.getDrawable()) {
                rectangle.setBounds(xOffSet - Math.round(c.getBottomWall() * xcale), yOffSet + Math.round(c.getLeftWall() * xcale),
                        xOffSet - Math.round(c.getTopWall() * xcale), yOffSet + Math.round(c.getRightWall() * xcale));
                rectangle.draw(canvas);
            }


    }

    public LocalizationMethod getLocalizationMethod() {
        return localizationMethod;
    }

    public LocateMeActivity getLocateMeActivity(){
        return this;
    }
}
