package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnPay = findViewById(R.id.btnPay);
        if (btnPay != null) {
            btnPay.setOnClickListener(v -> {
                startActivity(new Intent(this, BookingConfirmationActivity.class));
            });
        }
    }
}
