package com.kl.visionsdkdemo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.model.BpRecord;
import java.util.List;

public class BpRecordsAdapter extends RecyclerView.Adapter<BpRecordsAdapter.ViewHolder> {
    private final List<BpRecord> records;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(BpRecord record);
    }

    public BpRecordsAdapter(List<BpRecord> records, OnItemClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bp_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BpRecord record = records.get(position);
        holder.tvSystolic.setText(record.getSystolic() + " mmHg");
        holder.tvDiastolic.setText(record.getDiastolic() + " mmHg");
        holder.tvHeartRate.setText(record.getHeartRate() + " BPM");
        holder.tvDateTime.setText(record.getDate() + " " + record.getTime());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(record));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSystolic, tvDiastolic, tvHeartRate, tvDateTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystolic = itemView.findViewById(R.id.tvSystolic);
            tvDiastolic = itemView.findViewById(R.id.tvDiastolic);
            tvHeartRate = itemView.findViewById(R.id.tvHeartRate);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }
}