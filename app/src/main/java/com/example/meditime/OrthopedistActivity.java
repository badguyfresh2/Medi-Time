package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrthopedistActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orthopedist);

        TextView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnBook = findViewById(R.id.btnBook);
        if (btnBook != null) {
            btnBook.setOnClickListener(v -> startActivity(new Intent(this, AppointmentActivity.class)));
        }
    }
}
