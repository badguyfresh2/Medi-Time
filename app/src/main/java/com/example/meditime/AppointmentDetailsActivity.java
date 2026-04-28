package com.example.meditime;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AppointmentDetailsActivity extends AppCompatActivity {

    private TextView tvDoctorName, tvSpec, tvDate, tvTime, tvStatus, tvReason, tvNotes, tvFee;
    private Button btnCancel;
    private ProgressBar progressBar;
    private DatabaseReference db;
    private String appointmentId;
    private String patientId;       // added to know whom to notify

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        appointmentId = getIntent().getStringExtra("appointmentId");
        if (appointmentId != null) {
            db = FirebaseDatabase.getInstance().getReference("appointments").child(appointmentId);
        }

        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvSpec       = findViewById(R.id.tvSpecialization);
        tvDate       = findViewById(R.id.tvDate);
        tvTime       = findViewById(R.id.tvTime);
        tvStatus     = findViewById(R.id.tvStatus);
        tvReason     = findViewById(R.id.tvReason);
        tvNotes      = findViewById(R.id.tvNotes);
        tvFee        = findViewById(R.id.tvFee);
        btnCancel    = findViewById(R.id.btnCancel);
        progressBar  = findViewById(R.id.progressBar);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> confirmCancel());
        }

        loadAppointment();
    }

    private void loadAppointment() {
        if (db == null) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (!snapshot.exists()) return;

                // Extract data
                String doctorName = snapshot.child("doctorName").getValue(String.class);
                String spec       = snapshot.child("specialization").getValue(String.class);
                String date       = snapshot.child("date").getValue(String.class);
                String time       = snapshot.child("timeSlot").getValue(String.class);
                String status     = snapshot.child("status").getValue(String.class);
                String reason     = snapshot.child("reason").getValue(String.class);
                String notes      = snapshot.child("notes").getValue(String.class);
                patientId         = snapshot.child("patientId").getValue(String.class);

                Object feeObj = snapshot.child("consultationFeeUGX").getValue();
                Double fee = null;
                if (feeObj instanceof Long) {
                    fee = ((Long) feeObj).doubleValue();
                } else if (feeObj instanceof Double) {
                    fee = (Double) feeObj;
                }

                if (tvDoctorName != null) tvDoctorName.setText("Dr. " + (doctorName != null ? doctorName : ""));
                if (tvSpec != null)   tvSpec.setText(spec != null ? spec : "");
                if (tvDate != null)   tvDate.setText(date != null ? date : "");
                if (tvTime != null)   tvTime.setText(time != null ? time : "");
                if (tvStatus != null) tvStatus.setText(status != null ? status.toUpperCase() : "");
                if (tvReason != null) tvReason.setText(reason != null ? reason : "Not specified");
                if (tvNotes != null)  tvNotes.setText(notes != null && !notes.isEmpty() ? notes : "No notes yet");
                if (tvFee != null)    tvFee.setText(fee != null ? "UGX " + String.format("%,.0f", fee) : "N/A");

                if (btnCancel != null && ("cancelled".equals(status) || "completed".equals(status))) {
                    btnCancel.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(AppointmentDetailsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?")
                .setPositiveButton("Yes, Cancel", (d, w) -> cancelAppointment())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelAppointment() {
        if (db == null) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> update = new HashMap<>();
        update.put("status", "cancelled");

        db.updateChildren(update)
                .addOnSuccessListener(v -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    // ── Save in‑app notification ─────────────────────────
                    if (patientId != null && !patientId.isEmpty()) {
                        String title = "Appointment Cancelled";
                        String body = "Your appointment with Dr. " + tvDoctorName.getText().toString().replace("Dr. ", "")
                                + " on " + tvDate.getText().toString() + " has been cancelled.";
                        saveNotification(patientId, title, body);
                    }

                    Toast.makeText(this, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Notification helper ──────────────────────────────────────────────
    public void saveNotification(String userId, String title, String body) {
        if (userId.isEmpty()) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications").child(userId);
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("body", body);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        ref.push().setValue(notif);   // ✅ pass 'notif' here
    }
}