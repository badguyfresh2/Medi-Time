package com.example.meditime;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class VoiceCallActivity extends AppCompatActivity {

    private TextView tvCallStatus, tvCallDuration;
    private ImageView ivMute, ivSpeaker;
    private CardView btnMute, btnSpeaker, btnEndCall;

    private boolean isMuted = false;
    private boolean isSpeakerOn = false;

    private int seconds = 0;
    private final Handler handler = new Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        initViews();
        setupClickListeners();

        // Simulate call connection
        tvCallStatus.setText("Calling...");
        handler.postDelayed(() -> {
            tvCallStatus.setText("Connected");
            tvCallDuration.setVisibility(View.VISIBLE);
            startTimer();
        }, 2500);
    }

    private void initViews() {
        tvCallStatus = findViewById(R.id.tvCallStatus);
        tvCallDuration = findViewById(R.id.tvCallDuration);
        ivMute = findViewById(R.id.ivMute);
        ivSpeaker = findViewById(R.id.ivSpeaker);
        btnMute = findViewById(R.id.btnMute);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnEndCall = findViewById(R.id.btnEndCall);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            updateToggleStyle(btnMute, ivMute, isMuted);
            Toast.makeText(this, isMuted ? "Muted" : "Unmuted", Toast.LENGTH_SHORT).show();
        });

        btnSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            updateToggleStyle(btnSpeaker, ivSpeaker, isSpeakerOn);
            Toast.makeText(this, isSpeakerOn ? "Speaker On" : "Speaker Off", Toast.LENGTH_SHORT).show();
        });

        btnEndCall.setOnClickListener(v -> finish());
    }

    private void updateToggleStyle(CardView card, ImageView icon, boolean isActive) {
        if (isActive) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            icon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark));
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            // Setting a fallback color if transparent doesn't look right on CardView
            card.setCardBackgroundColor(0x3D3D5C);
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                int mins = seconds / 60;
                int secs = seconds % 60;
                tvCallDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }
}
