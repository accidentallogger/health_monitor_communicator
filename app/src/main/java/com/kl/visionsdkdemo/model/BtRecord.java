package com.kl.visionsdkdemo.model;

public class BtRecord {
    private double temperature;
    private String date;
    private String time;
    private String notes;

    public BtRecord(double temperature, String date, String time, String notes) {
        this.temperature = temperature;
        this.date = date;
        this.time = time;
        this.notes = notes;
    }

    public double getTemperature() {
        return temperature;
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