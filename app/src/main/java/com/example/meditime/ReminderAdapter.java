package com.example.meditime;

import android.graphics.Paint;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.Reminder;
import java.util.List;

/**
 * ReminderAdapter
 * – Selected item (tapped) is highlighted with a GREEN background.
 * – Checked-in items show green tick, strikethrough title, and green time label.
 * – Unchecked items show an empty checkbox, normal title, and amber time label.
 */
public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.VH> {

    public interface OnCheckIn { void onCheckIn(Reminder r, boolean checked); }
    public interface OnDelete  { void onDelete(Reminder r); }
    public interface OnSelect  { void onSelect(int position); }

    private final List<Reminder> list;
    private final OnCheckIn onCheckIn;
    private final OnDelete  onDelete;
    private final OnSelect  onSelect;   // optional; pass null if not needed

    private int selectedPosition = RecyclerView.NO_ID;

    public ReminderAdapter(List<Reminder> list, OnCheckIn onCheckIn, OnDelete onDelete) {
        this(list, onCheckIn, onDelete, null);
    }

    public ReminderAdapter(List<Reminder> list, OnCheckIn onCheckIn,
                           OnDelete onDelete, OnSelect onSelect) {
        this.list      = list;
        this.onCheckIn = onCheckIn;
        this.onDelete  = onDelete;
        this.onSelect  = onSelect;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Reminder r = list.get(pos);

        // ── Text content ──────────────────────────────────────────────────────
        h.tvTitle.setText(r.getTitle() != null ? r.getTitle() : "");
        h.tvNote .setText(r.getNote()  != null ? r.getNote()  : "");
        h.tvDate .setText(r.getDateLabel() != null ? r.getDateLabel() : "");
        h.tvTime .setText(r.getTimeLabel() != null ? r.getTimeLabel() : "");

        // ── Checked-in state ─────────────────────────────────────────────────
        boolean done = r.isCheckedIn();
        if (done) {
            h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvTitle.setTextColor(0xFF9CA3AF);   // muted gray
            h.tvTime.setTextColor(0xFF10B981);    // green time label when done
        } else {
            h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvTitle.setTextColor(0xFF111827);   // dark text
            h.tvTime.setTextColor(0xFFF4A623);    // amber time label when pending
        }

        // ── Checkbox ─────────────────────────────────────────────────────────
        h.checkBox.setOnCheckedChangeListener(null);
        h.checkBox.setChecked(done);
        h.checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                done ? 0xFF10B981 : 0xFF7B5EA7));
        h.checkBox.setOnCheckedChangeListener((btn, checked) -> onCheckIn.onCheckIn(r, checked));

        // ── Selected (tapped) → GREEN highlight ───────────────────────────────
        boolean selected = (pos == selectedPosition);
        if (selected) {
            h.itemView.setBackgroundColor(0xFFDCFCE7);   // green-50
            h.tvTitle.setTextColor(done ? 0xFF4ADE80 : 0xFF15803D); // green shades
            h.tvTime.setTextColor(0xFF16A34A);
        } else if (done) {
            h.itemView.setBackgroundColor(0xFFF9FAFB);   // near-white for done items
        } else {
            h.itemView.setBackgroundColor(0xFFFFFFFF);   // white default
        }

        // ── Selection tap ─────────────────────────────────────────────────────
        h.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = (selectedPosition == pos) ? RecyclerView.NO_ID : pos;
            notifyItemChanged(prev);
            notifyItemChanged(pos);
            if (onSelect != null) onSelect.onSelect(selectedPosition);
        });

        // ── Delete ────────────────────────────────────────────────────────────
        if (h.btnDelete != null)
            h.btnDelete.setOnClickListener(v -> onDelete.onDelete(r));
    }

    @Override public int getItemCount() { return list.size(); }

    /** Programmatically select an item (e.g. after add). */
    public void setSelectedPosition(int pos) {
        int prev = selectedPosition;
        selectedPosition = pos;
        notifyItemChanged(prev);
        notifyItemChanged(pos);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView  tvTitle, tvNote, tvDate, tvTime;
        CheckBox  checkBox;
        ImageView btnDelete;

        VH(View v) {
            super(v);
            tvTitle   = v.findViewById(R.id.tvReminderTitle);
            tvNote    = v.findViewById(R.id.tvReminderNote);
            tvDate    = v.findViewById(R.id.tvReminderDate);
            tvTime    = v.findViewById(R.id.tvReminderTime);
            checkBox  = v.findViewById(R.id.checkBoxReminder);
            btnDelete = v.findViewById(R.id.btnDeleteReminder);
        }
    }
}
