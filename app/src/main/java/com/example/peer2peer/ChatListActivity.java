package com.example.peer2peer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.adapters.ChatListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity implements ChatListAdapter.OnChatRoomClickListener {

    private static final String TAG = "ChatListActivity";

    private Toolbar toolbar;
    private RecyclerView recyclerViewChatList;
    private ChatListAdapter chatListAdapter;
    private List<ChatRoomSummary> chatRoomSummariesList;
    private ProgressBar progressBar;
    private TextView textViewNoChats;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration chatRoomsListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        toolbar = findViewById(R.id.toolbar_chat_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Chats");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerViewChatList = findViewById(R.id.recyclerView_chat_list);
        progressBar = findViewById(R.id.progressBar_chat_list);
        textViewNoChats = findViewById(R.id.textView_no_chats);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to view chats.", Toast.LENGTH_LONG).show();
            
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        chatRoomSummariesList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(this, chatRoomSummariesList, this);
        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChatList.setAdapter(chatListAdapter);

        loadChatRooms();
    }

    private void loadChatRooms() {
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null, cannot load chat rooms.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewNoChats.setVisibility(View.GONE);
        recyclerViewChatList.setVisibility(View.GONE);

        Query chatRoomsQuery = db.collection("chat_rooms")
                .whereArrayContains("participantIds", currentUserId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

        chatRoomsListener = chatRoomsQuery.addSnapshotListener((snapshots, e) -> {
            progressBar.setVisibility(View.GONE);
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                textViewNoChats.setText("Failed to load chats.");
                textViewNoChats.setVisibility(View.VISIBLE);
                return;
            }

            if (snapshots == null || snapshots.isEmpty()) {
                Log.d(TAG, "No chat rooms found for user: " + currentUserId);
                textViewNoChats.setText("You have no active chats yet.");
                textViewNoChats.setVisibility(View.VISIBLE);
                recyclerViewChatList.setVisibility(View.GONE);
                chatRoomSummariesList.clear(); // Clear any existing data
                chatListAdapter.notifyDataSetChanged();
                return;
            }

            chatRoomSummariesList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                String chatRoomId = doc.getId();
                List<String> participantIds = (List<String>) doc.get("participantIds");
                Map<String, String> participantNames = (Map<String, String>) doc.get("participantNames");
                String lastMessageText = doc.getString("lastMessageText");
                Date lastMessageTimestamp = doc.getDate("lastMessageTimestamp");
               

                if (participantIds != null && participantIds.size() == 2 && participantNames != null) {
                    String otherParticipantId = null;
                    String otherParticipantName = "Unknown User";
                   

                    for (String id : participantIds) {
                        if (!id.equals(currentUserId)) {
                            otherParticipantId = id;
                            break;
                        }
                    }

                    if (otherParticipantId != null && participantNames.containsKey(otherParticipantId)) {
                        otherParticipantName = participantNames.get(otherParticipantId);
                       
                    } else if (otherParticipantId != null) {
                        Log.w(TAG, "Name not found in participantNames map for ID: " + otherParticipantId + " in chatRoom: " + chatRoomId);
                       
                    }


                    if (otherParticipantId != null) {
                        
                        String profileImageUrl = null; 

                        ChatRoomSummary summary = new ChatRoomSummary(
                                chatRoomId,
                                otherParticipantId,
                                otherParticipantName,
                                profileImageUrl, 
                                lastMessageText,
                                lastMessageTimestamp
                        );
                        chatRoomSummariesList.add(summary);
                    }
                } else {
                    Log.w(TAG, "Chat room document " + chatRoomId + " has malformed participant data.");
                }
            }

            if (chatRoomSummariesList.isEmpty()) {
                textViewNoChats.setText("You have no active chats yet.");
                textViewNoChats.setVisibility(View.VISIBLE);
                recyclerViewChatList.setVisibility(View.GONE);
            } else {
                textViewNoChats.setVisibility(View.GONE);
                recyclerViewChatList.setVisibility(View.VISIBLE);
            }
            chatListAdapter.updateChatRooms(chatRoomSummariesList); 
            Log.d(TAG, "Chat rooms loaded: " + chatRoomSummariesList.size());
        });
    }

    @Override
    public void onChatRoomClick(ChatRoomSummary chatRoomSummary) {
        Log.d(TAG, "Clicked on chat room: " + chatRoomSummary.getChatRoomId() + " with " + chatRoomSummary.getOtherParticipantName());
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHAT_PARTNER_ID, chatRoomSummary.getOtherParticipantId());
        intent.putExtra(ChatActivity.EXTRA_CHAT_PARTNER_NAME, chatRoomSummary.getOtherParticipantName());

        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); 
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRoomsListener != null) {
            chatRoomsListener.remove(); 
        }
    }
}