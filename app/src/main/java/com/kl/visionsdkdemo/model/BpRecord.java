package com.kl.visionsdkdemo.model;

import java.io.Serializable;

public class BpRecord implements Serializable {
    private int systolic;
    private int diastolic;
    private int heartRate;
    private String date;
    private String time;
    private String notes;

    public BpRecord(int systolic, int diastolic, int heartRate, String date, String time, String notes) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.heartRate = heartRate;
        this.date = date;
        this.time = time;
        this.notes = notes;
    }

    // Getters and setters
    public int getSystolic() { return systolic; }
    public int getDiastolic() { return diastolic; }
    public int getHeartRate() { return heartRate; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getNotes() { return notes; }
    
    public void setNotes(String notes) { this.notes = notes; }
}