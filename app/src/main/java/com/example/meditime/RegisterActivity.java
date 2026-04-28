package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.meditime.model.Doctor;
import com.example.meditime.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private boolean isDoctorMode = false;
    private TextView tabPatient, tabDoctor;
    private LinearLayout doctorFields;
    private EditText etName, etPhone, etEmail, etPass, etConfirm, etSpecialization, etHospital, etFee;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        tabPatient      = findViewById(R.id.tab_patient);
        tabDoctor       = findViewById(R.id.tab_doctor);
        doctorFields    = findViewById(R.id.doctor_fields);
        etName          = findViewById(R.id.et_name);
        etPhone         = findViewById(R.id.et_phone);
        etEmail         = findViewById(R.id.et_email);
        etPass          = findViewById(R.id.et_password);
        etConfirm       = findViewById(R.id.et_confirm_password);
        etSpecialization= findViewById(R.id.et_specialization);
        etHospital      = findViewById(R.id.et_hospital);
        etFee           = findViewById(R.id.et_fee);
        progressBar     = findViewById(R.id.progressBar);
        Button btnSignup= findViewById(R.id.btn_signup);
        TextView tvLogin= findViewById(R.id.tv_login);

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        isDoctorMode = getIntent().getBooleanExtra("isDoctor", false);
        if (isDoctorMode) switchTab(true);

        tabPatient.setOnClickListener(v -> switchTab(false));
        tabDoctor.setOnClickListener(v  -> switchTab(true));
        btnSignup.setOnClickListener(v  -> registerUser());
        tvLogin.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void registerUser() {
        String name    = etName.getText().toString().trim();
        String phone   = etPhone.getText().toString().trim();
        String email   = etEmail.getText().toString().trim();
        String pass    = etPass.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    if (isDoctorMode) saveDoctor(uid, name, phone, email);
                    else savePatient(uid, name, phone, email);
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void savePatient(String uid, String name, String phone, String email) {
        User user = new User();
        user.setUserId(uid);
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole("patient");

        dbRef.child("users").child(uid).setValue(user)
                .addOnSuccessListener(v -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveDoctor(String uid, String name, String phone, String email) {
        String spec    = etSpecialization != null ? etSpecialization.getText().toString().trim() : "";
        String hospital= etHospital != null ? etHospital.getText().toString().trim() : "";
        String feeStr  = etFee != null ? etFee.getText().toString().trim() : "0";
        double fee     = feeStr.isEmpty() ? 0 : Double.parseDouble(feeStr);

        Doctor doctor  = new Doctor();
        doctor.setUserId(uid);
        doctor.setName(name);
        doctor.setPhone(phone);
        doctor.setEmail(email);
        doctor.setSpecialization(spec);
        doctor.setHospital(hospital);
        doctor.setConsultationFeeUGX(fee);
        doctor.setRating(0);
        doctor.setAvailable(true);
        doctor.setRole("doctor");

        User userEntry = new User();
        userEntry.setUserId(uid);
        userEntry.setName(name);
        userEntry.setPhone(phone);
        userEntry.setEmail(email);
        userEntry.setRole("doctor");

        dbRef.child("users").child(uid).setValue(userEntry);
        dbRef.child("doctors").child(uid).setValue(doctor)
                .addOnSuccessListener(v -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Doctor account created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, DoctorHomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void switchTab(boolean doctor) {
        isDoctorMode = doctor;
        if (doctorFields != null) doctorFields.setVisibility(doctor ? View.VISIBLE : View.GONE);
        if (doctor) {
            tabDoctor.setBackgroundResource(R.drawable.tab_selected);
            tabDoctor.setTextColor(0xFFFFFFFF);
            tabPatient.setBackgroundResource(android.R.color.transparent);
            tabPatient.setTextColor(0xFF9CA3AF);
        } else {
            tabPatient.setBackgroundResource(R.drawable.tab_selected);
            tabPatient.setTextColor(0xFFFFFFFF);
            tabDoctor.setBackgroundResource(android.R.color.transparent);
            tabDoctor.setTextColor(0xFF9CA3AF);
        }
    }
}