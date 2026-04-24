package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    private boolean isDoctorMode = false;
    private TextView tabPatient, tabDoctor;
    private EditText etEmail, etPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser.getUid());
            return;
        }

        tabPatient  = findViewById(R.id.tab_patient);
        tabDoctor   = findViewById(R.id.tab_doctor);
        etEmail     = findViewById(R.id.et_email);
        etPassword  = findViewById(R.id.et_password);
        progressBar = findViewById(R.id.progressBar);
        Button loginBtn = findViewById(R.id.btn_login);
        TextView signUpLink = findViewById(R.id.tv_signup);

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        tabPatient.setOnClickListener(v -> switchTab(false));
        tabDoctor.setOnClickListener(v -> switchTab(true));

        loginBtn.setOnClickListener(v -> loginUser());

        signUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            intent.putExtra("isDoctor", isDoctorMode);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> checkUserRoleAndRedirect(result.getUser().getUid()))
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkUserRoleAndRedirect(String uid) {
        dbRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if ("doctor".equals(role)) {
                        startActivity(new Intent(LoginActivity.this, DoctorHomeActivity.class));
                    } else {
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    }
                } else {
                    dbRef.child("doctors").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot doc) {
                            startActivity(new Intent(LoginActivity.this,
                                    doc.exists() ? DoctorHomeActivity.class : HomeActivity.class));
                            finish();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { finish(); }
                    });
                }
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }

    private void switchTab(boolean doctor) {
        isDoctorMode = doctor;
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