package com.example.meditime;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class ViewNotesActivity extends AppCompatActivity {

    private TextView tvPatientName, tvDate, tvTime, tvStatus, tvReason;
    private EditText etNotes;
    private Button btnApprove, btnDecline, btnCancel, btnSaveNotes, btnComplete;
    private LinearLayout layoutActions;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private ValueEventListener listener;
    private String appointmentId, currentStatus, doctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notes);

        appointmentId = getIntent().getStringExtra("appointmentId");
        if (appointmentId != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("appointments").child(appointmentId);
        }

        doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        tvPatientName = findViewById(R.id.tvPatientName);
        tvDate        = findViewById(R.id.tvDate);
        tvTime        = findViewById(R.id.tvTime);
        tvStatus      = findViewById(R.id.tvStatus);
        tvReason      = findViewById(R.id.tvReason);
        etNotes       = findViewById(R.id.etNotes);
        btnApprove    = findViewById(R.id.btnApprove);
        btnDecline    = findViewById(R.id.btnDecline);
        btnCancel     = findViewById(R.id.btnCancel);
        btnSaveNotes  = findViewById(R.id.btnSaveNotes);
        btnComplete   = findViewById(R.id.btnComplete);
        layoutActions = findViewById(R.id.layoutActions);
        progressBar   = findViewById(R.id.progressBar);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnApprove != null)   btnApprove.setOnClickListener(v -> updateStatus("confirmed"));
        if (btnDecline != null)   btnDecline.setOnClickListener(v -> confirmAction("decline", "declined"));
        if (btnCancel != null)    btnCancel.setOnClickListener(v -> confirmAction("cancel", "cancelled"));
        if (btnComplete != null)  btnComplete.setOnClickListener(v -> updateStatus("completed"));
        if (btnSaveNotes != null) btnSaveNotes.setOnClickListener(v -> saveNotes());

        loadAppointment();
    }

    private void loadAppointment() {
        if (appointmentId == null || dbRef == null) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        listener = dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (!snapshot.exists()) return;

                currentStatus = snapshot.child("status").getValue(String.class);
                if (currentStatus == null) currentStatus = "";
                
                String patient = snapshot.child("patientName").getValue(String.class);
                String date    = snapshot.child("date").getValue(String.class);
                String time    = snapshot.child("timeSlot").getValue(String.class);
                String reason  = snapshot.child("reason").getValue(String.class);
                String notes   = snapshot.child("notes").getValue(String.class);

                if (tvPatientName != null) tvPatientName.setText(patient != null ? patient : "Patient");
                if (tvDate != null)   tvDate.setText(date != null ? date : "");
                if (tvTime != null)   tvTime.setText(time != null ? time : "");
                if (tvStatus != null) tvStatus.setText(currentStatus.toUpperCase());
                if (tvReason != null) tvReason.setText(reason != null ? reason : "Not specified");
                if (etNotes != null && notes != null && !notes.isEmpty()) etNotes.setText(notes);

                updateActionVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateActionVisibility() {
        if (btnApprove == null) return;
        boolean isPending   = "pending".equals(currentStatus);
        boolean isConfirmed = "confirmed".equals(currentStatus);
        boolean isDone      = "cancelled".equals(currentStatus) || "completed".equals(currentStatus) || "declined".equals(currentStatus);

        btnApprove.setVisibility(isPending ? View.VISIBLE : View.GONE);
        btnDecline.setVisibility(isPending ? View.VISIBLE : View.GONE);
        btnCancel.setVisibility(!isDone ? View.VISIBLE : View.GONE);
        btnComplete.setVisibility(isConfirmed ? View.VISIBLE : View.GONE);
        if (btnSaveNotes != null) btnSaveNotes.setVisibility(!isDone ? View.VISIBLE : View.GONE);
    }

    private void updateStatus(String newStatus) {
        if (appointmentId == null || dbRef == null) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> update = new HashMap<>();
        update.put("status", newStatus);

        dbRef.updateChildren(update)
            .addOnSuccessListener(v -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                currentStatus = newStatus;
                updateActionVisibility();
                if ("completed".equals(newStatus) || "declined".equals(newStatus)) finish();
            })
            .addOnFailureListener(ex -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void saveNotes() {
        if (etNotes == null || appointmentId == null || dbRef == null) return;
        String notes = etNotes.getText().toString().trim();
        if (notes.isEmpty()) { Toast.makeText(this, "Please enter notes", Toast.LENGTH_SHORT).show(); return; }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        Map<String, Object> update = new HashMap<>();
        update.put("notes", notes);

        dbRef.updateChildren(update)
            .addOnSuccessListener(v -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Notes saved", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(ex -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void confirmAction(String action, String newStatus) {
        new AlertDialog.Builder(this)
            .setTitle("Confirm " + action)
            .setMessage("Are you sure you want to " + action + " this appointment?")
            .setPositiveButton("Yes", (d, w) -> updateStatus(newStatus))
            .setNegativeButton("No", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null && dbRef != null) {
            dbRef.removeEventListener(listener);
        }
    }
}
