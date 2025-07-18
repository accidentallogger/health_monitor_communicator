package com.kl.visionsdkdemo.model;

public class Spo2Record {
    private double spo2;
    private int heartRate;
    private String date;
    private String time;
    private String notes;

    public Spo2Record(double spo2, int heartRate, String date, String time, String notes) {
        this.spo2 = spo2;
        this.heartRate = heartRate;
        this.date = date;
        this.time = time;
        this.notes = notes;
    }

    // Getters and setters
    public double getSpo2() {
        return spo2;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}