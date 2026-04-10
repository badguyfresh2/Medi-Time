package com.example.meditime;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AmbulanceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance);

        // Bottom Nav
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navDoctor = findViewById(R.id.nav_doctor);
        LinearLayout navAmbulance = findViewById(R.id.nav_ambulance);
        LinearLayout navProfile = findViewById(R.id.nav_profile);


        // Bottom Nav Click Listeners
        navHome.setOnClickListener(v -> { /* Already on Home */ });
        navDoctor.setOnClickListener(v -> startActivity(new Intent(AmbulanceActivity.this, DoctorActivity.class)));
        navAmbulance.setOnClickListener(v -> startActivity(new Intent(AmbulanceActivity.this, AmbulanceActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(AmbulanceActivity.this, ProfileActivity.class)));


        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());


        Button btnSOS = findViewById(R.id.btnSOS);
        btnSOS.setOnClickListener(v -> {
            Toast.makeText(this, "Emergency SOS Signal Sent!", Toast.LENGTH_LONG).show();
        });


        Button btnCallNational = findViewById(R.id.btnCallNational);
        btnCallNational.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:999"));
            startActivity(intent);
        });


        Button btnCallKCCA = findViewById(R.id.btnCallKCCA);
        btnCallKCCA.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:112"));
            startActivity(intent);
        });
    }
}
