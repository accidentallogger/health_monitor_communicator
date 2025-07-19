package com.kl.visionsdkdemo.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.kl.visionsdkdemo.model.BpRecord;
import com.kl.visionsdkdemo.model.BtRecord;
import com.kl.visionsdkdemo.model.EcgRecord;
import com.kl.visionsdkdemo.model.Spo2Record;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserManager.db";
    private static final int DATABASE_VERSION = 4;

    // User table
    private static final String TABLE_USER = "users";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USER_PHONE = "phone";
    private static final String COLUMN_USER_PASSWORD = "password";
    private static final String COLUMN_USER_CREATED_AT = "created_at";


    private static final String TABLE_BT_RECORDS = "bt_records";
    private static final String COLUMN_RECORD_ID = "record_id";
    private static final String COLUMN_RECORD_USER_ID = "user_id";
    public static final String COLUMN_BT_VALUE = "bt_value";
    public static final String COLUMN_RECORD_DATE = "record_date";
    public static final String COLUMN_RECORD_TIME = "record_time";
    public static final String COLUMN_NOTES = "notes";


    // table SQL
    private static final String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + "("
            + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_PHONE + " TEXT UNIQUE NOT NULL,"
            + COLUMN_USER_PASSWORD + " TEXT NOT NULL,"
            + COLUMN_USER_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys=ON;");
        db.close();
    }

    private static final String CREATE_BT_RECORDS_TABLE = "CREATE TABLE " + TABLE_BT_RECORDS + "("
            + COLUMN_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_RECORD_USER_ID + " INTEGER NOT NULL,"
            + COLUMN_BT_VALUE + " REAL NOT NULL,"
            + COLUMN_RECORD_DATE + " TEXT NOT NULL,"
            + COLUMN_RECORD_TIME + " TEXT NOT NULL,"
            + COLUMN_NOTES + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_RECORD_USER_ID + ") REFERENCES " + TABLE_USER + "(" + COLUMN_USER_ID + ")"
            + ")";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_BT_RECORDS_TABLE);
        db.execSQL(CREATE_SPO2_RECORDS_TABLE);
        db.execSQL(CREATE_BP_RECORDS_TABLE);
        db.execSQL(CREATE_ECG_RECORDS_TABLE);

    }

    public long addBtRecord(int userId, double btValue, String date, String time, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(COLUMN_RECORD_USER_ID, userId);
            values.put(COLUMN_BT_VALUE, btValue);
            values.put(COLUMN_RECORD_DATE, date);
            values.put(COLUMN_RECORD_TIME, time);
            values.put(COLUMN_NOTES, notes);

            id = db.insert(TABLE_BT_RECORDS, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error adding BT record", e);
        } finally {
            db.endTransaction();
            db.close();
        }
        return id;
    }

    public int getUserId(String phone) {
        if (phone == null || phone.isEmpty()) {
            return -1;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        int id = -1;
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_USER,
                    new String[]{COLUMN_USER_ID},
                    COLUMN_USER_PHONE + "=?",
                    new String[]{phone},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting user ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return id;
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT_RECORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SPO2_RECORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BP_RECORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ECG_RECORDS);

        onCreate(db);
    }

    /*public boolean isDatabaseValid() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name IN(?,?)",
                new String[]{TABLE_USER, TABLE_BT_RECORDS});

        int tableCount = cursor.getCount();
        cursor.close();
        db.close();

        return tableCount == 2; // Both tables should exist
    }*/
    public boolean updateBtRecordNotes(int userId, String date, String time, String newNotes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTES, newNotes);

        int rowsAffected = db.update(TABLE_BT_RECORDS, values,
                COLUMN_RECORD_USER_ID + "=? AND " +
                        COLUMN_RECORD_DATE + "=? AND " +
                        COLUMN_RECORD_TIME + "=?",
                new String[]{String.valueOf(userId), date, time});

        db.close();
        return rowsAffected > 0;
    }
    public List<BtRecord> getBtRecordsAsList(int userId) {
        List<BtRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_BT_RECORDS,
                    new String[]{COLUMN_BT_VALUE, COLUMN_RECORD_DATE, COLUMN_RECORD_TIME, COLUMN_NOTES},
                    COLUMN_RECORD_USER_ID + "=?",
                    new String[]{String.valueOf(userId)},
                    null, null,
                    COLUMN_RECORD_DATE + " DESC, " + COLUMN_RECORD_TIME + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    double value = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BT_VALUE));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_DATE));
                    String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_TIME));
                    String notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES));

                    records.add(new BtRecord(value, date, time, notes));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return records;
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    /*public boolean validateDatabase() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        boolean usersTableExists = false;
        boolean btTableExists = false;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                if (tableName.equals(TABLE_USER)) usersTableExists = true;
                if (tableName.equals(TABLE_BT_RECORDS)) btTableExists = true;
            }
            cursor.close();
        }
        db.close();

        return usersTableExists && btTableExists;
    }*/
    public boolean checkUserExists(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_PHONE};
        String selection = COLUMN_USER_PHONE + " = ?";
        String[] selectionArgs = {phone};

        Cursor cursor = db.query(TABLE_USER, columns, selection, selectionArgs,
                null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();

        return count > 0;
    }


    // User management
    public void addUser(String phone, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_PHONE, phone);
        values.put(COLUMN_USER_PASSWORD, password); // Note: Store hashed password in production
        
        db.insert(TABLE_USER, null, values);
        db.close();
    }

    public boolean checkUser(String phone, String password) {
        String[] columns = {COLUMN_USER_ID};
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USER_PHONE + " = ?" + " AND " + COLUMN_USER_PASSWORD + " = ?";
        String[] selectionArgs = {phone, password};

        Cursor cursor = db.query(TABLE_USER, columns, selection, selectionArgs,
                null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();

        return count > 0;
    }

    //SPO2
    private static final String TABLE_SPO2_RECORDS = "spo2_records";
    private static final String COLUMN_SPO2_VALUE = "spo2_value";
    private static final String COLUMN_HR_VALUE = "hr_value";

    private static final String CREATE_SPO2_RECORDS_TABLE = "CREATE TABLE " + TABLE_SPO2_RECORDS + "("
            + COLUMN_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_RECORD_USER_ID + " INTEGER NOT NULL,"
            + COLUMN_SPO2_VALUE + " REAL NOT NULL,"
            + COLUMN_HR_VALUE + " INTEGER NOT NULL,"
            + COLUMN_RECORD_DATE + " TEXT NOT NULL,"
            + COLUMN_RECORD_TIME + " TEXT NOT NULL,"
            + COLUMN_NOTES + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_RECORD_USER_ID + ") REFERENCES " + TABLE_USER + "(" + COLUMN_USER_ID + ")"
            + ")";


    public long addSpo2Record(int userId, double spo2, int heartRate, String date, String time, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RECORD_USER_ID, userId);
        values.put(COLUMN_SPO2_VALUE, spo2);
        values.put(COLUMN_HR_VALUE, heartRate);
        values.put(COLUMN_RECORD_DATE, date);
        values.put(COLUMN_RECORD_TIME, time);
        values.put(COLUMN_NOTES, notes);

        long id = db.insert(TABLE_SPO2_RECORDS, null, values);
        db.close();
        return id;
    }

    public List<Spo2Record> getSpo2RecordsAsList(int userId) {
        List<Spo2Record> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SPO2_RECORDS,
                new String[]{COLUMN_SPO2_VALUE, COLUMN_HR_VALUE, COLUMN_RECORD_DATE, COLUMN_RECORD_TIME, COLUMN_NOTES},
                COLUMN_RECORD_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_RECORD_DATE + " DESC, " + COLUMN_RECORD_TIME + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                double spo2 = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SPO2_VALUE));
                int hr = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HR_VALUE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_TIME));
                String notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES));

                records.add(new Spo2Record(spo2, hr, date, time, notes));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return records;
    }

    public boolean updateSpo2RecordNotes(int userId, String date, String time, String newNotes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTES, newNotes);

        int rowsAffected = db.update(TABLE_SPO2_RECORDS, values,
                COLUMN_RECORD_USER_ID + "=? AND " +
                        COLUMN_RECORD_DATE + "=? AND " +
                        COLUMN_RECORD_TIME + "=?",
                new String[]{String.valueOf(userId), date, time});

        db.close();
        return rowsAffected > 0;
    }


    //BP
    private static final String TABLE_BP_RECORDS = "bp_records";
    private static final String COLUMN_SYSTOLIC = "systolic";
    private static final String COLUMN_DIASTOLIC = "diastolic";
    private static final String COLUMN_HR_VALUE_BP = "hr_value";

    private static final String CREATE_BP_RECORDS_TABLE = "CREATE TABLE " + TABLE_BP_RECORDS + "("
            + COLUMN_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_RECORD_USER_ID + " INTEGER NOT NULL,"
            + COLUMN_SYSTOLIC + " INTEGER NOT NULL,"
            + COLUMN_DIASTOLIC + " INTEGER NOT NULL,"
            + COLUMN_HR_VALUE_BP + " INTEGER NOT NULL,"
            + COLUMN_RECORD_DATE + " TEXT NOT NULL,"
            + COLUMN_RECORD_TIME + " TEXT NOT NULL,"
            + COLUMN_NOTES + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_RECORD_USER_ID + ") REFERENCES " + TABLE_USER + "(" + COLUMN_USER_ID + ")"
            + ")";



    public long addBpRecord(int userId, int systolic, int diastolic, int heartRate, String date, String time, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RECORD_USER_ID, userId);
        values.put(COLUMN_SYSTOLIC, systolic);
        values.put(COLUMN_DIASTOLIC, diastolic);
        values.put(COLUMN_HR_VALUE_BP, heartRate);
        values.put(COLUMN_RECORD_DATE, date);
        values.put(COLUMN_RECORD_TIME, time);
        values.put(COLUMN_NOTES, notes);

        long id = db.insert(TABLE_BP_RECORDS, null, values);
        db.close();
        return id;
    }

    public List<BpRecord> getBpRecordsAsList(int userId) {
        List<BpRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BP_RECORDS,
                new String[]{COLUMN_RECORD_ID, COLUMN_SYSTOLIC, COLUMN_DIASTOLIC, COLUMN_HR_VALUE_BP,
                        COLUMN_RECORD_DATE, COLUMN_RECORD_TIME, COLUMN_NOTES},
                COLUMN_RECORD_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_RECORD_DATE + " DESC, " + COLUMN_RECORD_TIME + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int recordId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RECORD_ID));
                int systolic = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYSTOLIC));
                int diastolic = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DIASTOLIC));
                int hr = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HR_VALUE_BP));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_TIME));
                String notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES));

                records.add(new BpRecord(recordId, systolic, diastolic, hr, date, time, notes));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return records;
    }

    public boolean updateBpRecordNotes(int recordId, String newNotes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTES, newNotes);

        int rowsAffected = db.update(TABLE_BP_RECORDS,
                values,
                COLUMN_RECORD_ID + "=?",
                new String[]{String.valueOf(recordId)});

        db.close();
        return rowsAffected > 0;
    }


    //ECG

    private static final String TABLE_ECG_RECORDS = "ecg_records";
    private static final String COLUMN_AVG_HR = "avg_hr";
    private static final String COLUMN_RESP_RATE = "resp_rate";
    private static final String COLUMN_RR_MAX = "rr_max";
    private static final String COLUMN_RR_MIN = "rr_min";
    private static final String COLUMN_HRV = "hrv";
    private static final String COLUMN_ECG_DURATION = "ecg_duration";
    private static final String COLUMN_ECG_DATA = "ecg_data";
    private static final String CREATE_ECG_RECORDS_TABLE = "CREATE TABLE " + TABLE_ECG_RECORDS + "("
            + COLUMN_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_RECORD_USER_ID + " INTEGER NOT NULL,"
            + COLUMN_AVG_HR + " INTEGER NOT NULL,"
            + COLUMN_RESP_RATE + " INTEGER,"
            + COLUMN_RR_MAX + " INTEGER NOT NULL,"
            + COLUMN_RR_MIN + " INTEGER NOT NULL,"
            + COLUMN_HRV + " INTEGER NOT NULL,"
            + COLUMN_ECG_DURATION + " TEXT NOT NULL,"
            + COLUMN_ECG_DATA + " TEXT NOT NULL," // Store ECG waveform data as JSON
            + COLUMN_RECORD_DATE + " TEXT NOT NULL,"
            + COLUMN_RECORD_TIME + " TEXT NOT NULL,"
            + COLUMN_NOTES + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_RECORD_USER_ID + ") REFERENCES " + TABLE_USER + "(" + COLUMN_USER_ID + ")"
            + ")";
    public long addEcgRecord(int userId, int avgHr, Integer respRate, int rrMax, int rrMin,
                             int hrv, String duration, String ecgData,
                             String date, String time, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(COLUMN_RECORD_USER_ID, userId);
            values.put(COLUMN_AVG_HR, avgHr);
            values.put(COLUMN_RESP_RATE, respRate);
            values.put(COLUMN_RR_MAX, rrMax);
            values.put(COLUMN_RR_MIN, rrMin);
            values.put(COLUMN_HRV, hrv);
            values.put(COLUMN_ECG_DURATION, duration);
            values.put(COLUMN_ECG_DATA, ecgData);
            values.put(COLUMN_RECORD_DATE, date);
            values.put(COLUMN_RECORD_TIME, time);
            values.put(COLUMN_NOTES, notes);

            id = db.insert(TABLE_ECG_RECORDS, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error adding ECG record", e);
        } finally {
            db.endTransaction();
            db.close();
        }
        return id;
    }

    public List<EcgRecord> getEcgRecordsAsList(int userId) {
        List<EcgRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_ECG_RECORDS,
                    new String[]{COLUMN_RECORD_ID, COLUMN_AVG_HR, COLUMN_RESP_RATE,
                            COLUMN_RR_MAX, COLUMN_RR_MIN, COLUMN_HRV,
                            COLUMN_ECG_DURATION, COLUMN_ECG_DATA,
                            COLUMN_RECORD_DATE, COLUMN_RECORD_TIME, COLUMN_NOTES},
                    COLUMN_RECORD_USER_ID + "=?",
                    new String[]{String.valueOf(userId)},
                    null, null,
                    COLUMN_RECORD_DATE + " DESC, " + COLUMN_RECORD_TIME + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RECORD_ID));
                    int avgHr = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AVG_HR));
                    int respRate = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RESP_RATE));
                    int rrMax = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RR_MAX));
                    int rrMin = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RR_MIN));
                    int hrv = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HRV));
                    String duration = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ECG_DURATION));
                    String ecgData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ECG_DATA));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_DATE));
                    String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_TIME));
                    String notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES));

                    records.add(new EcgRecord(id, avgHr, respRate, rrMax, rrMin, hrv,
                            duration, ecgData, date, time, notes));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return records;
    }

    public boolean updateEcgRecordNotes(int recordId, String newNotes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTES, newNotes);

        int rowsAffected = db.update(TABLE_ECG_RECORDS, values,
                COLUMN_RECORD_ID + "=?",
                new String[]{String.valueOf(recordId)});

        db.close();
        return rowsAffected > 0;
    }
    public EcgRecord getEcgRecordById(int userId, int recordId) {
        SQLiteDatabase db = this.getReadableDatabase();
        EcgRecord record = null;
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_ECG_RECORDS,
                    new String[]{COLUMN_RECORD_ID, COLUMN_AVG_HR, COLUMN_RESP_RATE,
                            COLUMN_RR_MAX, COLUMN_RR_MIN, COLUMN_HRV,
                            COLUMN_ECG_DURATION, COLUMN_ECG_DATA,
                            COLUMN_RECORD_DATE, COLUMN_RECORD_TIME, COLUMN_NOTES},
                    COLUMN_RECORD_USER_ID + "=? AND " + COLUMN_RECORD_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(recordId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RECORD_ID));
                int avgHr = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AVG_HR));
                Integer respRate = cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_RESP_RATE)) ?
                        null : cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RESP_RATE));
                int rrMax = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RR_MAX));
                int rrMin = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RR_MIN));
                int hrv = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HRV));
                String duration = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ECG_DURATION));
                String ecgData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ECG_DATA));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_TIME));
                String notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES));

                record = new EcgRecord(id, avgHr, respRate, rrMax, rrMin, hrv,
                        duration, ecgData, date, time, notes);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return record;
    }
}


