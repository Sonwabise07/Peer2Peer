package com.example.peer2peer.adapters; // Use your package name

import android.content.Context;
import android.text.TextUtils;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.peer2peer.R; // Ensure R is imported
import com.example.peer2peer.Tutor;

import java.util.List;
import java.util.Locale;

public class TutorListAdapter extends RecyclerView.Adapter<TutorListAdapter.TutorViewHolder> {

    private List<Tutor> tutorList; // This list will be updated by setTutors
    private Context context;
    private OnItemClickListener listener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(Tutor tutor);
    }

    // Constructor
    public TutorListAdapter(Context context, List<Tutor> tutorList, OnItemClickListener listener) {
        this.context = context;
        // It's safer to work with a copy or initialize here if needed,
        // but assigning the reference is okay as long as setTutors replaces it.
        this.tutorList = tutorList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TutorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_tutor, parent, false);
        return new TutorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorViewHolder holder, int position) {
        // Check bounds just in case
        if (tutorList != null && position >= 0 && position < tutorList.size()) {
            Tutor currentTutor = tutorList.get(position);
            holder.bind(currentTutor, listener, context);
        } else {
            Log.e("TutorListAdapter", "Invalid position or tutorList is null in onBindViewHolder: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return tutorList == null ? 0 : tutorList.size();
    }

    // --- *** ADD THIS METHOD *** ---
    /**
     * Updates the list of tutors displayed by the adapter.
     * @param tutors The new list of tutors to display.
     */
    public void setTutors(List<Tutor> tutors) {
        // Replace the existing list with the new one
        this.tutorList = tutors;
        // Notify the RecyclerView that the entire dataset has changed
        notifyDataSetChanged();
        Log.d("TutorListAdapter", "Adapter data updated. New size: " + (tutors != null ? tutors.size() : 0));
    }
    // --- *** END OF ADDED METHOD *** ---

    // --- ViewHolder Inner Class ---
    public static class TutorViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProfile;
        TextView textViewName;
        TextView textViewModules;
        TextView textViewRate;
        RatingBar ratingBar;
        TextView textViewRatingValue;

        public TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfile = itemView.findViewById(R.id.image_view_tutor_profile);
            textViewName = itemView.findViewById(R.id.text_view_tutor_name);
            textViewModules = itemView.findViewById(R.id.text_view_tutor_modules);
            textViewRate = itemView.findViewById(R.id.text_view_tutor_rate);
            ratingBar = itemView.findViewById(R.id.rating_bar_tutor);
            textViewRatingValue = itemView.findViewById(R.id.text_view_tutor_rating_value);
        }

        public void bind(final Tutor tutor, final OnItemClickListener listener, Context context) {
            // Set Name
            String firstName = tutor.getFirstName() != null ? tutor.getFirstName() : "";
            String surname = tutor.getSurname() != null ? tutor.getSurname() : "";
            // Use the pre-constructed fullName if available (from Activity)
            String fullName = tutor.getFullName() != null ? tutor.getFullName() : (firstName + " " + surname).trim();
            textViewName.setText(fullName.isEmpty() ? "Tutor Name N/A" : fullName);

            // Set Modules
            List<String> modules = tutor.getModulesToTutor();
            if (modules != null && !modules.isEmpty()) {
                textViewModules.setText("Modules: " + TextUtils.join(", ", modules));
                textViewModules.setVisibility(View.VISIBLE);
            } else {
                textViewModules.setText("No modules listed");
                textViewModules.setVisibility(View.VISIBLE);
            }

            // Set Hourly Rate
            Double rate = tutor.getHourlyRate();
            if (rate != null) {
                textViewRate.setText(String.format(Locale.getDefault(), "R %.2f /h", rate));
                textViewRate.setVisibility(View.VISIBLE);
            } else {
                textViewRate.setText("Rate N/A");
                textViewRate.setVisibility(View.VISIBLE);
            }

            // --- Set Rating ---
            Double averageRating = tutor.getAverageRating();
            Long ratingCount = tutor.getRatingCount();

            ratingBar.setVisibility(View.VISIBLE);

            if (averageRating != null) {
                ratingBar.setRating(averageRating.floatValue());
                String ratingValueStr = String.format(Locale.getDefault(), "%.1f", averageRating);
                if (ratingCount != null && ratingCount > 0) {
                    // Using a simple format, replace with R.string if needed
                    ratingValueStr += String.format(Locale.getDefault(), " (%d)", ratingCount);
                }
                textViewRatingValue.setText(ratingValueStr);
                textViewRatingValue.setVisibility(View.VISIBLE);
            } else {
                ratingBar.setRating(0f);
                textViewRatingValue.setText(context.getString(R.string.no_ratings_yet)); // Assumes this string resource exists
                textViewRatingValue.setVisibility(View.VISIBLE);
            }


            // Load Profile Image using Glide
            String imageUrl = tutor.getProfileImageUrl();
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person) // Your placeholder drawable
                    .error(R.drawable.ic_person) // Your error drawable
                    .circleCrop()
                    .into(imageViewProfile);

            // Set click listener on the item view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(tutor);
                }
            });
        }
    }
}