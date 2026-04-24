package com.example.meditime;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class DoctorScheduleActivity extends AppCompatActivity {

    private LinearLayout slotContainer;
    private TextView tvSelectedDate, tvSlotCount;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private String doctorId, selectedDate = "";
    private final List<String> currentSlots = new ArrayList<>();
    private final List<String> bookedSlots  = new ArrayList<>();

    private static final String[] DEFAULT_SLOTS = {
            "8:00 AM","8:30 AM","9:00 AM","9:30 AM","10:00 AM","10:30 AM",
            "11:00 AM","11:30 AM","1:00 PM","1:30 PM","2:00 PM","2:30 PM",
            "3:00 PM","3:30 PM","4:00 PM","4:30 PM","5:00 PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_schedule);

        dbRef = FirebaseDatabase.getInstance().getReference();
        doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        CalendarView calendarView = findViewById(R.id.calendarView);
        slotContainer  = findViewById(R.id.slotContainer);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSlotCount    = findViewById(R.id.tvSlotCount);
        Button btnAddSlot = findViewById(R.id.btn_add_slot);
        Button btnSave = findViewById(R.id.btn_edit_schedule);
        progressBar    = findViewById(R.id.progressBar);

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (calendarView != null) {
            calendarView.setMinDate(System.currentTimeMillis() - 1000);
            calendarView.setOnDateChangeListener((v, year, month, day) -> {
                selectedDate = day + "/" + (month + 1) + "/" + year;
                if (tvSelectedDate != null) tvSelectedDate.setText(selectedDate);
                loadScheduleForDate();
            });
        }

        if (btnAddSlot != null) btnAddSlot.setOnClickListener(v -> showAddSlotDialog());
        if (btnSave != null) btnSave.setOnClickListener(v -> saveSchedule());
    }

    private void loadScheduleForDate() {
        if (doctorId.isEmpty() || selectedDate.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String dateKey = selectedDate.replace("/", "-");

        dbRef.child("doctor_schedules").child(doctorId).child("slots").child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot doc) {
                        currentSlots.clear();
                        if (doc.exists() && doc.child("slots").getValue() != null) {
                            for (DataSnapshot slot : doc.child("slots").getChildren()) {
                                String s = slot.getValue(String.class);
                                if (s != null) currentSlots.add(s);
                            }
                        } else {
                            currentSlots.addAll(Arrays.asList(DEFAULT_SLOTS));
                        }
                        loadBookedSlots();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadBookedSlots() {
        dbRef.child("appointments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                bookedSlots.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    String dId = ds.child("doctorId").getValue(String.class);
                    String date = ds.child("date").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);
                    if (doctorId.equals(dId) && selectedDate.equals(date) && !"cancelled".equals(status)) {
                        String slot = ds.child("timeSlot").getValue(String.class);
                        if (slot != null) bookedSlots.add(slot);
                    }
                }
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                renderSlots();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void renderSlots() {
        if (slotContainer == null) return;
        slotContainer.removeAllViews();

        for (String slot : currentSlots) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_schedule_slot, slotContainer, false);
            TextView tvSlot   = row.findViewById(R.id.tvSlot);
            TextView tvStatus = row.findViewById(R.id.tvSlotStatus);
            Button btnRemove  = row.findViewById(R.id.btnRemoveSlot);

            if (tvSlot != null)   tvSlot.setText(slot);
            boolean isBooked = bookedSlots.contains(slot);
            if (tvStatus != null) {
                tvStatus.setText(isBooked ? getString(R.string.booked) : getString(R.string.available));
                tvStatus.setTextColor(isBooked ? 0xFFEF4444 : 0xFF059669);
            }
            if (btnRemove != null) {
                if (isBooked) {
                    btnRemove.setVisibility(View.GONE);
                } else {
                    btnRemove.setOnClickListener(v -> {
                        currentSlots.remove(slot);
                        renderSlots();
                    });
                }
            }
            slotContainer.addView(row);
        }
        if (tvSlotCount != null)
            tvSlotCount.setText(getString(R.string.slot_count_format, currentSlots.size()));
    }

    private void showAddSlotDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.add_slot_hint);
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.add_time_slot)
                .setView(input)
                .setPositiveButton(R.string.add, (d, w) -> {
                    String slot = input.getText().toString().trim();
                    if (!slot.isEmpty() && !currentSlots.contains(slot)) {
                        currentSlots.add(slot);
                        renderSlots();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveSchedule() {
        if (doctorId.isEmpty() || selectedDate.isEmpty()) {
            Toast.makeText(this, R.string.select_date_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String dateKey = selectedDate.replace("/", "-");
        Map<String, Object> data = new HashMap<>();
        data.put("slots", currentSlots);
        data.put("date", selectedDate);
        data.put("doctorId", doctorId);

        dbRef.child("doctor_schedules").child(doctorId).child("slots").child(dateKey).setValue(data)
                .addOnSuccessListener(v -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.schedule_saved_for, selectedDate), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.error_prefix, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }
}