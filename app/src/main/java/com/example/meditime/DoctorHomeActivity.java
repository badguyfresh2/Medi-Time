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
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

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

        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        doctorId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        tvDoctorName   = findViewById(R.id.tv_doctor_name);
        tvTodayCount   = findViewById(R.id.tv_today_count);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        tvTotalEarnings= findViewById(R.id.tv_total_earnings);
        imgDoctorAvatar= findViewById(R.id.img_doctor_avatar);
        rvTodayAppointments = findViewById(R.id.rv_today_appointments);
        progressBar    = findViewById(R.id.progressBar);

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        adapter = new DoctorAppointmentAdapter(todayList, appointment -> {
            Intent intent = new Intent(this, ViewNotesActivity.class);
            intent.putExtra("appointmentId", appointment.getAppointmentId());
            startActivity(intent);
        }, appointment -> {
            Intent intent = new Intent(this, DoctorPrescriptionsActivity.class);
            intent.putExtra("appointmentId", appointment.getAppointmentId());
            intent.putExtra("patientId", appointment.getPatientId());
            intent.putExtra("patientName", appointment.getPatientName());
            startActivity(intent);
        });

        if (rvTodayAppointments != null) {
            rvTodayAppointments.setLayoutManager(new LinearLayoutManager(this));
            rvTodayAppointments.setAdapter(adapter);
        }

        LinearLayout btnAppointments = findViewById(R.id.btn_my_appointments);
        LinearLayout btnPatients     = findViewById(R.id.btn_my_patients);
        LinearLayout btnPrescDr      = findViewById(R.id.btn_prescriptions_dr);
        LinearLayout btnSchedule     = findViewById(R.id.btn_schedule);
        if (btnAppointments != null) btnAppointments.setOnClickListener(v -> startActivity(new Intent(this, DoctorAppointmentsActivity.class)));
        if (btnPatients != null)     btnPatients.setOnClickListener(v -> startActivity(new Intent(this, DoctorPatientsActivity.class)));
        if (btnPrescDr != null)      btnPrescDr.setOnClickListener(v -> startActivity(new Intent(this, DoctorPrescriptionsActivity.class)));
        if (btnSchedule != null)     btnSchedule.setOnClickListener(v -> startActivity(new Intent(this, DoctorScheduleActivity.class)));

        TextView tvSeeAll = findViewById(R.id.tv_see_all);
        if (tvSeeAll != null) tvSeeAll.setOnClickListener(v -> startActivity(new Intent(this, DoctorAppointmentsActivity.class)));

        LinearLayout navHome         = findViewById(R.id.nav_home);
        LinearLayout navAppointments = findViewById(R.id.nav_appointments);
        LinearLayout navPatients     = findViewById(R.id.nav_patients);
        LinearLayout navProfile      = findViewById(R.id.nav_profile_dr);
        if (navHome != null)         navHome.setOnClickListener(v -> {});
        if (navAppointments != null) navAppointments.setOnClickListener(v -> startActivity(new Intent(this, DoctorAppointmentsActivity.class)));
        if (navPatients != null)     navPatients.setOnClickListener(v -> startActivity(new Intent(this, DoctorPatientsActivity.class)));
        if (navProfile != null)      navProfile.setOnClickListener(v -> startActivity(new Intent(this, DoctorProfileActivity.class)));

        loadDoctorProfile();
        loadTodayAppointments();
        loadStats();
    }

    private void loadDoctorProfile() {
        if (doctorId.isEmpty()) return;
        dbRef.child("doctors").child(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {
                if (!ds.exists()) return;
                String name = ds.child("name").getValue(String.class);
                String img  = ds.child("profileImageUrl").getValue(String.class);
                if (tvDoctorName != null)
                    tvDoctorName.setText(getString(R.string.dr_prefix, (name != null ? name : "")));
                if (imgDoctorAvatar != null && img != null && !img.isEmpty())
                    Glide.with(DoctorHomeActivity.this).load(new File(img)).circleCrop().into(imgDoctorAvatar);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTodayAppointments() {
        if (doctorId.isEmpty()) return;
        String today = new SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(new Date());

        todayListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todayList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment a = child.getValue(Appointment.class);
                    if (a != null && doctorId.equals(a.getDoctorId()) && today.equals(a.getDate())) {
                        a.setAppointmentId(child.getKey());
                        todayList.add(a);
                    }
                }
                adapter.notifyDataSetChanged();
                if (tvTodayCount != null) tvTodayCount.setText(String.valueOf(todayList.size()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        dbRef.child("appointments").addValueEventListener(todayListener);
    }

    private void loadStats() {
        if (doctorId.isEmpty()) return;

        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int pending = 0; double earnings = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment a = child.getValue(Appointment.class);
                    if (a == null || !doctorId.equals(a.getDoctorId())) continue;
                    String status = a.getStatus();
                    if ("pending".equals(status)) pending++;
                    if ("confirmed".equals(status) || "completed".equals(status)) {
                        earnings += a.getConsultationFeeUGX();
                    }
                }
                if (tvPendingCount != null)  tvPendingCount.setText(String.valueOf(pending));
                if (tvTotalEarnings != null)
                    tvTotalEarnings.setText(getString(R.string.ugx_prefix, String.format(Locale.US, "%,.0f", earnings)));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        dbRef.child("appointments").addValueEventListener(statsListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (todayListener != null) dbRef.child("appointments").removeEventListener(todayListener);
        if (statsListener != null) dbRef.child("appointments").removeEventListener(statsListener);
    }
}