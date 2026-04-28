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
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;

public class VoiceCallActivity extends AppCompatActivity {

    private TextView tvDoctorName, tvCallStatus, tvCallDuration;
    private ImageView ivDoctorAvatar, ivMute, ivSpeaker;
    private CardView btnMute, btnEndCall, btnSpeaker;

    private boolean isMuted    = false;
    private boolean isSpeaker  = false;
    private boolean callActive = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int elapsedSeconds = 0;
    private AudioManager audioManager;

    // Simulate "ringing → connected" after 3 s
    private static final long RING_DELAY_MS = 3000;

    private final Runnable timerRunnable = new Runnable() {
        @Override public void run() {
            elapsedSeconds++;
            if (tvCallDuration != null)
                tvCallDuration.setText(formatTime(elapsedSeconds));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // ── Views ──────────────────────────────────────────────────────────
        tvDoctorName  = findViewById(R.id.tvDoctorName);
        tvCallStatus  = findViewById(R.id.tvCallStatus);
        tvCallDuration= findViewById(R.id.tvCallDuration);
        ivDoctorAvatar= findViewById(R.id.ivDoctorAvatar);
        btnMute       = findViewById(R.id.btnMute);
        btnEndCall    = findViewById(R.id.btnEndCall);
        btnSpeaker    = findViewById(R.id.btnSpeaker);
        ivMute        = findViewById(R.id.ivMute);
        ivSpeaker     = findViewById(R.id.ivSpeaker);

        // ── Get data from intent ───────────────────────────────────────────
        String doctorId   = getIntent().getStringExtra("doctorId");
        String doctorName = getIntent().getStringExtra("doctorName");

        if (tvDoctorName != null && doctorName != null && !doctorName.isEmpty())
            tvDoctorName.setText(doctorName.startsWith("Dr.") ? doctorName : "Dr. " + doctorName);

        // Load doctor avatar if doctorId present
        if (doctorId != null && !doctorId.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("doctors").child(doctorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.child("name").getValue(String.class);
                            if (tvDoctorName != null && name != null)
                                tvDoctorName.setText("Dr. " + name);
                            String img = snapshot.child("profileImageUrl").getValue(String.class);
                            if (ivDoctorAvatar != null && img != null && !img.isEmpty())
                                Glide.with(VoiceCallActivity.this).load(img).circleCrop().into(ivDoctorAvatar);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> endCall());

        // ── Simulate ringing → connected ───────────────────────────────────
        if (tvCallStatus != null) tvCallStatus.setText("Ringing…");
        timerHandler.postDelayed(() -> {
            callActive = true;
            if (tvCallStatus  != null) tvCallStatus.setText("Connected");
            if (tvCallDuration!= null) tvCallDuration.setVisibility(View.VISIBLE);
            timerHandler.post(timerRunnable);
        }, RING_DELAY_MS);

        // ── Controls ────────────────────────────────────────────────────────
        if (btnMute != null) btnMute.setOnClickListener(v -> toggleMute());
        if (btnEndCall != null) btnEndCall.setOnClickListener(v -> endCall());
        if (btnSpeaker != null) btnSpeaker.setOnClickListener(v -> toggleSpeaker());
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (audioManager != null)
            audioManager.setMicrophoneMute(isMuted);
        if (ivMute != null)
            ivMute.setImageResource(isMuted ? R.drawable.mic_off : R.drawable.mic);
        if (btnMute != null)
            ((CardView) btnMute).setCardBackgroundColor(isMuted ? 0xFFEF4444 : 0xFF3D3D5C);
        Toast.makeText(this, isMuted ? "Muted" : "Unmuted", Toast.LENGTH_SHORT).show();
    }

    private void toggleSpeaker() {
        isSpeaker = !isSpeaker;
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(isSpeaker);
            audioManager.setMode(isSpeaker ? AudioManager.MODE_NORMAL : AudioManager.MODE_IN_COMMUNICATION);
        }
        if (ivSpeaker != null)
            ivSpeaker.setImageResource(isSpeaker ? R.drawable.volume : R.drawable.volume);
        if (btnSpeaker != null)
            ((CardView) btnSpeaker).setCardBackgroundColor(isSpeaker ? 0xFF0EA5E9 : 0xFF3D3D5C);
        Toast.makeText(this, isSpeaker ? "Speaker On" : "Speaker Off", Toast.LENGTH_SHORT).show();
    }

    private void endCall() {
        timerHandler.removeCallbacks(timerRunnable);
        if (audioManager != null) {
            audioManager.setMicrophoneMute(false);
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
        finish();
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
    }
}
