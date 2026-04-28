package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.meditime.model.Doctor;
import com.google.firebase.database.*;

import java.text.NumberFormat;
import java.util.Locale;

public class DoctorDetailActivity extends AppCompatActivity {

    private TextView tvName, tvSpec, tvRating, tvFee, tvPatients, tvExperience, tvHospital, tvBio, tvAvailability;
    private ImageView imgDoctor;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private ValueEventListener listener;
    private String doctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_detail);

        doctorId = getIntent().getStringExtra("doctorId");
        if (doctorId != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("doctors").child(doctorId);
        }

        tvName         = findViewById(R.id.tvDoctorName);
        tvSpec         = findViewById(R.id.tvSpecialization);
        tvRating       = findViewById(R.id.tvRating);
        tvFee          = findViewById(R.id.tvFee);
        tvPatients     = findViewById(R.id.tvPatients);
        tvExperience   = findViewById(R.id.tvExperience);
        tvHospital     = findViewById(R.id.tvHospital);
        tvBio          = findViewById(R.id.tvBio);
        tvAvailability = findViewById(R.id.tvAvailability);
        imgDoctor      = findViewById(R.id.imgDoctor);
        progressBar    = findViewById(R.id.progressBar);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnBook = findViewById(R.id.btnBookAppointment);
        if (btnBook != null) {
            btnBook.setOnClickListener(v -> {
                Intent intent = new Intent(this, AppointmentActivity.class);
                intent.putExtra("doctorId", doctorId);
                startActivity(intent);
            });
        }

        LinearLayout btnChat = findViewById(R.id.btnMessage);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("doctorId", doctorId);
                startActivity(intent);
            });
        }

        LinearLayout btnVideo = findViewById(R.id.btnVideoCall);
        if (btnVideo != null) btnVideo.setOnClickListener(v -> startActivity(new Intent(this, VideoCallActivity.class)));

        LinearLayout btnVoice = findViewById(R.id.btnVoiceCall);
        if (btnVoice != null) btnVoice.setOnClickListener(v -> startActivity(new Intent(this, VoiceCallActivity.class)));

        if (doctorId != null) loadDoctorData();
    }

    private void loadDoctorData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        listener = dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Doctor doctor = snapshot.getValue(Doctor.class);
                if (doctor == null) return;

                if (tvName != null)       tvName.setText("Dr. " + (doctor.getName() != null ? doctor.getName() : ""));
                if (tvSpec != null)       tvSpec.setText(doctor.getSpecialization() != null ? doctor.getSpecialization() : "");
                if (tvRating != null)     tvRating.setText(String.format(Locale.getDefault(), "%.1f ★", doctor.getRating()));
                if (tvFee != null)        tvFee.setText("UGX " + NumberFormat.getNumberInstance(Locale.US).format(doctor.getConsultationFeeUGX()));
                if (tvPatients != null)   tvPatients.setText(String.valueOf(doctor.getTotalPatients()));
                if (tvExperience != null) tvExperience.setText(doctor.getExperience() + " yrs");
                if (tvHospital != null)   tvHospital.setText(doctor.getHospital() != null ? doctor.getHospital() : "");
                if (tvBio != null)        tvBio.setText(doctor.getBio() != null ? doctor.getBio() : "");
                if (tvAvailability != null) {
                    tvAvailability.setText(doctor.isAvailable() ? "Available Now" : "Unavailable");
                    tvAvailability.setTextColor(doctor.isAvailable() ? 0xFF059669 : 0xFFEF4444);
                }
                if (imgDoctor != null && doctor.getProfileImageUrl() != null && !doctor.getProfileImageUrl().isEmpty()) {
                    Glide.with(DoctorDetailActivity.this).load(doctor.getProfileImageUrl()).circleCrop().into(imgDoctor);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null && dbRef != null) {
            dbRef.removeEventListener(listener);
        }
    }
}
