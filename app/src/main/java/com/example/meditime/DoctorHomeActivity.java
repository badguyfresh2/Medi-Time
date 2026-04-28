package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meditime.model.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Doctor home screen.
 *
 * Changes from original:
 *  • "Add Diagnostics" quick-action now routes to DoctorDiagnosticActivity.
 *  • "Patient Records" quick-action now routes to DoctorMedicalRecordsActivity
 *    (will prompt for patient selection if no patientId passed).
 *  • Appointment row "View Notes" still goes to ViewNotesActivity.
 *  • Appointment row "Prescriptions" still goes to DoctorPrescriptionsActivity.
 *  • New appointment row "Diagnostics" shortcut per patient goes to DoctorDiagnosticActivity.
 */
public class DoctorHomeActivity extends AppCompatActivity {

    private TextView tvDoctorName, tvTodayCount, tvPendingCount, tvTotalEarnings;
    private ImageView imgDoctorAvatar;
    private RecyclerView rvTodayAppointments;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private String doctorId;
    private ValueEventListener todayListener, statsListener;
    private final List<Appointment> todayList = new ArrayList<>();
    private DoctorAppointmentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home);

        dbRef    = FirebaseDatabase.getInstance().getReference();
        mAuth    = FirebaseAuth.getInstance();
        doctorId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        tvDoctorName    = findViewById(R.id.tv_doctor_name);
        tvTodayCount    = findViewById(R.id.tv_today_count);
        tvPendingCount  = findViewById(R.id.tv_pending_count);
        tvTotalEarnings = findViewById(R.id.tv_total_earnings);
        imgDoctorAvatar = findViewById(R.id.img_doctor_avatar);
        rvTodayAppointments = findViewById(R.id.rv_today_appointments);
        progressBar     = findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        // Appointment adapter:
        //   - "View Notes"     → ViewNotesActivity
        //   - "Prescriptions"  → DoctorPrescriptionsActivity
        adapter = new DoctorAppointmentAdapter(
            todayList,
            appointment -> {
                // View / manage appointment notes
                Intent intent = new Intent(this, ViewNotesActivity.class);
                intent.putExtra("appointmentId", appointment.getAppointmentId());
                startActivity(intent);
            },
            appointment -> {
                // Prescriptions shortcut
                Intent intent = new Intent(this, DoctorPrescriptionsActivity.class);
                intent.putExtra("appointmentId", appointment.getAppointmentId());
                intent.putExtra("patientId",     appointment.getPatientId());
                intent.putExtra("patientName",   appointment.getPatientName());
                startActivity(intent);
            }
        );

        if (rvTodayAppointments != null) {
            rvTodayAppointments.setLayoutManager(new LinearLayoutManager(this));
            rvTodayAppointments.setAdapter(adapter);
        }

        // ── Quick action tiles ────────────────────────────────────────────────
        LinearLayout btnAppointments = findViewById(R.id.btn_my_appointments);
        LinearLayout btnPatients     = findViewById(R.id.btn_my_patients);
        LinearLayout btnPrescDr      = findViewById(R.id.btn_prescriptions_dr);
        LinearLayout btnSchedule     = findViewById(R.id.btn_schedule);
        // NEW tiles:
        LinearLayout btnDiagnostics  = findViewById(R.id.btn_diagnostics);
        LinearLayout btnRecords      = findViewById(R.id.btn_records);

        if (btnAppointments != null) btnAppointments.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorAppointmentsActivity.class)));
        if (btnPatients != null)     btnPatients.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorPatientsActivity.class)));
        if (btnPrescDr != null)      btnPrescDr.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorPrescriptionsActivity.class)));
        if (btnSchedule != null)     btnSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorScheduleActivity.class)));

        // ── NEW: Diagnostics tile → DoctorDiagnosticActivity (no preset patient) ──
        if (btnDiagnostics != null)  btnDiagnostics.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorDiagnosticActivity.class)));

        // ── NEW: Records tile → DoctorPatientsActivity so doctor picks a patient ──
        if (btnRecords != null)      btnRecords.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorPatientsActivity.class)));

        TextView tvSeeAll = findViewById(R.id.tv_see_all);
        if (tvSeeAll != null) tvSeeAll.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorAppointmentsActivity.class)));

        // ── Bottom navigation ─────────────────────────────────────────────────
        LinearLayout navHome         = findViewById(R.id.nav_home);
        LinearLayout navAppointments = findViewById(R.id.nav_appointments);
        LinearLayout navPatients     = findViewById(R.id.nav_patients);
        LinearLayout navProfile      = findViewById(R.id.nav_profile_dr);

        if (navHome         != null) navHome.setOnClickListener(v -> {/* already here */});
        if (navAppointments != null) navAppointments.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorAppointmentsActivity.class)));
        if (navPatients     != null) navPatients.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorPatientsActivity.class)));
        if (navProfile      != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorProfileActivity.class)));

        loadDoctorProfile();
        loadTodayAppointments();
        loadStats();
    }

    // ── Firebase loaders ───────────────────────────────────────────────────────

    private void loadDoctorProfile() {
        if (doctorId.isEmpty()) return;
        dbRef.child("doctors").child(doctorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String name = snapshot.child("name").getValue(String.class);
                String img  = snapshot.child("profileImageUrl").getValue(String.class);
                if (tvDoctorName    != null) tvDoctorName.setText("Dr. " + (name != null ? name : ""));
                if (imgDoctorAvatar != null && img != null && !img.isEmpty())
                    Glide.with(DoctorHomeActivity.this).load(img).circleCrop().into(imgDoctorAvatar);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTodayAppointments() {
        if (doctorId.isEmpty()) return;
        String today = new SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(new Date());

        Query query = dbRef.child("appointments").orderByChild("doctorId").equalTo(doctorId);
        todayListener = query.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                todayList.clear();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Appointment a = doc.getValue(Appointment.class);
                    if (a != null && today.equals(a.getDate())) {
                        a.setAppointmentId(doc.getKey());
                        todayList.add(a);
                    }
                }
                if (adapter      != null) adapter.notifyDataSetChanged();
                if (tvTodayCount != null) tvTodayCount.setText(String.valueOf(todayList.size()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadStats() {
        if (doctorId.isEmpty()) return;
        Query query = dbRef.child("appointments").orderByChild("doctorId").equalTo(doctorId);
        statsListener = query.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                int pending = 0; double earnings = 0;
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Appointment a = doc.getValue(Appointment.class);
                    if (a != null) {
                        if ("pending".equals(a.getStatus())) pending++;
                        if ("confirmed".equals(a.getStatus()) || "completed".equals(a.getStatus()))
                            earnings += a.getConsultationFeeUGX();
                    }
                }
                if (tvPendingCount  != null) tvPendingCount.setText(String.valueOf(pending));
                if (tvTotalEarnings != null)
                    tvTotalEarnings.setText("UGX " + String.format(Locale.US, "%,.0f", earnings));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        DatabaseReference apptRef = dbRef.child("appointments");
        if (todayListener != null) apptRef.removeEventListener(todayListener);
        if (statsListener != null) apptRef.removeEventListener(statsListener);
    }
}
