package com.kl.visionsdkdemo.model;

import android.os.Parcel;
import android.os.Parcelable;

public class EcgRecord implements Parcelable {
    private int id;
    private int avgHr;
    private Integer respRate;
    private int rrMax;
    private int rrMin;
    private int hrv;
    private String duration;
    private String ecgData;
    private String date;
    private String time;
    private String notes;

    // Constructor
    public EcgRecord(int id, int avgHr, Integer respRate, int rrMax, int rrMin,
                     int hrv, String duration, String ecgData,
                     String date, String time, String notes) {
        this.id = id;
        this.avgHr = avgHr;
        this.respRate = respRate;
        this.rrMax = rrMax;
        this.rrMin = rrMin;
        this.hrv = hrv;
        this.duration = duration;
        this.ecgData = ecgData;
        this.date = date;
        this.time = time;
        this.notes = notes;
    }

    // Parcelable implementation
    protected EcgRecord(Parcel in) {
        id = in.readInt();
        avgHr = in.readInt();
        if (in.readByte() == 0) {
            respRate = null;
        } else {
            respRate = in.readInt();
        }
        rrMax = in.readInt();
        rrMin = in.readInt();
        hrv = in.readInt();
        duration = in.readString();
        ecgData = in.readString();
        date = in.readString();
        time = in.readString();
        notes = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(avgHr);
        if (respRate == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(respRate);
        }
        dest.writeInt(rrMax);
        dest.writeInt(rrMin);
        dest.writeInt(hrv);
        dest.writeString(duration);
        dest.writeString(ecgData);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeString(notes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EcgRecord> CREATOR = new Creator<EcgRecord>() {
        @Override
        public EcgRecord createFromParcel(Parcel in) {
            return new EcgRecord(in);
        }

        @Override
        public EcgRecord[] newArray(int size) {
            return new EcgRecord[size];
        }
    };

    // Getters and setters
    public int getId() { return id; }
    public int getAvgHr() { return avgHr; }
    public Integer getRespRate() { return respRate; }
    public int getRrMax() { return rrMax; }
    public int getRrMin() { return rrMin; }
    public int getHrv() { return hrv; }
    public String getDuration() { return duration; }
    public String getEcgData() { return ecgData; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }
}