package com.example.peer2peer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.peer2peer.adapters.ChatAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";

    private RecyclerView recyclerViewChatMessages;
    private EditText editTextChatMessage;
    private FloatingActionButton buttonSendChatMessage;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessageList;

    // For REST API communication
    private RequestQueue requestQueue;
    private String dialogflowProjectId = ""; // Will be fetched from service account JSON
    private String sessionUUID = "session_" + UUID.randomUUID().toString(); // Dialogflow session ID
    private String currentAccessToken = null;

    // For background tasks (like getting access token) and UI updates
    private ExecutorService executorService;
    private Handler mainThreadHandler;

    // Dialogflow REST API v2 endpoint
    // Format: https://dialogflow.googleapis.com/v2/projects/<PROJECT_ID>/agent/sessions/<SESSION_ID>:detectIntent
    private String DIALOGFLOW_DETECT_INTENT_URL_FORMAT = "https://dialogflow.googleapis.com/v2/projects/%s/agent/sessions/%s:detectIntent";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        Toolbar toolbar = findViewById(R.id.toolbar_chatbot);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Help Chat");
        }

        recyclerViewChatMessages = findViewById(R.id.recyclerView_chat_messages);
        editTextChatMessage = findViewById(R.id.editText_chat_message);
        buttonSendChatMessage = findViewById(R.id.button_send_chat_message);

        chatMessageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewChatMessages.setLayoutManager(layoutManager);
        recyclerViewChatMessages.setAdapter(chatAdapter);

        requestQueue = Volley.newRequestQueue(this);
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        // Fetch Project ID from service account JSON and then fetch initial access token
        loadProjectIdAndFetchToken();

        buttonSendChatMessage.setOnClickListener(v -> {
            String message = editTextChatMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessageToChat(message, ChatMessage.SENDER_USER);
                sendMessageToDialogflowRest(message);
                editTextChatMessage.setText("");
            } else {
                Toast.makeText(ChatbotActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProjectIdAndFetchToken() {
        executorService.execute(() -> {
            try {
                InputStream stream = getResources().openRawResource(R.raw.dialogflow_service_account_credentials); // Ensure this filename matches!
                GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                        .createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform", "https://www.googleapis.com/auth/dialogflow"));

                if (credentials instanceof ServiceAccountCredentials) {
                    dialogflowProjectId = ((ServiceAccountCredentials) credentials).getProjectId();
                    if (dialogflowProjectId == null || dialogflowProjectId.isEmpty()) {
                        Log.e(TAG, "Failed to get Project ID from service account JSON.");
                        mainThreadHandler.post(() -> Toast.makeText(ChatbotActivity.this, "Chatbot configuration error (Project ID).", Toast.LENGTH_LONG).show());
                        return;
                    }
                    Log.i(TAG, "Dialogflow Project ID loaded: " + dialogflowProjectId);
                    fetchAccessToken(credentials); // Fetch initial token
                } else {
                    Log.e(TAG, "Credentials are not ServiceAccountCredentials.");
                    mainThreadHandler.post(() -> Toast.makeText(ChatbotActivity.this, "Chatbot configuration error (Credentials type).", Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading service account credentials: " + e.getMessage(), e);
                mainThreadHandler.post(() -> Toast.makeText(ChatbotActivity.this, "Error loading chatbot credentials.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void fetchAccessToken(GoogleCredentials credentials) {
        executorService.execute(() -> {
            try {
                credentials.refreshIfExpired();
                currentAccessToken = credentials.getAccessToken().getTokenValue();
                Log.i(TAG, "Access token fetched successfully.");
                // You could potentially send a welcome event to Dialogflow here if needed
                // For example, trigger the welcome intent:
                // mainThreadHandler.post(() -> sendMessageToDialogflowRest("Hi"));
            } catch (IOException e) {
                Log.e(TAG, "Error fetching access token: " + e.getMessage(), e);
                currentAccessToken = null;
                mainThreadHandler.post(() -> Toast.makeText(ChatbotActivity.this, "Chatbot auth error. Please try again.", Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void addMessageToChat(String messageText, String senderType) {
        mainThreadHandler.post(() -> {
            ChatMessage chatMessage = new ChatMessage(messageText, senderType);
            chatMessageList.add(chatMessage);
            chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
            recyclerViewChatMessages.scrollToPosition(chatMessageList.size() - 1);
        });
    }

    private void sendMessageToDialogflowRest(final String userMessage) {
        if (dialogflowProjectId.isEmpty()) {
            Log.e(TAG, "Dialogflow Project ID is not set. Cannot send message.");
            addMessageToChat("Chatbot is not configured (Error: No Project ID).", ChatMessage.SENDER_BOT);
            return;
        }
        if (currentAccessToken == null) {
            Log.e(TAG, "Access token is null. Attempting to refresh.");
            // Attempt to re-fetch token and then send message (or queue message)
            // For simplicity here, just show error. A more robust app might queue and retry.
            addMessageToChat("Chatbot auth error. Please wait a moment and try again.", ChatMessage.SENDER_BOT);
            loadProjectIdAndFetchToken(); // Re-trigger token fetch
            return;
        }

        String url = String.format(DIALOGFLOW_DETECT_INTENT_URL_FORMAT, dialogflowProjectId, sessionUUID);

        JSONObject queryInputJson = new JSONObject();
        JSONObject textInputJson = new JSONObject();
        try {
            textInputJson.put("text", userMessage);
            textInputJson.put("languageCode", "en-US"); // Ensure this matches your agent's language
            queryInputJson.put("text", textInputJson);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating queryInput JSON: " + e.getMessage(), e);
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("queryInput", queryInputJson);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating requestBody JSON: " + e.getMessage(), e);
            return;
        }

        Log.d(TAG, "Sending REST request to Dialogflow: " + url);
        Log.d(TAG, "Request Body: " + requestBody.toString());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Dialogflow REST Response: " + response.toString());
                        try {
                            JSONObject queryResult = response.getJSONObject("queryResult");
                            String fulfillmentText = queryResult.getString("fulfillmentText");

                            if (fulfillmentText.isEmpty()) {
                                addMessageToChat("Sorry, I didn't quite get that.", ChatMessage.SENDER_BOT);
                            } else {
                                addMessageToChat(fulfillmentText, ChatMessage.SENDER_BOT);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing Dialogflow JSON response: " + e.getMessage(), e);
                            addMessageToChat("There was an issue understanding the bot's response.", ChatMessage.SENDER_BOT);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley Error sending message to Dialogflow: " + error.toString(), error);
                        String errorMessage = "Error connecting to chatbot. Please check your connection or try again later.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                Log.e(TAG, "Volley Error Body: " + responseBody);
                                // Try to parse more specific error from Dialogflow
                                JsonObject errorJson = JsonParser.parseString(responseBody).getAsJsonObject();
                                if (errorJson.has("error") && errorJson.getAsJsonObject("error").has("message")) {
                                    errorMessage = "Chatbot Error: " + errorJson.getAsJsonObject("error").get("message").getAsString();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing Volley error response body", e);
                            }
                        }
                        addMessageToChat(errorMessage, ChatMessage.SENDER_BOT);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + currentAccessToken);
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG); // Cancel Volley requests
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            Log.d(TAG, "ExecutorService shutdown.");
        }
    }
}