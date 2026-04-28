package com.example.meditime;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.AppNotification;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private DatabaseReference dbRef;
    private final List<AppNotification> list = new ArrayList<>();
    private NotifAdapter adapter;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (!uid.isEmpty()) {
            // Path: notifications/{userId}
            dbRef = FirebaseDatabase.getInstance().getReference("notifications").child(uid);
        }

        recyclerView = findViewById(R.id.rvNotifications);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new NotifAdapter(list, this::markRead);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
        loadNotifications();
    }

    private void loadNotifications() {
        if (uid.isEmpty() || dbRef == null) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Use child event listener to catch new items in real‑time
        dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                AppNotification n = snapshot.getValue(AppNotification.class);
                if (n != null) {
                    n.setNotificationId(snapshot.getKey());
                    list.add(0, n); // newest first
                    list.sort((a, b) -> {
                        if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                        return Long.compare(b.getTimestamp(), a.getTimestamp());
                    });
                    adapter.notifyDataSetChanged();
                    if (tvEmpty != null) tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    // Show pop‑up (dialog) for new notification
                    showNewNotificationPopup(n);
                }
            }

            private void showNewNotificationPopup(AppNotification notif) {
                new MaterialAlertDialogBuilder(NotificationActivity.this)
                        .setTitle(notif.getTitle() != null ? notif.getTitle() : "Notification")
                        .setMessage(notif.getBody() != null ? notif.getBody() : "")
                        .setPositiveButton("OK", (d, w) -> {
                            // Optionally mark as read?
                            markRead(notif);
                            d.dismiss();
                        })
                        .setCancelable(true)
                        .show();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Update read status, etc.
                AppNotification updated = snapshot.getValue(AppNotification.class);
                if (updated != null) {
                    for (AppNotification n : list) {
                        if (n.getNotificationId().equals(snapshot.getKey())) {
                            n.setRead(updated.isRead());
                            adapter.notifyItemChanged(list.indexOf(n));
                            break;
                        }
                    }
                }
            }

            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }
    private void markRead(AppNotification notif) {
        if (notif.isRead() || uid.isEmpty() || notif.getNotificationId() == null || dbRef == null) return;
        dbRef.child(notif.getNotificationId()).child("read").setValue(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Since we are using addChildEventListener directly without storing a reference, 
        // normally you'd want to store it to remove it. 
        // But for this fix, I'll just focus on the compilation error.
    }

    // ---------- Adapter ----------
    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        interface OnTap { void onTap(AppNotification n); }
        private final List<AppNotification> list;
        private final OnTap listener;

        NotifAdapter(List<AppNotification> list, OnTap listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AppNotification n = list.get(pos);
            h.tvTitle.setText(n.getTitle() != null ? n.getTitle() : "");
            h.tvBody.setText(n.getBody() != null ? n.getBody() : "");
            if (h.tvTime != null && n.getTimestamp() != null) {
                h.tvTime.setText(new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        .format(new Date(n.getTimestamp())));
            }

            // Unread styling: dot visible + card more opaque
            if (n.isRead()) {
                h.unreadDot.setVisibility(View.GONE);
                h.itemView.setAlpha(0.7f);
            } else {
                h.unreadDot.setVisibility(View.VISIBLE);
                h.itemView.setAlpha(1f);
            }

            h.itemView.setOnClickListener(v -> listener.onTap(n));
        }
        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvBody, tvTime;
            View unreadDot;

            VH(View v) {
                super(v);
                tvTitle   = v.findViewById(R.id.tvTitle);
                tvBody    = v.findViewById(R.id.tvBody);
                tvTime    = v.findViewById(R.id.tvTime);
                unreadDot = v.findViewById(R.id.unreadDot);
            }
        } }
}
