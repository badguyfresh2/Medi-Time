package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        LinearLayout backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        CardView cardNotifications = findViewById(R.id.cardNotifications);
        CardView cardPrivacy = findViewById(R.id.cardPrivacy);
        CardView cardEditProfile = findViewById(R.id.cardEditProfile);
        CardView cardDoctorPortal = findViewById(R.id.cardDoctorPortal);

        cardNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        cardPrivacy.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        cardDoctorPortal.setOnClickListener(v -> startActivity(new Intent(this, MedicalRecordsActivity.class)));
        
        // cardEditProfile could link to a new activity or just a Toast for now
        // For linking purposes, let's keep it or link to Settings
        cardEditProfile.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
