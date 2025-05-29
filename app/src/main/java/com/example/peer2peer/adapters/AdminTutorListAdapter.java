package com.example.peer2peer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.peer2peer.R;
import com.example.peer2peer.Tutor; // Using Tutor model

import java.util.List;

public class AdminTutorListAdapter extends RecyclerView.Adapter<AdminTutorListAdapter.AdminTutorViewHolder> {

    private List<Tutor> pendingTutors;
    private OnTutorClickListener listener;

    // Interface for click events - Accepts Tutor object
    public interface OnTutorClickListener {
        void onTutorClick(Tutor tutor); // *** Corrected to Tutor ***
    }

    // Constructor - Accepts List<Tutor> and the corrected listener interface
    public AdminTutorListAdapter(List<Tutor> pendingTutors, OnTutorClickListener listener) {
        this.pendingTutors = pendingTutors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminTutorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_admin_tutor, parent, false);
        return new AdminTutorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminTutorViewHolder holder, int position) {
        Tutor tutor = pendingTutors.get(position);
        holder.bind(tutor, listener);
    }

    @Override
    public int getItemCount() {
        return pendingTutors == null ? 0 : pendingTutors.size();
    }

    // Method to update the list
    public void updateData(List<Tutor> newTutors) {
        this.pendingTutors.clear();
        if (newTutors != null) {
            this.pendingTutors.addAll(newTutors);
        }
        notifyDataSetChanged();
    }

    // ViewHolder Class
    static class AdminTutorViewHolder extends RecyclerView.ViewHolder {
        TextView tutorName;
        // TextView tutorEmail; // Removed reference

        AdminTutorViewHolder(@NonNull View itemView) {
            super(itemView);
            tutorName = itemView.findViewById(R.id.text_admin_tutor_name);
            // tutorEmail = itemView.findViewById(R.id.text_admin_tutor_email); // Removed findViewById
        }

        // Bind method using available Tutor fields
        void bind(final Tutor tutor, final OnTutorClickListener listener) {
            // Combine first and surname for display
            String firstName = tutor.getFirstName() != null ? tutor.getFirstName() : "";
            String surname = tutor.getSurname() != null ? tutor.getSurname() : "";
            String displayName = (firstName + " " + surname).trim();
            tutorName.setText(displayName.isEmpty() ? "Name N/A" : displayName);

            // Email display removed

            // Set click listener - this should now work as listener expects Tutor
            itemView.setOnClickListener(v -> listener.onTutorClick(tutor));
        }
    }
}