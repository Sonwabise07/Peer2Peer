package com.example.peer2peer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // For image loading, if you add profile images later
import com.example.peer2peer.ChatRoomSummary; // Your ChatRoomSummary model
import com.example.peer2peer.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private final Context context;
    private List<ChatRoomSummary> chatRoomSummaries;
    private final OnChatRoomClickListener listener;
    private final SimpleDateFormat timeFormat; // For formatting the last message timestamp

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoomSummary chatRoomSummary);
    }

    public ChatListAdapter(Context context, List<ChatRoomSummary> chatRoomSummaries, OnChatRoomClickListener listener) {
        this.context = context;
        this.chatRoomSummaries = chatRoomSummaries;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault()); // Example: "10:30 AM"
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_conversation, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
        ChatRoomSummary summary = chatRoomSummaries.get(position);
        holder.bind(summary, listener);
    }

    @Override
    public int getItemCount() {
        return chatRoomSummaries == null ? 0 : chatRoomSummaries.size();
    }

    public void updateChatRooms(List<ChatRoomSummary> newChatRooms) {
        this.chatRoomSummaries = newChatRooms;
        notifyDataSetChanged(); // Consider using DiffUtil for better performance with large lists
    }

    private String formatTimestamp(Date date) {
        if (date == null) {
            return "";
        }
        // Check if the date is today, yesterday, or older for more user-friendly display
        // This is a simplified version, more complex logic can be added for "Yesterday", "Day of week" etc.
        long currentTime = System.currentTimeMillis();
        long messageTime = date.getTime();
        long dayInMillis = 24 * 60 * 60 * 1000;

        if (currentTime - messageTime < dayInMillis &&
                new Date(currentTime).getDate() == new Date(messageTime).getDate()) { // Same day
            return timeFormat.format(date);
        } else if (currentTime - messageTime < 2 * dayInMillis &&
                new Date(currentTime - dayInMillis).getDate() == new Date(messageTime).getDate()) { // Yesterday
            return "Yesterday";
        } else { // Older than yesterday
            SimpleDateFormat oldDateFormat = new SimpleDateFormat("MMM d", Locale.getDefault()); // Example: "May 16"
            return oldDateFormat.format(date);
        }
    }

    class ChatListViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewChatPartnerProfile;
        TextView textViewChatPartnerName;
        TextView textViewLastMessage;
        TextView textViewLastMessageTimestamp;
        // View viewUnreadIndicator; // If you add unread indicator

        ChatListViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewChatPartnerProfile = itemView.findViewById(R.id.imageView_chat_partner_profile);
            textViewChatPartnerName = itemView.findViewById(R.id.textView_chat_partner_name);
            textViewLastMessage = itemView.findViewById(R.id.textView_last_message);
            textViewLastMessageTimestamp = itemView.findViewById(R.id.textView_last_message_timestamp);
            // viewUnreadIndicator = itemView.findViewById(R.id.view_unread_indicator); // If added
        }

        void bind(final ChatRoomSummary summary, final OnChatRoomClickListener clickListener) {
            textViewChatPartnerName.setText(summary.getOtherParticipantName());
            textViewLastMessage.setText(summary.getLastMessageText());
            textViewLastMessageTimestamp.setText(formatTimestamp(summary.getLastMessageTimestamp()));

            // Load profile image using Glide (optional, if you have URLs)
            if (summary.getOtherParticipantProfileImageUrl() != null && !summary.getOtherParticipantProfileImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(summary.getOtherParticipantProfileImageUrl())
                        .placeholder(R.drawable.ic_person) // Default placeholder
                        .error(R.drawable.ic_person)       // Error placeholder
                        .circleCrop()
                        .into(imageViewChatPartnerProfile);
            } else {
                // Set default placeholder if no image URL
                imageViewChatPartnerProfile.setImageResource(R.drawable.ic_person);
            }

            // Handle unread indicator visibility (optional)
            // if (summary.getUnreadCount() > 0) {
            //    viewUnreadIndicator.setVisibility(View.VISIBLE);
            // } else {
            //    viewUnreadIndicator.setVisibility(View.GONE);
            // }

            itemView.setOnClickListener(v -> clickListener.onChatRoomClick(summary));
        }
    }
}