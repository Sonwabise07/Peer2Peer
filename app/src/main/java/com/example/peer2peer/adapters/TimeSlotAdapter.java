package com.example.peer2peer.adapters; // Your adapters package

import android.content.Context;
import android.graphics.Color; // Import Color for potential background tinting
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // For getting colors
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.R; // Import R from your main package
import com.example.peer2peer.TimeSlot; // Import the TimeSlot model
import com.google.android.material.card.MaterialCardView; // Import MaterialCardView

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private List<TimeSlot> timeSlots;
    private final Context context; // Keep context if needed for resources, otherwise can remove
    private final OnTimeSlotSelectedListener selectionListener; // Listener for selection changes
    private int selectedPosition = RecyclerView.NO_POSITION; // Track selected item

    // Interface for notifying the Activity/Fragment about selection
    public interface OnTimeSlotSelectedListener {
        void onSlotSelected(TimeSlot timeSlot); // Pass the selected TimeSlot object
    }

    // Updated Constructor
    public TimeSlotAdapter(List<TimeSlot> timeSlots, OnTimeSlotSelectedListener listener) {
        this.timeSlots = timeSlots;
        this.selectionListener = listener;
        // Context might not be needed anymore unless you load resources here
        this.context = null; // Set null if not used, or remove completely
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        TimeSlot currentTimeSlot = timeSlots.get(position);
        holder.bind(currentTimeSlot, position == selectedPosition); // Pass data and selected state

        // --- Handle Item Click for Selection ---
        holder.itemView.setOnClickListener(v -> {
            if (selectionListener != null) {
                // If already selected, maybe deselect? Optional behavior.
                // For now, selecting another item deselects the previous one.
                if (selectedPosition != holder.getAdapterPosition()) {
                    int previousSelectedPosition = selectedPosition;
                    selectedPosition = holder.getAdapterPosition();

                    // Notify changes for visual update
                    notifyItemChanged(previousSelectedPosition); // Un-highlight previous
                    notifyItemChanged(selectedPosition);      // Highlight new

                    // Call the listener with the newly selected TimeSlot
                    selectionListener.onSlotSelected(timeSlots.get(selectedPosition));
                    Log.d("TimeSlotAdapter", "Selected position: " + selectedPosition);
                }
                // Optional: Allow clicking the same item again to deselect
                /* else {
                    int deselectedPos = selectedPosition;
                    selectedPosition = RecyclerView.NO_POSITION; // Deselect
                    notifyItemChanged(deselectedPos);
                    selectionListener.onSlotSelected(null); // Notify with null or handle differently
                } */
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots == null ? 0 : timeSlots.size();
    }

    /**
     * Updates the data list for the adapter and clears selection.
     * @param newTimeSlots The new list of TimeSlot objects.
     */
    public void updateTimeSlots(List<TimeSlot> newTimeSlots) { // Renamed from updateData
        this.timeSlots.clear();
        if (newTimeSlots != null) {
            this.timeSlots.addAll(newTimeSlots);
        }
        clearSelection(); // Clear selection when data changes
        notifyDataSetChanged(); // Refresh the RecyclerView
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        int previousSelectedPosition = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;
        if (previousSelectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousSelectedPosition); // Update UI of previously selected item
        }
    }

    /**
     * Gets the adapter position of the currently selected item.
     * @return Adapter position or RecyclerView.NO_POSITION if nothing is selected.
     */
    public int getSelectedPosition() {
        return selectedPosition;
    }

    /**
     * Gets the TimeSlot object at the currently selected position.
     * @return TimeSlot object or null if nothing is selected or position is invalid.
     */
    public TimeSlot getSelectedTimeSlot() {
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < timeSlots.size()) {
            return timeSlots.get(selectedPosition);
        }
        return null;
    }

    /**
     * Gets the TimeSlot object at the specified position.
     * @param position The adapter position.
     * @return TimeSlot object or null if position is invalid.
     */
    public TimeSlot getTimeSlotAt(int position) {
        if (position >= 0 && position < timeSlots.size()) {
            return timeSlots.get(position);
        }
        return null;
    }


    // --- ViewHolder Inner Class ---
    public static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView textTimeSlot;
        ImageButton buttonRemoveSlot;
        MaterialCardView cardView; // Reference to the root card view

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView; // Get the root CardView
            textTimeSlot = itemView.findViewById(R.id.text_time_slot);
            buttonRemoveSlot = itemView.findViewById(R.id.button_remove_slot);
        }

        /**
         * Binds data to the views and updates appearance based on selection.
         * @param timeSlot The TimeSlot data for this item.
         * @param isSelected Whether this item is the currently selected one.
         */
        public void bind(final TimeSlot timeSlot, boolean isSelected) {
            textTimeSlot.setText(timeSlot.getFormattedTimeRange());

            // Hide the remove button as it's not used in TutorDetailActivity
            buttonRemoveSlot.setVisibility(View.GONE);

            // --- Update Visual State based on selection ---
            cardView.setChecked(isSelected); // Use the built-in checked state

            // Optional: You could also manually change background color or stroke
            /*
            if (isSelected) {
                // Example: Change background tint (requires Context)
                // cardView.setCardBackgroundColor(ContextCompat.getColor(cardView.getContext(), R.color.selected_slot_background));
                // Example: Change stroke
                 cardView.setStrokeWidth(4); // Set stroke width
                 cardView.setStrokeColor(ContextCompat.getColor(cardView.getContext(), R.color.purple_500)); // Use your theme's colorAccent or a specific color
            } else {
                // cardView.setCardBackgroundColor(ContextCompat.getColor(cardView.getContext(), R.color.white)); // Reset background
                 cardView.setStrokeWidth(0); // Remove stroke
            }
            */
        }
    }
}