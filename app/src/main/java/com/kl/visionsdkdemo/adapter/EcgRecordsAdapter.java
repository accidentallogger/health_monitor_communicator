package com.kl.visionsdkdemo.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kl.visionsdkdemo.EcgDetailActivity;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.model.EcgRecord;

import java.util.ArrayList;
import java.util.List;

public class EcgRecordsAdapter extends RecyclerView.Adapter<EcgRecordsAdapter.ViewHolder> {
    private List<EcgRecord> records;
    private final OnRecordClickListener listener;

    public interface OnRecordClickListener {
        void onRecordClick(EcgRecord record);
    }

    public EcgRecordsAdapter(List<EcgRecord> records, OnRecordClickListener listener) {
        this.records = records != null ? records : new ArrayList<>();
        this.listener = listener;
    }



    public void updateRecords(List<EcgRecord> newRecords) {
        this.records = newRecords != null ? newRecords : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ecg_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (records == null || position < 0 || position >= records.size()) {
            return;
        }

        EcgRecord record = records.get(position);
        holder.recordDate.setText(String.format("%s %s",
                record.getDate() != null ? record.getDate() : "N/A",
                record.getTime() != null ? record.getTime() : "N/A"));

        holder.recordHr.setText(String.format("%d BPM", record.getAvgHr()));

        holder.recordDuration.setText(record.getDuration() != null ?
                record.getDuration() : "N/A");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecordClick(record);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                Log.d("ECG_DEBUG", "Passing record: " + record.toString());
                listener.onRecordClick(record);
            }
        });
    }
    @Override
    public int getItemCount() {
        return records != null ? records.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView recordDate;
        TextView recordHr;
        TextView recordDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recordDate = itemView.findViewById(R.id.recordDate);
            recordHr = itemView.findViewById(R.id.recordHr);
            recordDuration = itemView.findViewById(R.id.recordDuration);
        }
    }
}