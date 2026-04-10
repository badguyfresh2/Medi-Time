package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class FindDoctorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find_doctor_screen);

        // Service links
        findViewById(R.id.service_hospital).setOnClickListener(v -> startActivity(new Intent(this, HospitalActivity.class)));
        findViewById(R.id.service_pharmacy).setOnClickListener(v -> startActivity(new Intent(this, PharmacyActivity.class)));
        findViewById(R.id.service_diagnostic).setOnClickListener(v -> startActivity(new Intent(this, DiagnosticActivity.class)));
        findViewById(R.id.service_reminder).setOnClickListener(v -> startActivity(new Intent(this, ReminderActivity.class)));
        findViewById(R.id.service_ambulance).setOnClickListener(v -> startActivity(new Intent(this, AmbulanceActivity.class)));
        findViewById(R.id.service_doctor).setOnClickListener(v -> startActivity(new Intent(this, DoctorActivity.class)));

        // Department links
        findViewById(R.id.dept_gastrologist).setOnClickListener(v -> startActivity(new Intent(this, GastrologistActivity.class)));
        findViewById(R.id.dept_neurologist).setOnClickListener(v -> startActivity(new Intent(this, NeurologistActivity.class)));
        findViewById(R.id.dept_cardiologist).setOnClickListener(v -> startActivity(new Intent(this, CardiologistActivity.class)));
        findViewById(R.id.dept_orthopedist).setOnClickListener(v -> startActivity(new Intent(this, OrthopedistActivity.class)));

        // Popular Doctor buttons
        findViewById(R.id.btnBook1).setOnClickListener(v -> startActivity(new Intent(this, DoctorDetailActivity.class)));
        findViewById(R.id.btnBook2).setOnClickListener(v -> startActivity(new Intent(this, DoctorDetailActivity.class)));
        findViewById(R.id.btnBook3).setOnClickListener(v -> startActivity(new Intent(this, DoctorDetailActivity.class)));

        // Navigation
        findViewById(R.id.nav_home).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.nav_user).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.nav_ambulance).setOnClickListener(v -> startActivity(new Intent(this, AmbulanceActivity.class)));
    }
}
