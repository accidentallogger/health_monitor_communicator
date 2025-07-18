package com.kl.visionsdkdemo.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.databinding.FragmentBpDetailBinding;
import com.kl.visionsdkdemo.model.BpRecord;

public class BpDetailFragment extends Fragment {
    private FragmentBpDetailBinding binding;
    private BpRecord record;

    public static BpDetailFragment newInstance(BpRecord record) {
        BpDetailFragment fragment = new BpDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("record", record);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            record = (BpRecord) getArguments().getSerializable("record");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBpDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (record != null) {
            // Set values
            binding.tvSystolicValue.setText(record.getSystolic() + " / mmHg");
            binding.tvDiastolicValue.setText(record.getDiastolic() + " / mmHg");
            binding.tvHrValue.setText(record.getHeartRate() + " / BPM");

            // Update indicators
            updateIndicators(record.getSystolic(), record.getDiastolic(), record.getHeartRate());

            // Set date and time
            binding.tvDate.setText(record.getDate());
            binding.tvTime.setText(record.getTime());
        }
    }

    private void updateIndicators(int systolic, int diastolic, int heartRate) {
        updatePointerPosition(binding.indicatorPointerSys, systolic, 0, 300);
        updatePointerPosition(binding.indicatorPointerDias, diastolic, 0, 300);
        updatePointerPosition(binding.indicatorPointerHr, heartRate, 0, 300);
    }

    private void updatePointerPosition(View pointer, int value, int min, int max) {
        if (binding == null || pointer == null) return; // Changed from getBinding() to binding

        // Get the parent container
        View parent = (View) pointer.getParent();
        parent.post(() -> {
            int parentWidth = parent.getWidth(); // Actual width in pixels
            int pointerWidth = pointer.getWidth();
            Log.d("IndicatorDebug", "Actual width: " + parentWidth + "px");

            // Calculate position percentage (0-1)
            float percentage = ((float)(value - min) / (float)(max - min));

            // Clamp between 0-1
            percentage = Math.max(0, Math.min(1, percentage));

            // Calculate pixel position (accounting for pointer width)
            float position = percentage * (parentWidth - pointerWidth);

            // Set new position
            if (pointer.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) pointer.getLayoutParams();
                params.horizontalBias = percentage; // This works better for ConstraintLayout
                pointer.setLayoutParams(params);
            } else if (pointer.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pointer.getLayoutParams();
                params.leftMargin = (int) position;
                params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                pointer.setLayoutParams(params);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Clear the binding when the view is destroyed
    }
}