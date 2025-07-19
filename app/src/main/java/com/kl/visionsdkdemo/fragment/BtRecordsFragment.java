package com.kl.visionsdkdemo.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.SessionManager;
import com.kl.visionsdkdemo.adapter.BtRecordsAdapter;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kl.visionsdkdemo.model.BtRecord;
import java.util.ArrayList;
import java.util.List;

public class BtRecordsFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private BtRecordsAdapter adapter;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private int userId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bt_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tv_empty);


        adapter = new BtRecordsAdapter(new ArrayList<>(), databaseHelper, userId);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);


        loadRecords();
    }

    private void loadRecords() {
        String phone = sessionManager.getLoggedInPhone();
        if (phone == null) {
            showEmptyState();
            return;
        }

        userId = databaseHelper.getUserId(phone);
        if (userId == -1) {
            showEmptyState();
            return;
        }

        List<BtRecord> records = databaseHelper.getBtRecordsAsList(userId);
        if (records.isEmpty()) {
            showEmptyState();
        } else {
            showRecords(records);
        }
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("No temperature records found");
    }

    private void showRecords(List<BtRecord> records) {
        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        adapter.updateData(records, userId);
    }

    @Override
    public void onDestroy() {
        databaseHelper.close();
        super.onDestroy();
    }
}