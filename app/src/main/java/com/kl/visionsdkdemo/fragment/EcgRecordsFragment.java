package com.kl.visionsdkdemo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kl.visionsdkdemo.EcgDetailActivity;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.SessionManager;
import com.kl.visionsdkdemo.adapter.EcgRecordsAdapter;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kl.visionsdkdemo.model.EcgRecord;

import java.util.ArrayList;
import java.util.List;

public class EcgRecordsFragment extends Fragment {
    private EcgRecordsAdapter adapter;
    private List<EcgRecord> ecgRecords = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private int userId;
    private TextView emptyStateView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ecg_records, container, false);

        emptyStateView = new TextView(getContext());
        emptyStateView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyStateView.setText("No ECG records found");
        emptyStateView.setGravity(Gravity.CENTER);
        emptyStateView.setTextSize(16);
        emptyStateView.setTextColor(getResources().getColor(android.R.color.black));
        emptyStateView.setPadding(0, 0, 0, 0);

        ((ViewGroup) view).addView(emptyStateView);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());
        SessionManager session = new SessionManager(requireContext());
        String phone = session.getLoggedInPhone();
        userId = dbHelper.getUserId(phone);

        RecyclerView recyclerView = view.findViewById(R.id.ecgRecordsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new EcgRecordsAdapter(ecgRecords, this::onRecordClicked);
        recyclerView.setAdapter(adapter);

        loadEcgRecords();
    }

    private void loadEcgRecords() {
        if (userId != -1) {
            ecgRecords = dbHelper.getEcgRecordsAsList(userId);
            adapter.updateRecords(ecgRecords);

            if (ecgRecords.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
            } else {
                emptyStateView.setVisibility(View.GONE);
            }
        }
    }

    private void onRecordClicked(EcgRecord record) {
        try {
            Intent intent = new Intent(requireContext(), EcgDetailActivity.class);
            intent.putExtra("record_id", record.getId());
            startActivity(intent);
        } catch (Exception e) {
            Log.e("EcgRecordsFragment", "Error showing ECG details", e);
            Toast.makeText(requireContext(), "Error showing details", Toast.LENGTH_SHORT).show();
        }
    }
}
