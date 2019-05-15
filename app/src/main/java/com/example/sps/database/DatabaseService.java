package com.example.sps.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.wifi.ScanResult;

import com.example.sps.data_collection.Direction;
import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiScan;

import java.util.LinkedList;
import java.util.List;

public class DatabaseService extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "SPSDataBase.db";
    public static final int DATABASE_VERSION = 1;


    public static final String SCAN_TABLE_NAME = "scan";
    public static final String SCAN_COLUMN_SCAN_ID = "scan_id";
    public static final String SCAN_COLUMN_CELL_ID = "cell_id";
    public static final String SCAN_COLUMN_DIR = "dir";
    public static final String SCAN_COLUMN_TIME = "time";


    public static final String SCAN_ITEM_TABLE_NAME = "scan_item";
    public static final String SCAN_ITEM_COLUMN_SCAN_ID = "scan_id";
    public static final String SCAN_ITEM_COLUMN_BSSID = "bssid";
    public static final String SCAN_ITEM_COLUMN_SSID = "ssid";
    public static final String SCAN_ITEM_COLUMN_RSSI = "rssi";

    public static final String GAUSSIANS_TABLE_NAME = "gaussians";
    public static final String GAUSSIANS_COLUMN_CELL_ID = "cell_id";
    public static final String GAUSSIANS_COLUMN_BSSID = "bssid";
    public static final String GAUSSIANS_COLUMN_MEAN = "mean";
    public static final String GAUSSIANS_COLUMN_STDDEV = "stddev";


    private static final String SQL_CREATE_TABLE_SCAN =
            "CREATE TABLE " + SCAN_TABLE_NAME + " (" +
                    SCAN_COLUMN_SCAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SCAN_COLUMN_CELL_ID + " INTEGER," +
                    SCAN_COLUMN_DIR + " TEXT," +
                    SCAN_COLUMN_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    private static final String SQL_DELETE_TABLE_SCAN =
            "DROP TABLE IF EXISTS " + SCAN_TABLE_NAME;




    private static final String SQL_CREATE_TABLE_SCAN_ITEM =
            "CREATE TABLE " + SCAN_ITEM_TABLE_NAME + " (" +
                    SCAN_ITEM_COLUMN_SCAN_ID + " INTEGER," +
                    SCAN_ITEM_COLUMN_BSSID + " TEXT," +
                    SCAN_ITEM_COLUMN_RSSI + " INTEGER," +
                    SCAN_ITEM_COLUMN_SSID + " TEXT," +
                    "FOREIGN KEY (" + SCAN_ITEM_COLUMN_SCAN_ID + ") REFERENCES " + SCAN_TABLE_NAME + " (" + SCAN_COLUMN_SCAN_ID + "))";

    private static final String SQL_DELETE_TABLE_SCAN_ITEM =
            "DROP TABLE IF EXISTS " + SCAN_ITEM_TABLE_NAME;



    private static final String SQL_CREATE_TABLE_GAUSSIANS =
            "CREATE TABLE " + GAUSSIANS_TABLE_NAME + " (" +
                    GAUSSIANS_COLUMN_BSSID + " TEXT NOT NULL," +
                    GAUSSIANS_COLUMN_CELL_ID + " INTEGER NOT NULL," +
                    GAUSSIANS_COLUMN_MEAN + " INTEGER," +
                    GAUSSIANS_COLUMN_STDDEV + " TEXT," +
                    "PRIMARY KEY (" + GAUSSIANS_COLUMN_BSSID + ", " + GAUSSIANS_COLUMN_CELL_ID + ")"  + ")";

    private static final String SQL_DELETE_TABLE_GAUSSIANS =
            "DROP TABLE IF EXISTS " + GAUSSIANS_TABLE_NAME;



    SQLiteDatabase dbconnection;

    public DatabaseService(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.dbconnection = this.getWritableDatabase();

    }
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(SQL_CREATE_TABLE_SCAN_ITEM);
            db.execSQL(SQL_CREATE_TABLE_GAUSSIANS);
            db.execSQL(SQL_CREATE_TABLE_SCAN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_TABLE_SCAN);
        db.execSQL(SQL_DELETE_TABLE_SCAN_ITEM);
        db.execSQL(SQL_DELETE_TABLE_GAUSSIANS);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void insertTableScan(int cellID, Direction dir, List<ScanResult> scanResults) {
        ContentValues rowScanTable = new ContentValues();
        rowScanTable.put(SCAN_COLUMN_CELL_ID, cellID);
        rowScanTable.put(SCAN_COLUMN_DIR, dir.getDir());
        dbconnection.insert(SCAN_TABLE_NAME, null, rowScanTable);

        ContentValues rowScanItem = new ContentValues();

        for (ScanResult result : scanResults) {
            rowScanItem.clear();
            rowScanItem.put(SCAN_ITEM_COLUMN_BSSID, result.BSSID);
            rowScanItem.put(SCAN_ITEM_COLUMN_SSID, result.SSID);
            rowScanItem.put(SCAN_ITEM_COLUMN_RSSI, result.level);
            dbconnection.insert(SCAN_ITEM_TABLE_NAME, null, rowScanItem);
        }
    }

    public List<List<WifiScan>> getRawReadings() {
        List<List<WifiScan>> data = new LinkedList<>();

        Cursor cellCursor = dbconnection.rawQuery("SELECT DISTINCT " + SCAN_COLUMN_CELL_ID + " FROM " + SCAN_TABLE_NAME, new String[]{});

        do{
            data.add(new LinkedList<WifiScan>());
        }while(cellCursor.moveToNext());

        Cursor scanCursor;
        cellCursor.moveToFirst();
        do{
            int cellId = cellCursor.getInt(cellCursor.getColumnIndex(SCAN_COLUMN_CELL_ID));
            scanCursor = dbconnection.rawQuery("SELECT * FROM " + SCAN_TABLE_NAME + " WHERE " + SCAN_COLUMN_CELL_ID +" = " + cellId, new String[]{});
            while(scanCursor.moveToNext()) {
                int scanId = scanCursor.getInt(scanCursor.getColumnIndex(SCAN_COLUMN_SCAN_ID));
                Cursor resultsCursor = dbconnection.rawQuery("SELECT * FROM " + SCAN_ITEM_TABLE_NAME + " WHERE " + SCAN_ITEM_COLUMN_SCAN_ID + " = " + scanId, new String[]{});
                List<WifiReading> readings = new LinkedList<>();
                while (resultsCursor.moveToNext()) {
                    readings.add(new WifiReading(resultsCursor.getString(resultsCursor.getColumnIndex(SCAN_ITEM_COLUMN_BSSID)), resultsCursor.getInt(resultsCursor.getColumnIndex(SCAN_ITEM_COLUMN_RSSI))));
                }
                data.get(cellId).add(new WifiScan(readings));
            }
        }while(cellCursor.moveToNext());
        return data;
    }
}
