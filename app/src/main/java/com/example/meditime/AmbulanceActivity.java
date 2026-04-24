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

        // SOS Button
        Button btnSOS = findViewById(R.id.btnSOS);
        if (btnSOS != null) {
            btnSOS.setOnLongClickListener(v -> {
                makeCall("999");
                return true;
            });
            btnSOS.setOnClickListener(v -> Toast.makeText(this, "Press and hold to call emergency", Toast.LENGTH_SHORT).show());
        }

        // Call Buttons
        Button btnCallNational = findViewById(R.id.btnCallNational);
        if (btnCallNational != null) btnCallNational.setOnClickListener(v -> makeCall("999"));

        Button btnCallKCCA = findViewById(R.id.btnCallKCCA);
        if (btnCallKCCA != null) btnCallKCCA.setOnClickListener(v -> makeCall("112"));

        Button btnBookAmbulance = findViewById(R.id.btnBookAmbulance);
        if (btnBookAmbulance != null) {
            btnBookAmbulance.setOnClickListener(v -> Toast.makeText(this, "Booking ambulance service...", Toast.LENGTH_SHORT).show());
        }

        // Navigation
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navDoctor = findViewById(R.id.nav_doctor);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        if (navDoctor != null) navDoctor.setOnClickListener(v -> {
            startActivity(new Intent(this, DoctorActivity.class));
            finish();
        });
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void makeCall(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }
}
