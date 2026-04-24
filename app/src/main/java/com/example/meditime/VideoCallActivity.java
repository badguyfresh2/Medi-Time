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

public class VideoCallActivity extends AppCompatActivity {

    private TextView tvCallStatus;
    private CardView btnMute, btnCamera, btnSpeaker, btnEndCall;
    private ImageView ivMute, ivCamera, ivSpeaker;

    private boolean isMuted = false;
    private boolean isCameraOff = false;
    private boolean isSpeakerOn = true;

    private int seconds = 0; // Start at 04:23 as per layout
    private final Handler handler = new Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        initViews();
        setupClickListeners();
        startTimer();
    }

    private void initViews() {
        tvCallStatus = findViewById(R.id.tvCallStatus);
        btnMute = findViewById(R.id.btnMute);
        btnCamera = findViewById(R.id.btnCamera);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnEndCall = findViewById(R.id.btnEndCall);

        ivMute = findViewById(R.id.ivMute);
        ivCamera = findViewById(R.id.ivCamera);
        ivSpeaker = findViewById(R.id.ivSpeaker);
    }

    private void setupClickListeners() {
        btnEndCall.setOnClickListener(v -> finish());

        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            updateToggleStyle(btnMute, ivMute, isMuted);
            Toast.makeText(this, isMuted ? "Muted" : "Unmuted", Toast.LENGTH_SHORT).show();
        });

        btnCamera.setOnClickListener(v -> {
            isCameraOff = !isCameraOff;
            updateToggleStyle(btnCamera, ivCamera, isCameraOff);
            View localVideo = findViewById(R.id.localVideoView);
            if (localVideo != null) {
                localVideo.setVisibility(isCameraOff ? View.GONE : View.VISIBLE);
            }
            Toast.makeText(this, isCameraOff ? "Camera Off" : "Camera On", Toast.LENGTH_SHORT).show();
        });

        btnSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            if (isSpeakerOn) {
                btnSpeaker.setCardBackgroundColor(0xFF2A5EE8); // Active blue
                ivSpeaker.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
            } else {
                btnSpeaker.setCardBackgroundColor(0x3D3D5C); // Inactive gray
                ivSpeaker.setColorFilter(0x80FFFFFF);
            }
            Toast.makeText(this, isSpeakerOn ? "Speaker On" : "Speaker Off", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateToggleStyle(CardView card, ImageView icon, boolean isActive) {
        if (isActive) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
            icon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark));
        } else {
            card.setCardBackgroundColor(0x00000000); // Transparent
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
                String timeStr = String.format(Locale.getDefault(), "Connected · %02d:%02d", mins, secs);
                tvCallStatus.setText(timeStr);
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
