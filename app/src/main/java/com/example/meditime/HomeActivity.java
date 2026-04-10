package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);

        //profile
        ImageView profile_image = findViewById(R.id.profile_image);
        profile_image.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        });

        // Services
        LinearLayout serviceDoctor = findViewById(R.id.service_doctor);
        LinearLayout serviceHospital = findViewById(R.id.service_hospital);
        LinearLayout servicePharmacy = findViewById(R.id.service_pharmacy);
        LinearLayout serviceAmbulance = findViewById(R.id.service_ambulance);
        LinearLayout serviceDiagnostic = findViewById(R.id.service_diagnostic);
        LinearLayout serviceReminder = findViewById(R.id.service_reminder);
        LinearLayout serviceChat = findViewById(R.id.service_chat);
        LinearLayout serviceRecords = findViewById(R.id.service_records);

        // Bottom Nav
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navDoctor = findViewById(R.id.nav_doctor);
        LinearLayout navAmbulance = findViewById(R.id.nav_ambulance);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        // Service Click Listeners
        serviceDoctor.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DoctorActivity.class)));
        serviceHospital.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, HospitalActivity.class)));
        servicePharmacy.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PharmacyActivity.class)));
        serviceAmbulance.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AmbulanceActivity.class)));
        serviceDiagnostic.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DiagnosticActivity.class)));
        serviceReminder.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ReminderActivity.class)));
        serviceChat.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ChatActivity.class)));
        serviceRecords.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, MedicalRecordsActivity.class)));

        // Bottom Nav Click Listeners
        navHome.setOnClickListener(v -> { /* Already on Home */ });
        navDoctor.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DoctorActivity.class)));
        navAmbulance.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AmbulanceActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));

        // Other buttons
        TextView btnOrderNow = findViewById(R.id.btnOrderNow);
        btnOrderNow.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PharmacyActivity.class)));

        CardView btnBook1 = findViewById(R.id.btnBook1);
        CardView btnBook2 = findViewById(R.id.btnBook2);
        
        btnBook1.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DoctorDetailActivity.class)));
        btnBook2.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DoctorDetailActivity.class)));
    }
}
