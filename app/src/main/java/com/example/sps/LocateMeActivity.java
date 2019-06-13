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

import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.FileWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

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

    public static final int DRAW_FRAMES_PER_SECOND = 4;
    public static final int SKIP_TICKS_DRAW = Math.round(1000.0f / DRAW_FRAMES_PER_SECOND);

    public static final int UPDATE_FRAMES_PER_SECOND = 8;
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


        accelerometerData = new LinkedBlockingQueue<FloatTriplet>();
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
                actMiscText.setText("");
                if (scanData != null)
                    scanData.removeAll(scanData);

                if (!(localizationMethod instanceof ContinuousLocalization))
                    new Thread(new singleLocalizationRunnable()).start();
                else {
                    new Thread(new continuousLocalizationRunnable()).start();
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

    private class singleLocalizationRunnable implements Runnable {
        public void run() {
            update = true;

            //Start wifi scan
            wifiManager.startScan();
            accelerometerData.removeAll(accelerometerData);
            sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

            while (scanData == null || scanData.size() == 0 || accelerometerData.size() < NUM_ACC_READINGS) { //spin while data not ready
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //when finished, compute location and activity and post to user. unregister accelorometer listener
            sensorManager.unregisterListener(accelerometerListener);

            final SubjectActivity activity = activityRecognizer.recognizeActivity(accelerometerData);

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


    private class continuousLocalizationRunnable implements Runnable {

        @Override
        public void run() {

            update = false;
            sensorManager.registerListener(new AccelerometerListener(accelerometerData), accelerometer, 100000);
            // Spread particles
            particles = ((ContinuousLocalization) localizationMethod).spreadParticles(cellProbabilities);
            long lastUpdateTime = System.currentTimeMillis();
            new CountParticleWeightsThread(particles, walls, getLocateMeActivity()).start();

            long nextTick = System.currentTimeMillis();
            long sleepTime = 0;

            while (localizationMethod instanceof ContinuousLocalization) {

                updateParticles(lastUpdateTime);
                lastUpdateTime = System.currentTimeMillis();


                nextTick += SKIP_TICKS_UPDATE;
                sleepTime = nextTick -System.currentTimeMillis();
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

    private void updateParticles(long lastUpdateTime) {
        float avgWalkingSpeed = 1.4f; // m/s
        float distance = 0;
        long currentTime = System.currentTimeMillis();
        float timePassed = (currentTime - lastUpdateTime) / 1000.0f;
        final SubjectActivity a = activityRecognizer.recognizeActivity(accelerometerData);


        if(a == SubjectActivity.WALKING) {
            distance = timePassed * avgWalkingSpeed;
        }
        if (a == SubjectActivity.RUNNING)
            distance = timePassed * avgWalkingSpeed * 2;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                actMiscText.setText("Activity: " + a.name() + ". "+ localizationMethod.getMiscInfo());
            }
        });
        ((ContinuousLocalization) localizationMethod).updateParticles(mAzimuth, distance, particles);


        collideAndResample(particles);


        distanceCumulative += distance;
        update = true;
        return;
    }

    private void collideAndResample(CopyOnWriteArrayList<Particle> particles) {
        LinkedList<Particle> deadParticles = new LinkedList<>();

        //collide and erase
        for (Particle p : particles) {
            if (walls.getDrawable().get(p.getCell()).collide(p))
                deadParticles.add(p);
        }
        particles.removeAll(deadParticles);

        boolean cellFound;
        int cell_slack = 4; //check collisions in cells in the proximity (+-cell_slack)
        for (Particle p : particles) {
            cellFound = false;
            for(int i = Math.max(0, p.getCell() - cell_slack); i < Math.min(walls.getCells().size(), p.getCell() + cell_slack + 1); i++) {
                if(walls.getDrawable().get(i).isParticleInside(p)) {
                    p.setCell(i);
                    cellFound = true;
                    break;
                }
            }
            if (!cellFound) {
                deadParticles.add(p);
            }
        }
        particles.removeAll(deadParticles);

        Random r = new Random(System.currentTimeMillis());
        for (Particle p : deadParticles) {
            Particle selected = particles.get(r.nextInt(particles.size()));

            p.setCell(selected.getCell());

            NormalDistribution resampleNoise = new NormalDistribution(0, 3);

            p.setY((float) (selected.getY() + resampleNoise.sample()));
            p.setX((float) (selected.getX()+ resampleNoise.sample()));
        }
        particles.addAll(deadParticles);
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

        double stopX = 200 * Math.cos((mAzimuth + 90) / 180 * Math.PI);
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

        /*normal
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
