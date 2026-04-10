package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class DoctorDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_detail);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnBookAppointment = findViewById(R.id.btnBookAppointment);
        if (btnBookAppointment != null) {
            btnBookAppointment.setOnClickListener(v -> {
                startActivity(new Intent(this, AppointmentActivity.class));
            });
        }
        
        // Link Contact buttons
        LinearLayout videoCall = findViewById(R.id.btnVideoCall);
                videoCall.setOnClickListener(v ->
                        startActivity(new Intent(this, VideoCallActivity.class))
                );

                LinearLayout voiceCall = findViewById(R.id.btnVoiceCall);
        voiceCall.setOnClickListener(v ->
                startActivity(new Intent(this, VideoCallActivity.class))
        );

        LinearLayout message =findViewById(R.id.btnMessage);
                message.setOnClickListener(v ->
                        startActivity(new Intent(this, ChatActivity.class))
                );
    }
}
