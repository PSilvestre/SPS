package com.example.sps;

import java.util.List;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    /**
     * The text view.
     */
    private TextView textRssi;
    /**
     * The button.
     */
    private Button buttonRssi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create items.
        textRssi = (TextView) findViewById(R.id.textRSSI);
        buttonRssi = (Button) findViewById(R.id.buttonRSSI);
        // Set listener for the button.
        buttonRssi.setOnClickListener(this);

        BroadcastReceiver br = new ScanBroadcastReceiver(this);
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(br, filter);
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        // Set text.
        textRssi.setText("\n\tScan all access points:");
        // Set wifi manager.
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // Start a wifi scan.
        boolean scanRes = wifiManager.startScan();
        System.out.println("SCAN ATTEMPT: " + scanRes);
        if(scanRes == true)
            textRssi.setText("Scanning");
        else
            textRssi.setText("Failed");

    }

    public void setText() {
        // Store results in a list.
        List<ScanResult> scanResults = wifiManager.getScanResults();
        // Write results to a label
        for (ScanResult scanResult : scanResults) {
            textRssi.setText(textRssi.getText() + "\n\tBSSID = "
                    + scanResult.BSSID + "    RSSI = "
                    + scanResult.level + "dBm");
        }
    }
}