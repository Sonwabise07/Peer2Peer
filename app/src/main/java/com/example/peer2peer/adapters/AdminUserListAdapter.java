package com.example.peer2peer.adapters; // Adjust package name if needed

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.peer2peer.R;
import com.example.peer2peer.Tutor; // Using Tutor model for now

import java.util.ArrayList;
import java.util.List;

public class AdminUserListAdapter extends RecyclerView.Adapter<AdminUserListAdapter.UserViewHolder> {

    private List<Tutor> userList; // Using Tutor model for now
    private Context context;
    private OnUserClickListener listener; // Listener for item clicks

    // Interface for click events
    public interface OnUserClickListener {
        void onUserClick(Tutor user);
    }

    public AdminUserListAdapter(Context context, OnUserClickListener listener) {
        this.context = context;
        this.userList = new ArrayList<>();
        this.listener = listener;
    }

    // Method to update the list of users in the adapter
    public void setUsers(List<Tutor> users) {
        this.userList = users;
        notifyDataSetChanged(); // Notify RecyclerView to refresh
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each list item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_user_admin, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        // Get the user at the current position
        Tutor user = userList.get(position);
        // Bind the user data to the ViewHolder's views
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        // Return the total number of users in the list
        return userList.size();
    }

    // ViewHolder class holds references to the views in list_item_user_admin.xml
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textEmail;
        TextView textRole;
        TextView textStatus;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find the views by their IDs from the item layout
            textName = itemView.findViewById(R.id.text_user_item_name);
            textEmail = itemView.findViewById(R.id.text_user_item_email);
            textRole = itemView.findViewById(R.id.text_user_item_role);
            textStatus = itemView.findViewById(R.id.text_user_item_status);
        }

        // Method to bind data to the views for a specific user
        void bind(final Tutor user, final OnUserClickListener listener) {
            // Combine first and last name, handling potential nulls
            String firstName = user.getFirstName() != null ? user.getFirstName() : "";
            String surname = user.getSurname() != null ? user.getSurname() : "";
            String fullName = (firstName + " " + surname).trim();
            textName.setText(fullName.isEmpty() ? "[No Name]" : fullName);

            textEmail.setText(user.getEmail() != null ? user.getEmail() : "[No Email]");

            // Display Role (capitalize first letter)
            String role = user.getRole(); // Assuming Tutor model has getRole() or we add it
            if (role != null && !role.isEmpty()) {
                textRole.setText("Role: " + role.substring(0, 1).toUpperCase() + role.substring(1));
                textRole.setVisibility(View.VISIBLE);
            } else {
                textRole.setVisibility(View.GONE); // Hide if role is missing
            }

            // Display Status (using profileStatus, capitalize first letter)
            String status = user.getProfileStatus();
            if (status != null && !status.isEmpty()) {
                textStatus.setText("Status: " + status.substring(0, 1).toUpperCase() + status.substring(1).replace("_", " "));
                textStatus.setVisibility(View.VISIBLE);
            } else {
                // Maybe show a default status like "Active" for tutees if profileStatus is null/empty?
                if ("tutee".equalsIgnoreCase(role)) {
                    textStatus.setText("Status: Active");
                    textStatus.setVisibility(View.VISIBLE);
                } else {
                    textStatus.setVisibility(View.GONE); // Hide otherwise
                }
            }

            // Set the click listener for the entire item view
            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
    }

    // --- Add getRole() method to Tutor.java if it doesn't exist ---
    // Open Tutor.java and add:
    // private String role; // Add this field if not present
    // public String getRole() { return role; } // Add this getter
    // Remember to handle it in the Parcelable implementation too!
}