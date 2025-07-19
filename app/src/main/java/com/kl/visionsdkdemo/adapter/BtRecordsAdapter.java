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
import com.kl.visionsdkdemo.model.BtRecord;
import java.util.List;

public class BtRecordsAdapter extends RecyclerView.Adapter<BtRecordsAdapter.ViewHolder> {
    private List<BtRecord> recordList;
    private Context context;
    private DatabaseHelper databaseHelper;
    private int userId;

    public BtRecordsAdapter(List<BtRecord> recordList, DatabaseHelper databaseHelper, int userId) {
        this.recordList = recordList;
        this.databaseHelper = databaseHelper;
        this.userId = userId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_bt_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BtRecord record = recordList.get(position);
        double temperature = record.getTemperature();

        holder.tvTemperature.setText(String.format("%.1f ℃", temperature));
        holder.tvDateTime.setText(record.getDate() + " " + record.getTime());
        holder.tvNotes.setText(record.getNotes());

        int colorResId;
        if (temperature < 36.0) {
            colorResId = R.color.yellow;
        } else if (temperature <= 37.1) {
            colorResId = R.color.green;
        } else {
            colorResId = R.color.red;
        }
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorResId));

        holder.editButton.setOnClickListener(v -> showEditDialog(record, position));
    }

    private void showEditDialog(BtRecord record, int position) {
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

    private void updateRecordNotes(BtRecord record, String newNotes, int position) {
        boolean success = databaseHelper.updateBtRecordNotes(
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

    public void updateData(List<BtRecord> newRecords, int userId) {
        this.userId = userId;
        recordList.clear();
        recordList.addAll(newRecords);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTemperature, tvDateTime, tvNotes;
        ImageButton editButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTemperature = itemView.findViewById(R.id.tv_temperature);
            tvDateTime = itemView.findViewById(R.id.tv_datetime);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            editButton = itemView.findViewById(R.id.edit_button);
        }
    }
}