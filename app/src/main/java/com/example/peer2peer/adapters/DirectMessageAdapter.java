package com.example.peer2peer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.peer2peer.DirectMessage; // Your DirectMessage model
import com.example.peer2peer.R; // For accessing layout resources
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;

public class DirectMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<DirectMessage> messages;
    private String currentUserId;
    private Context context; // Context can be useful for inflating or other context-specific operations

    public DirectMessageAdapter(Context context, List<DirectMessage> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        DirectMessage message = messages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else { // VIEW_TYPE_RECEIVED
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DirectMessage message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    public void setMessages(List<DirectMessage> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged(); // Or use DiffUtil for better performance
    }

    private String formatTimestamp(Date date) {
        if (date == null) {
            return "";
        }
        // Example format: "10:30 AM"
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    // ViewHolder for sent messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessageText;
        TextView textViewMessageTimestamp;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessageText = itemView.findViewById(R.id.textViewMessageText);
            textViewMessageTimestamp = itemView.findViewById(R.id.textViewMessageTimestamp);
        }

        void bind(DirectMessage message) {
            textViewMessageText.setText(message.getMessageText());
            textViewMessageTimestamp.setText(formatTimestamp(message.getTimestamp()));
        }

        // Helper method for timestamp formatting (could be moved to adapter level if preferred)
        private String formatTimestamp(Date date) {
            if (date == null) {
                return "";
            }
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf.format(date);
        }
    }

    // ViewHolder for received messages
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSenderName;
        TextView textViewMessageText;
        TextView textViewMessageTimestamp;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            textViewMessageText = itemView.findViewById(R.id.textViewMessageText);
            textViewMessageTimestamp = itemView.findViewById(R.id.textViewMessageTimestamp);
        }

        void bind(DirectMessage message) {
            if (message.getSenderName() != null) {
                textViewSenderName.setText(message.getSenderName());
                textViewSenderName.setVisibility(View.VISIBLE);
            } else {
                textViewSenderName.setVisibility(View.GONE);
            }
            textViewMessageText.setText(message.getMessageText());
            textViewMessageTimestamp.setText(formatTimestamp(message.getTimestamp()));
        }

        // Helper method for timestamp formatting
        private String formatTimestamp(Date date) {
            if (date == null) {
                return "";
            }
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf.format(date);
        }
    }
}