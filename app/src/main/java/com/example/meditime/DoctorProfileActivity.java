package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.meditime.model.Doctor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Locale;

public class DoctorProfileActivity extends AppCompatActivity {

    private TextView tvName, tvSpec, tvEmail, tvPhone, tvHospital, tvRating, tvFee, tvPatients, tvExperience;
    private ImageView imgProfile;
    private Switch switchAvailable;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private String doctorId;
    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        mAuth = FirebaseAuth.getInstance();
        doctorId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (!doctorId.isEmpty()) {
            dbRef = FirebaseDatabase.getInstance().getReference("doctors").child(doctorId);
        }

        tvName       = findViewById(R.id.tvDoctorName);
        tvSpec       = findViewById(R.id.tvSpecialization);
        tvEmail      = findViewById(R.id.tvEmail);
        tvPhone      = findViewById(R.id.tvPhone);
        tvHospital   = findViewById(R.id.tvHospital);
        tvRating     = findViewById(R.id.tvRating);
        tvFee        = findViewById(R.id.tvFee);
        tvPatients   = findViewById(R.id.tvPatients);
        tvExperience = findViewById(R.id.tvExperience);
        imgProfile   = findViewById(R.id.imgProfile);
        switchAvailable = findViewById(R.id.switchAvailable);
        progressBar  = findViewById(R.id.progressBar);

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Button btnEdit = findViewById(R.id.btn_edit_profile);
        if (btnEdit != null) btnEdit.setOnClickListener(v ->
            startActivity(new Intent(this, EditDoctorProfileActivity.class)));

        TextView btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        // Bottom nav
        LinearLayout navHome         = findViewById(R.id.nav_home);
        LinearLayout navAppointments = findViewById(R.id.nav_appointments);
        LinearLayout navPatients     = findViewById(R.id.nav_patients);
        if (navHome != null)         navHome.setOnClickListener(v -> startActivity(new Intent(this, DoctorHomeActivity.class)));
        if (navAppointments != null) navAppointments.setOnClickListener(v -> startActivity(new Intent(this, DoctorAppointmentsActivity.class)));
        if (navPatients != null)     navPatients.setOnClickListener(v -> startActivity(new Intent(this, DoctorPatientsActivity.class)));

        if (switchAvailable != null) {
            switchAvailable.setOnCheckedChangeListener((btn, checked) -> {
                if (!doctorId.isEmpty() && dbRef != null)
                    dbRef.child("available").setValue(checked);
            });
        }

        loadDoctorProfile();
    }

    private void loadDoctorProfile() {
        if (doctorId.isEmpty() || dbRef == null) return;

        listener = dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Doctor d = snapshot.getValue(Doctor.class);
                if (d == null) return;

                if (tvName != null)       tvName.setText("Dr. " + (d.getName() != null ? d.getName() : ""));
                if (tvSpec != null)       tvSpec.setText(d.getSpecialization() != null ? d.getSpecialization() : "");
                if (tvEmail != null)      tvEmail.setText(d.getEmail() != null ? d.getEmail() : "");
                if (tvPhone != null)      tvPhone.setText(d.getPhone() != null ? d.getPhone() : "");
                if (tvHospital != null)   tvHospital.setText(d.getHospital() != null ? d.getHospital() : "");
                if (tvRating != null)     tvRating.setText(String.format(Locale.getDefault(), "%.1f ★", d.getRating()));
                if (tvFee != null)        tvFee.setText("UGX " + String.format(Locale.US, "%,.0f", d.getConsultationFeeUGX()));
                if (tvPatients != null)   tvPatients.setText(String.valueOf(d.getTotalPatients()));
                if (tvExperience != null) tvExperience.setText(d.getExperience() + " years");
                if (switchAvailable != null) switchAvailable.setChecked(d.isAvailable());
                if (imgProfile != null && d.getProfileImageUrl() != null && !d.getProfileImageUrl().isEmpty())
                    Glide.with(DoctorProfileActivity.this).load(d.getProfileImageUrl()).circleCrop().into(imgProfile);
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
