package com.example.meditime;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoCallActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        ImageView btnEndCall = findViewById(R.id.btnEndCall);
        if (btnEndCall != null) btnEndCall.setOnClickListener(v -> finish());
    }
}
