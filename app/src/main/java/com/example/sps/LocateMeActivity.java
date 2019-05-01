package com.example.sps;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.example.sps.activity_recognizer.ActivityRecognizer;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_collection.DataCollectionActivity;
import com.example.sps.localization_method.LocalizationMethod;

import java.util.ArrayList;
import java.util.List;

public class LocateMeActivity extends AppCompatActivity {

    private Button initialBeliefButton;
    private Button locateMeButton;
    private Button collectDataButton;

    private TextView cellText;
    private TextView actText;

    private ActivityRecognizer activityRecognizer;
    private LocalizationMethod localizationMethod;


    List<Float> cellProbabilities;
    int numCells;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_me);

        initialBeliefButton = findViewById(R.id.btn_initial_belief);
        locateMeButton = findViewById(R.id.btn_locate_me);
        collectDataButton = findViewById(R.id.btn_collect_data);

        cellText = findViewById(R.id.cell_guess);
        actText = findViewById(R.id.act_guess);

        numCells = 4;

        initialBeliefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialBelief();
            }
        });

        locateMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<ScanResult> scanResults = null;
                int cell_guess = localizationMethod.computeLocation(scanResults);
                List<FloatTriplet> accelorometerData = null;
                SubjectActivity activity = activityRecognizer.recognizeActivity(accelorometerData);


            }
        });

        collectDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent((Activity) view.getContext(), DataCollectionActivity.class);
                startActivity(intent);
            }
        });
    }

    protected void setInitialBelief(){
        cellProbabilities = new ArrayList<>(numCells);
        for(int i = 0; i < numCells; i++)
            cellProbabilities.add(1.0f/numCells);
    }


}
