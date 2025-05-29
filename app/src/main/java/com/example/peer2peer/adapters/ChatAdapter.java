package com.example.peer2peer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.ChatMessage; // Import your ChatMessage model
import com.example.peer2peer.R;       // Import R class for layout resources

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER_MESSAGE = 1;
    private static final int VIEW_TYPE_BOT_MESSAGE = 2;

    private List<ChatMessage> chatMessageList;

    public ChatAdapter(List<ChatMessage> chatMessageList) {
        this.chatMessageList = chatMessageList;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessageList.get(position);
        if (ChatMessage.SENDER_USER.equals(message.getSenderType())) {
            return VIEW_TYPE_USER_MESSAGE;
        } else { // By default, assume SENDER_BOT or any other type
            return VIEW_TYPE_BOT_MESSAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_USER_MESSAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        } else { // VIEW_TYPE_BOT_MESSAGE
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bot_message, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessageList.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER_MESSAGE) {
            ((UserMessageViewHolder) holder).bind(message);
        } else { // VIEW_TYPE_BOT_MESSAGE
            ((BotMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessageList.size();
    }

    // ViewHolder for User Messages
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textView_user_message);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessageText());
        }
    }

    // ViewHolder for Bot Messages
    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textView_bot_message);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessageText());
        }
    }
}