package com.example.sps.data_collection;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.sps.R;

public class DataCollectionActivity extends AppCompatActivity {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;

    private Button[] btns;

    private TextView[] txts;

    private  Integer[] counters = {0, 0, 0, 0};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        btns = new Button[4];
        txts = new TextView[4];

        btns[0] = (Button) findViewById(R.id.buttonCell1);
        btns[1] = (Button) findViewById(R.id.buttonCell2);
        btns[2] = (Button) findViewById(R.id.buttonCell3);
        btns[3] = (Button) findViewById(R.id.buttonCell4);

        txts[0] = (TextView) findViewById(R.id.textCell1);
        txts[1] = (TextView) findViewById(R.id.textCell2);
        txts[2] = (TextView) findViewById(R.id.textCell3);
        txts[3] = (TextView) findViewById(R.id.textCell4);


        for(int i = 0; i < 4; i++) {
            btns[i].setOnClickListener(new DataCollectOnClickListener("/sps/readings" + i, txts[i], wifiManager, this, counters[i]));
        }


    }

}
