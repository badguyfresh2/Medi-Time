package com.example.meditime;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meditime.model.Patient;

import java.util.ArrayList;
import java.util.List;

/**
 * Patient list adapter for DoctorPatientsActivity.
 *
 * Each row shows:
 *  • Avatar, name, blood type, email.
 *  • Three action buttons: "View", "Diagnostics", "Records".
 */
public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.VH> {

    public interface OnPatientAction { void onAction(Patient patient); }

    private final List<Patient> fullList = new ArrayList<>();
    private final List<Patient> filtered = new ArrayList<>();

    private final OnPatientAction onView;
    private final OnPatientAction onDiagnostics;
    private final OnPatientAction onRecords;

    // Original constructor (single action) kept for backwards compatibility
    public PatientAdapter(List<Patient> list, OnPatientAction onView) {
        this(list, onView, null, null);
    }

    // Full constructor with all three actions
    public PatientAdapter(List<Patient> list,
                          OnPatientAction onView,
                          OnPatientAction onDiagnostics,
                          OnPatientAction onRecords) {
        this.onView        = onView;
        this.onDiagnostics = onDiagnostics;
        this.onRecords     = onRecords;
        fullList.addAll(list);
        filtered.addAll(list);
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Patient p = filtered.get(pos);

        if (h.tvName      != null) h.tvName.setText(p.getName() != null ? p.getName() : "Patient");
        if (h.tvEmail     != null) h.tvEmail.setText(p.getEmail() != null ? p.getEmail() : "");
        if (h.tvBloodType != null) h.tvBloodType.setText(
                p.getBloodType() != null ? "🩸 " + p.getBloodType() : "");

        // Avatar
        if (h.imgAvatar != null) {
            if (p.getProfileImageUrl() != null && !p.getProfileImageUrl().isEmpty()) {
                Glide.with(h.imgAvatar.getContext())
                        .load(p.getProfileImageUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_person);
            }
        }

        // Row tap → View details
        h.itemView.setOnClickListener(v -> { if (onView != null) onView.onAction(p); });

        // "View" button
        if (h.btnView != null) h.btnView.setOnClickListener(v -> { if (onView != null) onView.onAction(p); });

        // "Diagnostics" button
        if (h.btnDiagnostics != null) {
            h.btnDiagnostics.setVisibility(onDiagnostics != null ? View.VISIBLE : View.GONE);
            h.btnDiagnostics.setOnClickListener(v -> { if (onDiagnostics != null) onDiagnostics.onAction(p); });
        }

        // "Records" button
        if (h.btnRecords != null) {
            h.btnRecords.setVisibility(onRecords != null ? View.VISIBLE : View.GONE);
            h.btnRecords.setOnClickListener(v -> { if (onRecords != null) onRecords.onAction(p); });
        }
    }

    @Override public int getItemCount() { return filtered.size(); }

    /** Live search filter. Call from TextWatcher. */
    public void filter(String query) {
        filtered.clear();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(fullList);
        } else {
            String q = query.toLowerCase().trim();
            for (Patient p : fullList) {
                if ((p.getName()  != null && p.getName().toLowerCase().contains(q))
                 || (p.getEmail() != null && p.getEmail().toLowerCase().contains(q))) {
                    filtered.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    /** Sync with external list changes (call notifyDataSetChanged after mutating source). */
    @Override public void notifyDataSetChanged() {
        // Re-sync filtered list from fullList if no active search
        super.notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView  tvName, tvEmail, tvBloodType;
        Button    btnView, btnDiagnostics, btnRecords;

        VH(View v) {
            super(v);
            imgAvatar       = v.findViewById(R.id.imgPatientAvatar);
            tvName          = v.findViewById(R.id.tvPatientName);
            tvEmail         = v.findViewById(R.id.tvPatientEmail);
            tvBloodType     = v.findViewById(R.id.tvBloodType);
            btnView         = v.findViewById(R.id.btnViewPatient);
            btnDiagnostics  = v.findViewById(R.id.btnPatientDiagnostics);
            btnRecords      = v.findViewById(R.id.btnPatientRecords);
        }
    }
}
