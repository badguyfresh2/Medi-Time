package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class BookingConfirmationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        Button btnGoHome = findViewById(R.id.btnGoHome);
        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        Button btnViewAppointments = findViewById(R.id.btnViewAppointments);
        if (btnViewAppointments != null) {
            btnViewAppointments.setOnClickListener(v -> {
                startActivity(new Intent(this, AppointmentActivity.class));
            });
        }
    }
}
