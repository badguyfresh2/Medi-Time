package com.example.meditime;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.Reminder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Fully offline reminder screen.
 * All data stored locally in SharedPreferences.
 * Notifications fired by AlarmManager — no internet required.
 */
public class ReminderActivity extends AppCompatActivity {

    private RecyclerView rvReminders;
    private TextView tvEmpty, tvTotal, tvChecked, tvUpcoming, tvProgress;
    private View progressFill, progressRemain;
    private ProgressBar progressBarContainer;

    private final List<Reminder> reminders = new ArrayList<>();
    private ReminderAdapter adapter;

    // Picked date/time for new reminder
    private Calendar pickedCal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnAdd = findViewById(R.id.btnAddAppointment);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> showAddReminderDialog());

        tvTotal    = findViewById(R.id.tvTotalToday);
        tvChecked  = findViewById(R.id.tvCheckedIn);
        tvUpcoming = findViewById(R.id.tvUpcoming);
        tvProgress = findViewById(R.id.tvProgressText);
        progressFill   = findViewById(R.id.progressBarCompleted);
        progressRemain = findViewById(R.id.progressBarRemaining);
        tvEmpty    = findViewById(R.id.tvReminderEmpty);
        rvReminders= findViewById(R.id.rvReminders);

        adapter = new ReminderAdapter(reminders,
            // check-in toggle
            (reminder, checked) -> {
                reminder.setCheckedIn(checked);
                ReminderStore.update(this, reminder);
                updateSummary();
            },
            // delete
            reminder -> {
                cancelAlarm(reminder);
                ReminderStore.delete(this, reminder.getId());
                reminders.remove(reminder);
                adapter.notifyDataSetChanged();
                updateSummary();
            }
        );

        if (rvReminders != null) {
            rvReminders.setLayoutManager(new LinearLayoutManager(this));
            rvReminders.setAdapter(adapter);
        }

        loadReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders(); // refresh in case returning from add
    }

    private void loadReminders() {
        reminders.clear();
        reminders.addAll(ReminderStore.load(this));
        if (adapter != null) adapter.notifyDataSetChanged();
        if (tvEmpty != null)
            tvEmpty.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
        updateSummary();
    }

    private void updateSummary() {
        int total = reminders.size();
        int checked = 0;
        for (Reminder r : reminders) if (r.isCheckedIn()) checked++;
        int upcoming = total - checked;
        float pct = total == 0 ? 0 : (checked * 100f / total);

        if (tvTotal != null)    tvTotal.setText(String.valueOf(total));
        if (tvChecked != null)  tvChecked.setText(String.valueOf(checked));
        if (tvUpcoming != null) tvUpcoming.setText(String.valueOf(upcoming));
        if (tvProgress != null) tvProgress.setText(String.format(Locale.getDefault(),
            "%d%% complete • %d appointment%s left %s",
            Math.round(pct), upcoming,
            upcoming == 1 ? "" : "s", upcoming == 0 ? "🎉" : "🗓️"));

        if (progressFill != null && progressRemain != null) {
            LinearLayout.LayoutParams pfp = (LinearLayout.LayoutParams) progressFill.getLayoutParams();
            LinearLayout.LayoutParams prp = (LinearLayout.LayoutParams) progressRemain.getLayoutParams();
            pfp.weight = Math.round(pct);
            prp.weight = 100 - Math.round(pct);
            progressFill.setLayoutParams(pfp);
            progressRemain.setLayoutParams(prp);
        }
    }

    // ── Add reminder dialog ──────────────────────────────────────────────────
    private void showAddReminderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null);
        EditText etTitle = dialogView.findViewById(R.id.etReminderTitle);
        EditText etNote  = dialogView.findViewById(R.id.etReminderNote);
        Button   btnDate = dialogView.findViewById(R.id.btnPickDate);
        Button   btnTime = dialogView.findViewById(R.id.btnPickTime);

        pickedCal = Calendar.getInstance();
        pickedCal.add(Calendar.HOUR_OF_DAY, 1);

        updateDateTimeButtons(btnDate, btnTime);

        btnDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (dp, y, m, d) -> {
                pickedCal.set(y, m, d);
                updateDateTimeButtons(btnDate, btnTime);
            }, pickedCal.get(Calendar.YEAR),
               pickedCal.get(Calendar.MONTH),
               pickedCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (tp, h, min) -> {
                pickedCal.set(Calendar.HOUR_OF_DAY, h);
                pickedCal.set(Calendar.MINUTE, min);
                pickedCal.set(Calendar.SECOND, 0);
                updateDateTimeButtons(btnDate, btnTime);
            }, pickedCal.get(Calendar.HOUR_OF_DAY),
               pickedCal.get(Calendar.MINUTE), false).show();
        });

        new AlertDialog.Builder(this)
            .setTitle("New Appointment Reminder")
            .setView(dialogView)
            .setPositiveButton("Set Reminder", (dlg, w) -> {
                String title = etTitle.getText().toString().trim();
                String note  = etNote.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
                    return;
                }
                String dateLabel = new SimpleDateFormat("EEE dd MMM", Locale.getDefault())
                    .format(pickedCal.getTime());
                String timeLabel = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(pickedCal.getTime());

                Reminder r = ReminderStore.add(this, title, note,
                    dateLabel, timeLabel, pickedCal.getTimeInMillis());
                scheduleAlarm(r);
                loadReminders();
                Toast.makeText(this, "Reminder set for " + timeLabel + " on " + dateLabel,
                    Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateDateTimeButtons(Button btnDate, Button btnTime) {
        btnDate.setText(new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            .format(pickedCal.getTime()));
        btnTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(pickedCal.getTime()));
    }

    // ── AlarmManager scheduling ──────────────────────────────────────────────
    private void scheduleAlarm(Reminder r) {
        if (r.getAlarmMillis() <= System.currentTimeMillis()) return;

        Intent intent = new Intent(this, ReminderAlarmReceiver.class);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_TITLE, r.getTitle());
        intent.putExtra(ReminderAlarmReceiver.EXTRA_NOTE,  r.getNote());

        PendingIntent pi = PendingIntent.getBroadcast(this,
            r.getId().hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                    && !am.canScheduleExactAlarms()) {
                // Fallback to inexact alarm (still works)
                am.set(AlarmManager.RTC_WAKEUP, r.getAlarmMillis(), pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.getAlarmMillis(), pi);
            }
        }
    }

    private void cancelAlarm(Reminder r) {
        Intent intent = new Intent(this, ReminderAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this,
            r.getId().hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }
}
