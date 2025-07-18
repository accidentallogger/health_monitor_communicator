package com.kl.visionsdkdemo.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.SessionManager;
import com.kl.visionsdkdemo.adapter.BpRecordsAdapter;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kl.visionsdkdemo.model.BpRecord;
import java.util.List;

public class BpRecordsFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView tvEmptyView;
    private BpRecordsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bp_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmptyView = view.findViewById(R.id.tvEmptyView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadBpRecords();
    }

    private void loadBpRecords() {
        String phone = new SessionManager(requireContext()).getLoggedInPhone();
        if (phone != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            int userId = dbHelper.getUserId(phone);

            if (userId != -1) {
                List<BpRecord> records = dbHelper.getBpRecordsAsList(userId);

                // Handle empty state
                if (records.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyView.setVisibility(View.GONE);

                    adapter = new BpRecordsAdapter(records, record -> {
                        // Handle item click
                        BpDetailFragment detailFragment = BpDetailFragment.newInstance(record);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.container, detailFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                    recyclerView.setAdapter(adapter);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBpRecords();
    }
}