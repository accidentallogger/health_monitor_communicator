package com.kl.visionsdkdemo.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kl.visionsdkdemo.model.Spo2Record;
import java.util.List;

public class Spo2RecordsAdapter extends RecyclerView.Adapter<Spo2RecordsAdapter.ViewHolder> {
    private List<Spo2Record> recordList;
    private Context context;
    private DatabaseHelper databaseHelper;
    private int userId;

    public Spo2RecordsAdapter(List<Spo2Record> recordList, DatabaseHelper databaseHelper, int userId) {
        this.recordList = recordList;
        this.databaseHelper = databaseHelper;
        this.userId = userId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_spo2_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Spo2Record record = recordList.get(position);

        holder.tvSpo2.setText(String.format("%.1f %%", record.getSpo2()));
        holder.tvHr.setText(record.getHeartRate() + " bpm");
        holder.tvDateTime.setText(record.getDate() + " " + record.getTime());
        holder.tvNotes.setText(record.getNotes());

        // Set click listener for edit button
        holder.editButton.setOnClickListener(v -> showEditDialog(record, position));
    }

    private void showEditDialog(Spo2Record record, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_notes, null);
        builder.setView(dialogView);

        EditText etNotes = dialogView.findViewById(R.id.et_notes);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnOk = dialogView.findViewById(R.id.btn_ok);

        etNotes.setText(record.getNotes());

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            String newNotes = etNotes.getText().toString().trim();
            updateRecordNotes(record, newNotes, position);
            dialog.dismiss();
        });
    }

    private void updateRecordNotes(Spo2Record record, String newNotes, int position) {
        boolean success = databaseHelper.updateSpo2RecordNotes(
                userId,
                record.getDate(),
                record.getTime(),
                newNotes
        );

        if (success) {
            record.setNotes(newNotes);
            notifyItemChanged(position);
            Toast.makeText(context, "Notes updated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Failed to update notes", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    public void updateData(List<Spo2Record> newRecords, int userId) {
        this.userId = userId;
        recordList.clear();
        recordList.addAll(newRecords);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvSpo2, tvHr, tvDateTime, tvNotes;
        ImageButton editButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvSpo2 = itemView.findViewById(R.id.tv_spo2);
            tvHr = itemView.findViewById(R.id.tv_hr);
            tvDateTime = itemView.findViewById(R.id.tv_datetime);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            editButton = itemView.findViewById(R.id.edit_button);
        }
    }
}