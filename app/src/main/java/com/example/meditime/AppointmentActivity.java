package com.example.meditime;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.meditime.model.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppointmentActivity extends AppCompatActivity {

    private String selectedDate = "", selectedTime = "", doctorId = "";
    private TextView tvSelectedDate, tvSelectedTime, tvDoctorNameHdr, tvSpecHdr, tvFeeHdr;
    private ProgressBar progressBar;
    private Button lastTimeBtn;
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private final List<String> bookedSlots = new ArrayList<>();
    private final List<String> availableSlots = new ArrayList<>();   // slots fetched from doctor's schedule
    private ViewGroup slotContainer;                                 // container for dynamic buttons

    // Doctor details loaded from DB
    private String doctorName = "", specialization = "", hospital = "";
    private double consultationFee = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appointment_screen);

        db    = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        doctorId = getIntent().getStringExtra("doctorId") != null
                ? getIntent().getStringExtra("doctorId") : "";

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        tvDoctorNameHdr = findViewById(R.id.tvDoctorNameHdr);
        tvSpecHdr       = findViewById(R.id.tvSpecHdr);
        tvFeeHdr        = findViewById(R.id.tvFeeHdr);
        progressBar    = findViewById(R.id.progressBar);
        slotContainer  = findViewById(R.id.slotGrid);    // replace with your container ID

        if (tvSelectedDate == null){
            Toast.makeText(AppointmentActivity.this ,"Select Date",Toast.LENGTH_LONG).show();
        }

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnConfirm = findViewById(R.id.btnConfirmBooking);
        if (btnConfirm != null) btnConfirm.setOnClickListener(v -> proceedToCheckout());

        CalendarView calendar = findViewById(R.id.calendarView);
        if (calendar != null) {
            calendar.setMinDate(System.currentTimeMillis() - 1000);
            calendar.setOnDateChangeListener((v, year, month, day) -> {
                selectedDate = day + "/" + (month + 1) + "/" + year;
                if (tvSelectedDate != null) tvSelectedDate.setText(selectedDate);
                loadDoctorSchedule(selectedDate);      // fetch real available slots
            });
        }

        loadDoctorDetails();
    }

    // ---------- Doctor Details ----------
    private void loadDoctorDetails() {
        if (doctorId.isEmpty()) return;
        db.child("doctors").child(doctorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snap) {
                        if (!snap.exists()) return;
                        doctorName       = snap.child("name").getValue(String.class) != null
                                ? snap.child("name").getValue(String.class) : "Doctor";
                        specialization   = snap.child("specialization").getValue(String.class) != null
                                ? snap.child("specialization").getValue(String.class) : "";
                        hospital         = snap.child("hospital").getValue(String.class) != null
                                ? snap.child("hospital").getValue(String.class) : "";
                        Double fee       = snap.child("consultationFeeUGX").getValue(Double.class);
                        consultationFee  = fee != null ? fee : 0;

                        if (tvDoctorNameHdr != null) tvDoctorNameHdr.setText("Dr. " + doctorName);
                        if (tvSpecHdr != null)       tvSpecHdr.setText(specialization.isEmpty() ? "General" : specialization);
                        if (tvFeeHdr != null)        tvFeeHdr.setText("UGX " + String.format(Locale.US, "%,.0f", consultationFee));
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    // ---------- Fetch Doctor's Schedule for the selected date ----------
    // ---------- Fetch Doctor's Schedule for the selected date ----------
    private void loadDoctorSchedule(String date) {
        if (doctorId.isEmpty() || date.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String dateKey = date.replace("/", "-");

        db.child("doctor_schedules").child(doctorId).child("slots").child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        availableSlots.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot slotSnap : snapshot.child("slots").getChildren()) {
                                String slot = slotSnap.getValue(String.class);
                                if (slot != null) availableSlots.add(slot);
                            }
                        }

                        // If the doctor has not set a schedule for this date, fall back to default slots
                        if (availableSlots.isEmpty()) {
                            generateDefaultSlots();   // 9:00 AM – 7:00 PM (hourly)
                        }

                        loadBookedSlots();   // now proceed to render
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(AppointmentActivity.this, "Failed to load schedule", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------- Helper to generate default hourly slots ----------
    private void generateDefaultSlots() {
        availableSlots.clear();
        // Hours from 9 AM to 7 PM (inclusive)
        int[] hours = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        for (int h : hours) {
            String amPm = (h < 12) ? "AM" : "PM";
            int displayHour = (h <= 12) ? h : h - 12;
            // Show as "9:00 AM", "12:00 PM", "5:00 PM", etc.
            availableSlots.add(String.format(Locale.US, "%d:00 %s", displayHour, amPm));
        }
    }

    // ---------- Booked slots from appointments ----------
    private void loadBookedSlots() {
        if (doctorId.isEmpty() || selectedDate.isEmpty()) return;
        db.child("appointments").orderByChild("doctorId").equalTo(doctorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snap) {
                        bookedSlots.clear();
                        for (DataSnapshot child : snap.getChildren()) {
                            String date   = child.child("date").getValue(String.class);
                            String status = child.child("status").getValue(String.class);
                            String slot   = child.child("timeSlot").getValue(String.class);
                            if (selectedDate.equals(date) && !"cancelled".equals(status) && slot != null)
                                bookedSlots.add(slot);
                        }
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        renderSlotButtons();
                    }
                    @Override public void onCancelled(DatabaseError e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    // ---------- Dynamically create time slot buttons ----------
    private void renderSlotButtons() {
        if (slotContainer == null) return;
        slotContainer.removeAllViews();

        // Ensure the GridLayout has 4 columns
        if (slotContainer instanceof GridLayout) {
            ((GridLayout) slotContainer).setColumnCount(4);
        }

        for (String slot : availableSlots) {
            // Create chip
            TextView chip = new TextView(this);
            chip.setText(slot);
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(13f);
            chip.setPadding(16, 12, 16, 12);
            chip.setTextColor(0xFF374151);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(32f);

            boolean isBooked = bookedSlots.contains(slot);
            if (isBooked) {
                bg.setColor(0xFFE5E7EB);
                chip.setTextColor(0xFF9CA3AF);
                chip.setEnabled(false);
            } else if (slot.equals(selectedTime)) {
                bg.setColor(0xFF0EA5E9);
                chip.setTextColor(0xFFFFFFFF);
            } else {
                bg.setColor(0xFFF3F4F6);
                chip.setTextColor(0xFF374151);
            }
            chip.setBackground(bg);

            // Layout parameters for GridLayout (make columns equal width)
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;                          // 0dp so column weight works
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);   // span 1 column, weight 1
            params.setMargins(8, 8, 8, 8);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                if (!isBooked) {
                    selectedTime = slot;
                    if (tvSelectedTime != null) tvSelectedTime.setText(slot);
                    renderSlotButtons(); // re-render to update highlights
                }
            });

            slotContainer.addView(chip);
        }
    }
    // ---------- Proceed to Checkout (unchanged) ----------
    private void proceedToCheckout() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show(); return;
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show(); return;
        }
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class)); return;
        }
        if (doctorId.isEmpty()) {
            Toast.makeText(this, "Doctor not specified", Toast.LENGTH_SHORT).show(); return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        String patientId = mAuth.getCurrentUser().getUid();

        db.child("users").child(patientId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot patSnap) {
                        String nameValue = patSnap.child("name").getValue(String.class);
                        final String patientName = (nameValue != null) ? nameValue : "Patient";

                        Appointment appt = new Appointment();
                        appt.setPatientId(patientId);
                        appt.setDoctorId(doctorId);
                        appt.setPatientName(patientName);
                        appt.setDoctorName(doctorName);
                        appt.setSpecialization(specialization);
                        appt.setDate(selectedDate);
                        appt.setTimeSlot(selectedTime);
                        appt.setStatus("pending");
                        appt.setPaymentStatus("unpaid");
                        appt.setConsultationFeeUGX(consultationFee);
                        appt.setCreatedAt(System.currentTimeMillis());

                        DatabaseReference apptRef = db.child("appointments").push();
                        final String apptId = apptRef.getKey();

                        apptRef.setValue(appt)
                                .addOnSuccessListener(v -> {
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                                    long alarmMillis = parseAlarmMillis(selectedDate, selectedTime);
                                    ReminderStore.add(
                                            AppointmentActivity.this,
                                            "Appointment – Dr. " + doctorName,
                                            specialization + (hospital.isEmpty() ? "" : "  •  " + hospital),
                                            formatDateLabel(selectedDate),
                                            selectedTime,
                                            alarmMillis
                                    );

                                    Intent intent = new Intent(AppointmentActivity.this, CheckoutActivity.class);
                                    intent.putExtra("appointmentId",  apptId);
                                    intent.putExtra("doctorId",       doctorId);
                                    intent.putExtra("doctorName",     doctorName);
                                    intent.putExtra("specialization", specialization);
                                    intent.putExtra("hospital",       hospital);
                                    intent.putExtra("date",           selectedDate);
                                    intent.putExtra("timeSlot",       selectedTime);
                                    intent.putExtra("fee",            consultationFee);
                                    intent.putExtra("patientName",    patientName);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(ex -> {
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                                    Toast.makeText(AppointmentActivity.this,
                                            "Failed to save appointment: " + ex.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                    @Override public void onCancelled(DatabaseError e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(AppointmentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------- Helper methods (unchanged) ----------
    private long parseAlarmMillis(String date, String time) {
        try {
            String[] dp = date.split("/");
            int day = Integer.parseInt(dp[0]), mon = Integer.parseInt(dp[1]) - 1, year = Integer.parseInt(dp[2]);
            String[] tp = time.split(":");
            int hour = Integer.parseInt(tp[0]);
            String[] minAm = tp[1].split(" ");
            int min = Integer.parseInt(minAm[0]);
            boolean pm = minAm[1].equalsIgnoreCase("PM");
            if (pm && hour != 12) hour += 12;
            if (!pm && hour == 12) hour = 0;
            Calendar cal = Calendar.getInstance();
            cal.set(year, mon, day, hour, min, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis() + 3_600_000L;
        }
    }

    private String formatDateLabel(String date) {
        try {
            String[] dp = date.split("/");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(dp[2]), Integer.parseInt(dp[1]) - 1, Integer.parseInt(dp[0]));
            return new SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(cal.getTime());
        } catch (Exception e) { return date; }
    }
}
