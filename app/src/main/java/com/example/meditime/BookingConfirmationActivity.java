package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Booking Confirmation screen — loads the appointment freshly from
 * Realtime Database to show the most up-to-date, accurate details.
 */
public class BookingConfirmationActivity extends AppCompatActivity {

    private TextView tvDoctorName, tvSpec, tvDate, tvTime, tvHospital;
    private TextView tvBookingId, tvConsultFee, tvTotal, tvPayMethod, tvStatus, tvPatientName;
    private TextView tvReminderNotice;
    private ProgressBar progressBar;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        db = FirebaseDatabase.getInstance().getReference();

        // ── Bind views ────────────────────────────────────────────────────
        tvDoctorName   = findViewById(R.id.tvConfirmDoctorName);
        tvSpec         = findViewById(R.id.tvConfirmSpec);
        tvDate         = findViewById(R.id.tvConfirmDate);
        tvTime         = findViewById(R.id.tvConfirmTime);
        tvHospital     = findViewById(R.id.tvConfirmHospital);
        tvBookingId    = findViewById(R.id.tvConfirmBookingId);
        tvConsultFee   = findViewById(R.id.tvConfirmFee);
        tvTotal        = findViewById(R.id.tvConfirmTotal);
        tvPayMethod    = findViewById(R.id.tvConfirmPayMethod);
        tvStatus       = findViewById(R.id.tvConfirmStatus);
        tvPatientName  = findViewById(R.id.tvConfirmPatientName);
        tvReminderNotice = findViewById(R.id.tvReminderNotice);
        progressBar    = findViewById(R.id.progressBar);

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // ── Pull data from intent (instant display) ───────────────────────
        Intent i           = getIntent();
        String appointmentId = nvl(i.getStringExtra("appointmentId"));
        String doctorName    = nvl(i.getStringExtra("doctorName"), "Doctor");
        String spec          = nvl(i.getStringExtra("specialization"), "General");
        String hospital      = nvl(i.getStringExtra("hospital"), "N/A");
        String date          = nvl(i.getStringExtra("date"));
        String timeSlot      = nvl(i.getStringExtra("timeSlot"));
        String patientName   = nvl(i.getStringExtra("patientName"), "Patient");
        String payMethod     = nvl(i.getStringExtra("paymentMethod"), "—");
        double fee           = i.getDoubleExtra("fee", 0.0);
        double total         = i.getDoubleExtra("total", fee);

        // Show immediately from intent
        populateUI(appointmentId, doctorName, spec, hospital, date, timeSlot, patientName, payMethod, fee, total, "pending");

        // ── Then refresh from Realtime Database for accuracy ──────────────
        if (!appointmentId.isEmpty()) {
            db.child("appointments").child(appointmentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snap) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (!snap.exists()) return;

                        String dbDoctor  = nvl(snap.child("doctorName").getValue(String.class),     doctorName);
                        String dbSpec    = nvl(snap.child("specialization").getValue(String.class), spec);
                        String dbHosp    = nvl(snap.child("hospital").getValue(String.class),       hospital);
                        String dbDate    = nvl(snap.child("date").getValue(String.class),           date);
                        String dbTime    = nvl(snap.child("timeSlot").getValue(String.class),       timeSlot);
                        String dbPat     = nvl(snap.child("patientName").getValue(String.class),    patientName);
                        String dbMethod  = nvl(snap.child("paymentMethod").getValue(String.class),  payMethod);
                        String dbStatus  = nvl(snap.child("status").getValue(String.class),         "pending");
                        Double dbFee     = snap.child("consultationFeeUGX").getValue(Double.class);
                        Double dbTotal   = snap.child("amountPaid").getValue(Double.class);

                        populateUI(appointmentId, dbDoctor, dbSpec, dbHosp, dbDate, dbTime,
                                   dbPat, dbMethod,
                                   dbFee   != null ? dbFee   : fee,
                                   dbTotal != null ? dbTotal : total,
                                   dbStatus);
                    }
                    @Override public void onCancelled(DatabaseError e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
        } else {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }

        // ── Action buttons ────────────────────────────────────────────────
        Button btnViewAppointments = findViewById(R.id.btnViewAppointments);
        Button btnViewReminders    = findViewById(R.id.btnViewReminders);
        Button btnGoHome           = findViewById(R.id.btnGoHome);

        if (btnViewAppointments != null)
            btnViewAppointments.setOnClickListener(v ->
                startActivity(new Intent(this, UserAppointmentsActivity.class)));

        if (btnViewReminders != null)
            btnViewReminders.setOnClickListener(v ->
                startActivity(new Intent(this, ReminderActivity.class)));

        if (btnGoHome != null)
            btnGoHome.setOnClickListener(v -> {
                Intent home = new Intent(this, HomeActivity.class);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(home);
            });
    }

    private void populateUI(String apptId, String doctor, String spec, String hospital,
                             String date, String time, String patient, String payMethod,
                             double consultFee, double total, String status) {

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

        if (tvDoctorName  != null) tvDoctorName.setText("Dr. " + doctor);
        if (tvSpec        != null) tvSpec.setText(spec.isEmpty() ? "General" : spec);
        if (tvDate        != null) tvDate.setText(date);
        if (tvTime        != null) tvTime.setText(time);
        if (tvHospital    != null) tvHospital.setText(hospital.isEmpty() ? "N/A" : hospital);
        if (tvPatientName != null) tvPatientName.setText(patient);
        if (tvPayMethod   != null) tvPayMethod.setText(payMethod);
        if (tvConsultFee  != null) tvConsultFee.setText("UGX " + nf.format(consultFee));
        if (tvTotal       != null) tvTotal.setText("UGX " + nf.format(total));

        // Booking ID — show last 8 chars of push key
        if (tvBookingId != null && !apptId.isEmpty()) {
            String shortId = apptId.length() > 8
                    ? apptId.substring(apptId.length() - 8).toUpperCase()
                    : apptId.toUpperCase();
            tvBookingId.setText("#" + shortId);
        }

        // Status badge colour
        if (tvStatus != null) {
            switch (status) {
                case "confirmed":
                    tvStatus.setText("✓  Confirmed");
                    tvStatus.setBackgroundColor(0xFFD1FAE5);
                    tvStatus.setTextColor(0xFF059669);
                    break;
                case "cancelled":
                    tvStatus.setText("✗  Cancelled");
                    tvStatus.setBackgroundColor(0xFFFEE2E2);
                    tvStatus.setTextColor(0xFFDC2626);
                    break;
                default:
                    tvStatus.setText("⏳  Pending Approval");
                    tvStatus.setBackgroundColor(0xFFFEF3C7);
                    tvStatus.setTextColor(0xFFD97706);
            }
        }

        // Reminder notice
        if (tvReminderNotice != null)
            tvReminderNotice.setText("🔔  Reminder saved! You'll be notified for your appointment on " + date + " at " + time + ".");
    }

    @Override public void onBackPressed() {
        // Don't allow going back to checkout
        Intent home = new Intent(this, HomeActivity.class);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(home);
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private String nvl(String s, String def) { return (s != null && !s.isEmpty()) ? s : def; }
}
