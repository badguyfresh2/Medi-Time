package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.meditime.model.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class AppointmentActivity extends AppCompatActivity {

    private String selectedDate = "", selectedTime = "", doctorId = "";
    private TextView tvSelectedDate, tvSelectedTime;
    private ProgressBar progressBar;
    private Button lastTimeBtn;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private final List<String> bookedSlots = new ArrayList<>();

    private static final int[]    SLOT_IDS   = {
            R.id.timeSlot9, R.id.timeSlot930, R.id.timeSlot10,
            R.id.timeSlot11, R.id.timeSlot2,  R.id.timeSlot230,
            R.id.timeSlot330, R.id.timeSlot4, R.id.timeSlot430
    };
    private static final String[] SLOT_LABELS = {
            "9:00 AM","9:30 AM","10:00 AM",
            "11:00 AM","2:00 PM","2:30 PM",
            "3:30 PM","4:00 PM","4:30 PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appointment_screen);

        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        doctorId = getIntent().getStringExtra("doctorId") != null
                ? getIntent().getStringExtra("doctorId") : "";

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        progressBar    = findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        CalendarView calendar = findViewById(R.id.calendarView);
        if (calendar != null) {
            calendar.setMinDate(System.currentTimeMillis());
            calendar.setOnDateChangeListener((v, year, month, day) -> {
                selectedDate = day + "/" + (month + 1) + "/" + year;
                if (tvSelectedDate != null) tvSelectedDate.setText(selectedDate);
                loadBookedSlots();
            });
        }

        for (int i = 0; i < SLOT_IDS.length; i++) {
            setupTimeSlot(SLOT_IDS[i], SLOT_LABELS[i]);
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnConfirm = findViewById(R.id.btnConfirmBooking);
        if (btnConfirm != null) btnConfirm.setOnClickListener(v -> bookAppointment());
    }

    private void setupTimeSlot(int id, String time) {
        Button btn = findViewById(id);
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            if (bookedSlots.contains(time)) {
                Toast.makeText(this, R.string.slot_already_booked, Toast.LENGTH_SHORT).show();
                return;
            }
            selectedTime = time;
            if (tvSelectedTime != null) tvSelectedTime.setText(time);
            if (lastTimeBtn != null) {
                lastTimeBtn.setBackgroundColor(0xFFF3F4F6);
                lastTimeBtn.setTextColor(0xFF374151);
            }
            btn.setBackgroundColor(0xFF0EA5E9);
            btn.setTextColor(0xFFFFFFFF);
            lastTimeBtn = btn;
        });
    }

    private void loadBookedSlots() {
        if (doctorId.isEmpty() || selectedDate.isEmpty()) return;
        dbRef.child("appointments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookedSlots.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment a = child.getValue(Appointment.class);
                    if (a != null && doctorId.equals(a.getDoctorId())
                            && selectedDate.equals(a.getDate())
                            && !"cancelled".equals(a.getStatus())) {
                        String slot = a.getTimeSlot();
                        if (slot != null) bookedSlots.add(slot);
                    }
                }
                refreshSlotColors();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AppointmentActivity.this, "Failed to load slots", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshSlotColors() {
        for (int i = 0; i < SLOT_IDS.length; i++) {
            Button btn = findViewById(SLOT_IDS[i]);
            if (btn == null) continue;
            if (bookedSlots.contains(SLOT_LABELS[i])) {
                btn.setBackgroundColor(0xFFE5E7EB);
                btn.setTextColor(0xFF9CA3AF);
            } else if (SLOT_LABELS[i].equals(selectedTime)) {
                btn.setBackgroundColor(0xFF0EA5E9);
                btn.setTextColor(0xFFFFFFFF);
            } else {
                btn.setBackgroundColor(0xFFF3F4F6);
                btn.setTextColor(0xFF374151);
            }
        }
    }

    private void bookAppointment() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, R.string.please_select_date, Toast.LENGTH_SHORT).show(); return;
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, R.string.please_select_time, Toast.LENGTH_SHORT).show(); return;
        }
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, R.string.please_login_first, Toast.LENGTH_SHORT).show(); return;
        }
        if (doctorId.isEmpty()) {
            Toast.makeText(this, R.string.doctor_not_specified, Toast.LENGTH_SHORT).show(); return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        final String patientId = mAuth.getCurrentUser().getUid();

        dbRef.child("users").child(patientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot patSnap) {
                String fetchedName = patSnap.child("name").getValue(String.class);
                final String finalPatientName = (fetchedName != null) ? fetchedName : "Patient";

                dbRef.child("doctors").child(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot docSnap) {
                        final String doctorName  = docSnap.child("name").getValue(String.class);
                        final String spec        = docSnap.child("specialization").getValue(String.class);
                        final String hospital    = docSnap.child("hospital").getValue(String.class);
                        Object feeObj            = docSnap.child("consultationFeeUGX").getValue();
                        final double fee         = (feeObj instanceof Number) ? ((Number) feeObj).doubleValue() : 0;

                        Appointment appt = new Appointment();
                        appt.setPatientId(patientId);
                        appt.setDoctorId(doctorId);
                        appt.setPatientName(finalPatientName);
                        appt.setDoctorName(doctorName != null ? doctorName : "Doctor");
                        appt.setSpecialization(spec != null ? spec : "");
                        appt.setDate(selectedDate);
                        appt.setTimeSlot(selectedTime);
                        appt.setVenue(hospital != null ? hospital : "");
                        appt.setStatus("pending");
                        appt.setPaymentStatus("unpaid");
                        appt.setConsultationFeeUGX(fee);
                        appt.setCreatedAtLong(System.currentTimeMillis());

                        DatabaseReference newRef = dbRef.child("appointments").push();
                        appt.setAppointmentId(newRef.getKey());
                        newRef.setValue(appt)
                                .addOnSuccessListener(aVoid -> {
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                                    String appointmentId = newRef.getKey();

                                    String reminderTitle = getString(R.string.appointment_reminder_title, doctorName != null ? doctorName : "Doctor");
                                    String reminderNote  = (spec != null ? spec : "") + (hospital != null && !hospital.isEmpty() ? " • " + hospital : "");

                                    long alarmMillis = parseAlarmMillis(selectedDate, selectedTime);

                                    ReminderStore.add(
                                            AppointmentActivity.this,
                                            reminderTitle,
                                            reminderNote,
                                            formatDateLabel(selectedDate),
                                            selectedTime,
                                            alarmMillis
                                    );

                                    Intent intent = new Intent(AppointmentActivity.this, BookingConfirmationActivity.class);
                                    intent.putExtra("appointmentId",   appointmentId);
                                    intent.putExtra("doctorName",      doctorName);
                                    intent.putExtra("specialization",  spec != null ? spec : "");
                                    intent.putExtra("hospital",        hospital != null ? hospital : "");
                                    intent.putExtra("date",            selectedDate);
                                    intent.putExtra("timeSlot",        selectedTime);
                                    intent.putExtra("fee",             fee);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);

                                })
                                .addOnFailureListener(ex -> {
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                                    Toast.makeText(AppointmentActivity.this, getString(R.string.error_prefix, ex.getMessage()), Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private long parseAlarmMillis(String date, String time) {
        try {
            String[] dp = date.split("/");
            int day  = Integer.parseInt(dp[0]);
            int mon  = Integer.parseInt(dp[1]) - 1;
            int year = Integer.parseInt(dp[2]);

            String[] tp    = time.split(":");
            int hour       = Integer.parseInt(tp[0]);
            String[] minAm = tp[1].split(" ");
            int min        = Integer.parseInt(minAm[0]);
            boolean pm     = minAm[1].equalsIgnoreCase("PM");
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
            cal.set(Integer.parseInt(dp[2]),
                    Integer.parseInt(dp[1]) - 1,
                    Integer.parseInt(dp[0]));
            return new java.text.SimpleDateFormat("EEE dd MMM", Locale.getDefault())
                    .format(cal.getTime());
        } catch (Exception e) {
            return date;
        }
    }
}
