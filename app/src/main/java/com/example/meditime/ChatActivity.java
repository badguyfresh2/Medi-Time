package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnSend;
    private TextView tvOtherName;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private String currentUid, otherUserId, chatRoomId;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        dbRef    = FirebaseDatabase.getInstance().getReference();
        mAuth    = FirebaseAuth.getInstance();
        currentUid   = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        otherUserId  = getIntent().getStringExtra("doctorId") != null
                ? getIntent().getStringExtra("doctorId")
                : getIntent().getStringExtra("patientId") != null
                ? getIntent().getStringExtra("patientId") : "";

        String[] ids = new String[]{currentUid, otherUserId};
        java.util.Arrays.sort(ids);
        chatRoomId = "chat_" + ids[0] + "_" + ids[1];

        rvMessages  = findViewById(R.id.rvMessages);
        etMessage   = findViewById(R.id.etMessage);
        btnSend     = findViewById(R.id.btnSend);
        tvOtherName = findViewById(R.id.tvOtherName);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageView btnVideoCall = findViewById(R.id.btnVideoCall);
        if (btnVideoCall != null)
            btnVideoCall.setOnClickListener(v -> startActivity(new Intent(this, VideoCallActivity.class)));

        adapter = new ChatAdapter(messages, currentUid);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        loadOtherUserName();
        listenForMessages();

        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadOtherUserName() {
        if (otherUserId.isEmpty()) return;
        dbRef.child("users").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {
                if (ds.exists()) {
                    String name = ds.child("name").getValue(String.class);
                    if (tvOtherName != null) tvOtherName.setText(name != null ? name : "Chat");
                } else {
                    dbRef.child("doctors").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dsnap) {
                            String n = dsnap.child("name").getValue(String.class);
                            if (tvOtherName != null) tvOtherName.setText(n != null ? "Dr. " + n : "Doctor");
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForMessages() {
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                messages.clear();
                for (DataSnapshot msgSnap : snap.getChildren()) {
                    ChatMessage msg = msgSnap.getValue(ChatMessage.class);
                    if (msg != null) {
                        msg.setMessageId(msgSnap.getKey());
                        messages.add(msg);
                    }
                }
                Collections.sort(messages, (a, b) -> {
                    Long t1 = a.getTimestamp() != null ? a.getTimestamp().getTime() : 0;
                    Long t2 = b.getTimestamp() != null ? b.getTimestamp().getTime() : 0;
                    return t1.compareTo(t2);
                });
                adapter.notifyDataSetChanged();
                if (!messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        dbRef.child("chat_rooms").child(chatRoomId).child("messages").addValueEventListener(listener);
    }

    private void sendMessage() {
        if (etMessage == null) return;
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage msg = new ChatMessage();
        msg.setSenderId(currentUid);
        msg.setText(text);
        msg.setType("text");
        msg.setSeen(false);
        msg.setTimestamp(new Date());

        // Update room metadata
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("participants", Arrays.asList(currentUid, otherUserId));
        roomData.put("lastMessage", text);
        roomData.put("updatedAt", ServerValue.TIMESTAMP);
        dbRef.child("chat_rooms").child(chatRoomId).updateChildren(roomData);

        DatabaseReference msgRef = dbRef.child("chat_rooms").child(chatRoomId).child("messages").push();
        msgRef.setValue(msg)
                .addOnSuccessListener(r -> etMessage.setText(""))
                .addOnFailureListener(ex -> Toast.makeText(ChatActivity.this, "Send failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            dbRef.child("chat_rooms").child(chatRoomId).child("messages").removeEventListener(listener);
        }
    }
}