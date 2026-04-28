package com.example.meditime;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.database.*;

public class VideoCallActivity extends AppCompatActivity {

    private TextView tvDoctorName, tvCallStatus;
    private ImageView ivMute, ivCamera, ivSpeaker;
    private CardView btnMute, btnEndCall, btnCamera, btnSpeaker;
    private View remoteVideoView;
    private CardView localVideoView;

    private boolean isMuted    = false;
    private boolean isCameraOn = true;
    private boolean isSpeaker  = true;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int elapsedSeconds = 0;
    private AudioManager audioManager;

    private final Runnable timerRunnable = new Runnable() {
        @Override public void run() {
            elapsedSeconds++;
            updateStatusTimer();
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
        }

        // ── Views ──────────────────────────────────────────────────────────
        tvDoctorName   = findViewById(R.id.tvDoctorName);
        tvCallStatus   = findViewById(R.id.tvCallStatus);
        remoteVideoView= findViewById(R.id.remoteVideoView);
        localVideoView = findViewById(R.id.localVideoView);
        btnMute        = findViewById(R.id.btnMute);
        btnEndCall     = findViewById(R.id.btnEndCall);
        btnCamera      = findViewById(R.id.btnCamera);
        btnSpeaker     = findViewById(R.id.btnSpeaker);
        ivMute         = findViewById(R.id.ivMute);
        ivCamera       = findViewById(R.id.ivCamera);
        ivSpeaker      = findViewById(R.id.ivSpeaker);

        String doctorId   = getIntent().getStringExtra("doctorId");
        String doctorName = getIntent().getStringExtra("doctorName");

        if (tvDoctorName != null && doctorName != null && !doctorName.isEmpty())
            tvDoctorName.setText(doctorName.startsWith("Dr.") ? doctorName : "Dr. " + doctorName);

        if (doctorId != null && !doctorId.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("doctors").child(doctorId)
                .child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        if (tvDoctorName != null && name != null)
                            tvDoctorName.setText("Dr. " + name);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }

        // ── Simulate connecting → connected ───────────────────────────────
        if (tvCallStatus != null) tvCallStatus.setText("Connecting…");
        timerHandler.postDelayed(() -> {
            if (tvCallStatus != null) tvCallStatus.setText("Connected · 00:00");
            timerHandler.post(timerRunnable);
        }, 2500);

        // ── Controls ────────────────────────────────────────────────────────
        if (btnMute    != null) btnMute.setOnClickListener(v    -> toggleMute());
        if (btnEndCall != null) btnEndCall.setOnClickListener(v -> endCall());
        if (btnCamera  != null) btnCamera.setOnClickListener(v  -> toggleCamera());
        if (btnSpeaker != null) btnSpeaker.setOnClickListener(v -> toggleSpeaker());
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (audioManager != null) audioManager.setMicrophoneMute(isMuted);
        if (ivMute != null) ivMute.setImageResource(isMuted ? R.drawable.mic_off : R.drawable.mic);
        if (btnMute != null) ((CardView)btnMute).setCardBackgroundColor(isMuted ? 0xFFEF4444 : 0x553D3D5C);
        Toast.makeText(this, isMuted ? "Muted" : "Unmuted", Toast.LENGTH_SHORT).show();
    }

    private void toggleCamera() {
        isCameraOn = !isCameraOn;
        if (localVideoView != null) localVideoView.setAlpha(isCameraOn ? 1f : 0.3f);
        if (ivCamera != null) ivCamera.setImageResource(isCameraOn ? R.drawable.video : R.drawable.video_off);
        if (btnCamera != null) ((CardView)btnCamera).setCardBackgroundColor(isCameraOn ? 0x553D3D5C : 0xFFEF4444);
        Toast.makeText(this, isCameraOn ? "Camera On" : "Camera Off", Toast.LENGTH_SHORT).show();
    }

    private void toggleSpeaker() {
        isSpeaker = !isSpeaker;
        if (audioManager != null) audioManager.setSpeakerphoneOn(isSpeaker);
        if (btnSpeaker != null) ((CardView)btnSpeaker).setCardBackgroundColor(isSpeaker ? 0xFF2A5EE8 : 0x553D3D5C);
        Toast.makeText(this, isSpeaker ? "Speaker On" : "Earpiece" , Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("DefaultLocale")
    private void updateStatusTimer() {
        if (tvCallStatus != null)
            tvCallStatus.setText(String.format("Connected · %02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60));
    }

    private void endCall() {
        timerHandler.removeCallbacksAndMessages(null);
        if (audioManager != null) {
            audioManager.setMicrophoneMute(false);
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
        finish();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        if (audioManager != null) {
            audioManager.setMicrophoneMute(false);
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }
}
