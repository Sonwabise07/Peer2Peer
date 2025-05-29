package com.example.peer2peer.adapters; // Your adapters package

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.R; // Import R from your main package
import com.example.peer2peer.TimeSlot; // Import the TimeSlot model

import java.util.List;

// Renamed adapter specifically for ManageAvailabilityActivity
public class ManageAvailabilityTimeSlotAdapter extends RecyclerView.Adapter<ManageAvailabilityTimeSlotAdapter.ManageAvailabilityViewHolder> {

    private List<TimeSlot> timeSlots;
    private final Context context;
    private final OnManageAvailabilitySlotListener listener; // Renamed listener interface

    // Renamed Interface for handling remove clicks in ManageAvailabilityActivity
    public interface OnManageAvailabilitySlotListener {
        void onRemoveClick(String documentId); // Method signature remains the same
    }

    // Constructor using the renamed listener
    public ManageAvailabilityTimeSlotAdapter(Context context, List<TimeSlot> timeSlots, OnManageAvailabilitySlotListener listener) {
        this.context = context;
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ManageAvailabilityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout (assuming list_item_time_slot.xml is still correct)
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_time_slot, parent, false);
        return new ManageAvailabilityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageAvailabilityViewHolder holder, int position) {
        TimeSlot currentTimeSlot = timeSlots.get(position);
        // Pass data and the specific listener interface to ViewHolder
        holder.bind(currentTimeSlot, listener);
    }

    @Override
    public int getItemCount() {
        return timeSlots == null ? 0 : timeSlots.size();
    }

    /**
     * Updates the data list for the adapter.
     * Uses the original method name 'updateData'.
     * @param newTimeSlots The new list of TimeSlot objects.
     */
    public void updateData(List<TimeSlot> newTimeSlots) {
        this.timeSlots.clear();
        if (newTimeSlots != null) {
            this.timeSlots.addAll(newTimeSlots);
        }
        notifyDataSetChanged(); // Refresh the RecyclerView
    }

    // --- ViewHolder Inner Class (Renamed) ---
    public static class ManageAvailabilityViewHolder extends RecyclerView.ViewHolder {
        TextView textTimeSlot;
        ImageButton buttonRemoveSlot; // Keep the remove button

        public ManageAvailabilityViewHolder(@NonNull View itemView) {
            super(itemView);
            textTimeSlot = itemView.findViewById(R.id.text_time_slot);
            buttonRemoveSlot = itemView.findViewById(R.id.button_remove_slot);
        }

        /**
         * Binds data to the views and sets up the remove listener.
         * Uses the specific listener interface for ManageAvailability.
         * @param timeSlot The TimeSlot data for this item.
         * @param listener The listener interface to call back to the Activity/Fragment.
         */
        public void bind(final TimeSlot timeSlot, final OnManageAvailabilitySlotListener listener) {
            // Make sure getFormattedTimeRange() exists in your TimeSlot model
            if (timeSlot != null) {
                textTimeSlot.setText(timeSlot.getFormattedTimeRange());
            } else {
                textTimeSlot.setText("Invalid slot data");
                Log.w("MAViewHolder", "TimeSlot object was null in bind");
            }


            // Ensure remove button is visible and set listener
            buttonRemoveSlot.setVisibility(View.VISIBLE);
            buttonRemoveSlot.setOnClickListener(v -> {
                // Check listener and ensure timeslot/documentId are not null
                if (listener != null && timeSlot != null && timeSlot.getDocumentId() != null) {
                    // Call the listener's method, passing the document ID
                    listener.onRemoveClick(timeSlot.getDocumentId());
                } else {
                    Log.e("MAViewHolder", "Listener or TimeSlot/DocumentId was null on remove click.");
                    // Optionally show a toast to the user?
                }
            });
        }
    }
}