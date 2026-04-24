package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.Locale;

public class BookingConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        // ── Pull all appointment details from the Intent ──────────────────
        Intent i          = getIntent();
        String apptId     = i.getStringExtra("appointmentId")  != null ? i.getStringExtra("appointmentId")  : "";
        String doctorName = i.getStringExtra("doctorName")     != null ? i.getStringExtra("doctorName")     : "Doctor";
        String spec       = i.getStringExtra("specialization") != null ? i.getStringExtra("specialization") : "";
        String hospital   = i.getStringExtra("hospital")       != null ? i.getStringExtra("hospital")       : "N/A";
        String date       = i.getStringExtra("date")           != null ? i.getStringExtra("date")           : "";
        String timeSlot   = i.getStringExtra("timeSlot")       != null ? i.getStringExtra("timeSlot")       : "";
        double fee        = i.getDoubleExtra("fee", 0.0);

        // ── Bind to Views ─────────────────────────────────────────────────
        TextView tvDoctorName  = findViewById(R.id.tvConfirmDoctorName);
        TextView tvSpec        = findViewById(R.id.tvConfirmSpec);
        TextView tvDate        = findViewById(R.id.tvConfirmDate);
        TextView tvTime        = findViewById(R.id.tvConfirmTime);
        TextView tvHospital    = findViewById(R.id.tvConfirmHospital);
        TextView tvBookingId   = findViewById(R.id.tvConfirmBookingId);
        TextView tvFee         = findViewById(R.id.tvConfirmFee);
        TextView tvStatus      = findViewById(R.id.tvConfirmStatus);

        if (tvDoctorName != null) tvDoctorName.setText("Dr. " + doctorName);
        if (tvSpec       != null) tvSpec.setText(spec.isEmpty() ? "General" : spec);
        if (tvDate       != null) tvDate.setText(date);
        if (tvTime       != null) tvTime.setText(timeSlot);
        if (tvHospital   != null) tvHospital.setText(hospital.isEmpty() ? "N/A" : hospital);
        if (tvBookingId  != null) tvBookingId.setText("#" + apptId.substring(0, Math.min(apptId.length(), 8)).toUpperCase());
        if (tvFee        != null) tvFee.setText("UGX " + NumberFormat.getNumberInstance(Locale.US).format(fee));
        if (tvStatus     != null) tvStatus.setText("Pending Approval");

        // ── Buttons ───────────────────────────────────────────────────────
        Button btnViewAppointments = findViewById(R.id.btnViewAppointments);
        if (btnViewAppointments != null) {
            btnViewAppointments.setOnClickListener(v ->
                startActivity(new Intent(this, UserAppointmentsActivity.class)));
        }

        Button btnViewReminders = findViewById(R.id.btnViewReminders);
        if (btnViewReminders != null) {
            btnViewReminders.setOnClickListener(v ->
                startActivity(new Intent(this, ReminderActivity.class)));
        }

        Button btnGoHome = findViewById(R.id.btnGoHome);
        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                Intent home = new Intent(this, HomeActivity.class);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(home);
            });
        }
    }
}
