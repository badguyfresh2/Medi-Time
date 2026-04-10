package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DoctorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());


        // Bottom Nav
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navDoctor = findViewById(R.id.nav_doctor);
        LinearLayout navAmbulance = findViewById(R.id.nav_ambulance);
        LinearLayout navProfile = findViewById(R.id.nav_profile);


        // Bottom Nav Click Listeners
        navHome.setOnClickListener(v -> { /* Already on Home */ });
        navDoctor.setOnClickListener(v -> startActivity(new Intent(DoctorActivity.this, DoctorActivity.class)));
        navAmbulance.setOnClickListener(v -> startActivity(new Intent(DoctorActivity.this, AmbulanceActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(DoctorActivity.this, ProfileActivity.class)));


        // Specialty chips linking to specific activities
        TextView chipCardiology = findViewById(R.id.chipCardiology);
        TextView chipGeneral = findViewById(R.id.chipGeneral);
        TextView chipPediatrics = findViewById(R.id.chipPediatrics);
        TextView chipDermatology = findViewById(R.id.chipDermatology);
        TextView chipDentistry = findViewById(R.id.chipDentistry);

        chipCardiology.setOnClickListener(v -> startActivity(new Intent(this, CardiologistActivity.class)));
        // Others can be linked similarly if activities exist

        // Doctor Booking Buttons
        Button btnBookDoctor1 = findViewById(R.id.btnBookDoctor1);
        Button btnBookDoctor2 = findViewById(R.id.btnBookDoctor2);
        Button btnBookDoctor3 = findViewById(R.id.btnBookDoctor3);

        btnBookDoctor1.setOnClickListener(v -> startActivity(new Intent(this, DoctorDetailActivity.class)));
        btnBookDoctor2.setOnClickListener(v -> startActivity(new Intent(this, DoctorDetailActivity.class)));
        btnBookDoctor3.setOnClickListener(v -> startActivity(new Intent(this, DoctorDetailActivity.class)));
    }
}
