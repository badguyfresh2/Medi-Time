package com.example.meditime;

import android.graphics.Color;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.Appointment;
import java.util.List;

/**
 * General-purpose appointment adapter (used in HomeActivity and other screens).
 * For doctor-specific view use DoctorAppointmentAdapter.
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.VH> {
    public interface OnClick { void onClick(Appointment a); }

    private final List<Appointment> list;
    private final OnClick listener;

    public AppointmentAdapter(List<Appointment> list, OnClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Appointment a = list.get(pos);
        if (h.tvDoctor  != null) h.tvDoctor.setText("Dr. " + (a.getDoctorName() != null ? a.getDoctorName() : ""));
        if (h.tvSpec    != null) h.tvSpec.setText(a.getSpecialization() != null ? a.getSpecialization() : "");
        if (h.tvDate    != null) h.tvDate.setText((a.getDate() != null ? a.getDate() : "") + "  " + (a.getTimeSlot() != null ? a.getTimeSlot() : ""));
        if (h.tvStatus  != null) {
            String status = a.getStatus() != null ? a.getStatus() : "";
            h.tvStatus.setText(status.toUpperCase());
            switch (status) {
                case "confirmed":  h.tvStatus.setTextColor(Color.parseColor("#059669")); break;
                case "cancelled":  h.tvStatus.setTextColor(Color.parseColor("#EF4444")); break;
                default:           h.tvStatus.setTextColor(Color.parseColor("#D97706")); break;
            }
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(a); });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDoctor, tvSpec, tvDate, tvStatus;
        VH(View v) {
            super(v);
            tvDoctor = v.findViewById(R.id.tvDoctorName);
            tvSpec   = v.findViewById(R.id.tvSpecialization);
            tvDate   = v.findViewById(R.id.tvDate);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}
