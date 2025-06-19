package com.example.peer2peer;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.adapters.DirectMessageAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    public static final String EXTRA_CHAT_PARTNER_ID = "chat_partner_id";
    public static final String EXTRA_CHAT_PARTNER_NAME = "chat_partner_name";

    private Toolbar toolbarChat;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessageInput;
    private ImageButton buttonSendMessage;

    private DirectMessageAdapter messageAdapter;
    private List<DirectMessage> messagesList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration messagesListener;

    private String currentUserId;
    private String currentUserName; 
    private String chatPartnerId;
    private String chatPartnerName;
    private String chatRoomId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated. Please login.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = firebaseUser.getUid();

        chatPartnerId = getIntent().getStringExtra(EXTRA_CHAT_PARTNER_ID);
        chatPartnerName = getIntent().getStringExtra(EXTRA_CHAT_PARTNER_NAME);

        if (chatPartnerId == null || chatPartnerId.isEmpty() || chatPartnerName == null || chatPartnerName.isEmpty()) {
            Toast.makeText(this, "Chat partner details missing. Cannot start chat.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Chat partner ID or Name is null/empty. PartnerID: " + chatPartnerId + ", PartnerName: " + chatPartnerName);
            finish();
            return;
        }

        chatRoomId = getChatRoomId(currentUserId, chatPartnerId);
        if (chatRoomId.equals("error_chat_room_id")) {
            Toast.makeText(this, "Error creating chat session ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        toolbarChat = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbarChat);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(chatPartnerName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessageInput = findViewById(R.id.editTextMessageInput);
        buttonSendMessage = findViewById(R.id.buttonSendMessage);

        messagesList = new ArrayList<>();
        messageAdapter = new DirectMessageAdapter(this, messagesList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);

        fetchCurrentUserNameAndLoadMessages();

        buttonSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void fetchCurrentUserNameAndLoadMessages() {
        if (currentUserId == null) {
            Log.e(TAG, "Cannot fetch user name, currentUserId is null.");
            currentUserName = "User";
            loadMessages();
            return;
        }
        Log.d(TAG, "Fetching user name for UID: " + currentUserId);
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("firstName");
                        if (TextUtils.isEmpty(currentUserName)) {
                            currentUserName = "User " + currentUserId.substring(0, Math.min(5, currentUserId.length()));
                            Log.w(TAG, "Fetched user document but 'firstName' is empty. Using fallback: " + currentUserName);
                        } else {
                            Log.d(TAG, "Successfully fetched currentUserName: " + currentUserName);
                        }
                    } else {
                        currentUserName = "You (Name N/A)";
                        Log.w(TAG, "User document not found for UID: " + currentUserId + ". Using fallback name.");
                    }
                    loadMessages();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user name for UID: " + currentUserId, e);
                    currentUserName = "You (Error)";
                    loadMessages();
                });
    }


    private String getChatRoomId(String userId1, String userId2) {
        if (userId1 == null || userId2 == null || userId1.isEmpty() || userId2.isEmpty()) {
            Log.e(TAG, "User ID is null or empty, cannot create chat room ID. User1: " + userId1 + ", User2: " + userId2);
            return "error_chat_room_id";
        }
        if (userId1.compareTo(userId2) > 0) {
            return userId2 + "_" + userId1;
        } else {
            return userId1 + "_" + userId2;
        }
    }

    private void loadMessages() {
        if (chatRoomId == null || chatRoomId.equals("error_chat_room_id")) {
            Log.e(TAG, "Invalid chatRoomId (" + chatRoomId + "), cannot load messages.");
            Toast.makeText(this, "Cannot load messages: Invalid chat session.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Loading messages for chatRoomId: " + chatRoomId);

        CollectionReference messagesRef = db.collection("chat_rooms").document(chatRoomId)
                .collection("messages");

        messagesListener = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for messages in chatRoomId: " + chatRoomId, e);
                        if (e.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Toast.makeText(ChatActivity.this, "You don't have permission to view these messages.", Toast.LENGTH_LONG).show();
                        } else if (e.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            Toast.makeText(ChatActivity.this, "Loading messages failed: Data query needs an index.", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "FAILED_PRECONDITION for messages query. Check Firestore indexes for messages subcollection, ordering by timestamp.", e);
                        }
                        return;
                    }

                    if (snapshots == null) {
                        Log.d(TAG, "Snapshots are null for messages in chatRoomId: " + chatRoomId);
                        return;
                    }

                    Log.d(TAG, "Received " + snapshots.getDocumentChanges().size() + " document changes for messages.");
                    boolean newMessagesAddedToList = false; // Renamed for clarity
                    List<DirectMessage> newBatchOfMessages = new ArrayList<>();
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DirectMessage message = dc.getDocument().toObject(DirectMessage.class);
                            message.setMessageId(dc.getDocument().getId());
                            newBatchOfMessages.add(message);
                        }
                       
                    }

                    if (!newBatchOfMessages.isEmpty()) {
                        Map<String, DirectMessage> messageMap = new HashMap<>();
                        
                        for (DirectMessage msg : messagesList) {
                            messageMap.put(msg.getMessageId(), msg);
                        }
                        
                        for (DirectMessage msg : newBatchOfMessages) {
                            messageMap.put(msg.getMessageId(), msg);
                            newMessagesAddedToList = true; 
                        }

                        if(newMessagesAddedToList){
                            messagesList.clear();
                            messagesList.addAll(messageMap.values());

                            Collections.sort(messagesList, (m1, m2) -> {
                                if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                                if (m1.getTimestamp() == null) return -1;
                                if (m2.getTimestamp() == null) return 1;
                                return m1.getTimestamp().compareTo(m2.getTimestamp());
                            });

                            messageAdapter.setMessages(new ArrayList<>(messagesList)); // Pass a new list
                            if (!messagesList.isEmpty()) {
                                recyclerViewMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                            }
                        }
                    } else if (snapshots.isEmpty() && messagesList.isEmpty()) {
                        Log.d(TAG, "No messages found in chatRoomId: " + chatRoomId);
                    }
                });
    }

    private void sendMessage() {
        String messageText = editTextMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null || chatPartnerId == null || chatRoomId == null || chatRoomId.equals("error_chat_room_id")) {
            Toast.makeText(this, "Chat session not properly initialized. Cannot send message.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "sendMessage: Critical chat session details missing. chatRoomId: " + chatRoomId);
            return;
        }

        if (currentUserName == null || currentUserName.isEmpty() || currentUserName.equals("You (Name N/A)") || currentUserName.equals("You (Error)") || currentUserName.startsWith("User ")) {
            Toast.makeText(this, "Cannot send message: Your user details are not fully loaded. Please wait or try restarting the chat.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "sendMessage: currentUserName is not properly set or is a fallback: " + currentUserName);
            if (currentUserName.equals("You (Name N/A)") || currentUserName.equals("You (Error)") || currentUserName.startsWith("User ")){
                fetchCurrentUserNameAndLoadMessages();
            }
            return;
        }

        Map<String, Object> chatRoomData = new HashMap<>();
        chatRoomData.put("lastMessageText", messageText);
        chatRoomData.put("lastMessageTimestamp", FieldValue.serverTimestamp());
        chatRoomData.put("lastMessageSenderId", currentUserId);

        List<String> participantIdsList = new ArrayList<>(Arrays.asList(currentUserId, chatPartnerId));
        Collections.sort(participantIdsList); 
        chatRoomData.put("participantIds", participantIdsList);

        Map<String, String> participantNamesMap = new HashMap<>();
        participantNamesMap.put(currentUserId, currentUserName);
        participantNamesMap.put(chatPartnerId, chatPartnerName);
        chatRoomData.put("participantNames", participantNamesMap);

        Log.d(TAG, "Attempting to set/merge chat_rooms document: " + chatRoomId + " with data: " + chatRoomData.toString());
        db.collection("chat_rooms").document(chatRoomId)
                .set(chatRoomData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Chat room document ensured/updated for chatRoomId: " + chatRoomId);

                    DirectMessage message = new DirectMessage(
                            currentUserId,
                            chatPartnerId,
                            currentUserName,
                            messageText
                    );
                    // Log the message object that is about to be sent
                    Log.d(TAG, "Message Object to be sent: senderId=" + message.getSenderId() + ", text=" + message.getMessageText());


                    Log.d(TAG, "Attempting to add message to subcollection for chatRoomId: " + chatRoomId);
                    db.collection("chat_rooms").document(chatRoomId)
                            .collection("messages")
                            .add(message)
                            .addOnSuccessListener(documentReference -> {
                                editTextMessageInput.setText("");
                                Log.d(TAG, "Message sent successfully to subcollection: " + documentReference.getId());
                            })
                            .addOnFailureListener(e_msg -> {
                                Toast.makeText(ChatActivity.this, "Failed to send message. Please check logs.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error sending message to subcollection for chatRoomId: " + chatRoomId, e_msg);
                            });
                })
                .addOnFailureListener(e_room -> {
                    Toast.makeText(ChatActivity.this, "Failed to initiate or update chat session. Please check logs.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error ensuring chat room document for chatRoomId: " + chatRoomId, e_room);
                });
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
        if (messagesListener != null) {
            messagesListener.remove();
            Log.d(TAG, "Messages listener removed for chatRoomId: " + chatRoomId);
        }
    }
}