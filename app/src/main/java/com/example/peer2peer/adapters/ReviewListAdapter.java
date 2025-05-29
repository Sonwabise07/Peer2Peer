package com.example.peer2peer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.peer2peer.R;
import com.example.peer2peer.Review; // Make sure your Review model is imported
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReviewListAdapter extends RecyclerView.Adapter<ReviewListAdapter.ReviewViewHolder> {

    private List<Review> reviewList;
    private Context context; // Optional: For formatting or resources if needed
    private SimpleDateFormat dateFormatter;

    public ReviewListAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
        // Consider a more concise date format for reviews
        this.dateFormatter = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.bind(review, dateFormatter);
    }

    @Override
    public int getItemCount() {
        return reviewList == null ? 0 : reviewList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        RatingBar ratingBar;
        TextView commentTextView;
        TextView tuteeNameTextView;
        TextView timestampTextView;

        ReviewViewHolder(View itemView) {
            super(itemView);
            ratingBar = itemView.findViewById(R.id.review_rating_bar);
            commentTextView = itemView.findViewById(R.id.review_comment_text);
            tuteeNameTextView = itemView.findViewById(R.id.review_tutee_name_text);
            timestampTextView = itemView.findViewById(R.id.review_timestamp_text);
        }

        void bind(Review review, SimpleDateFormat dateFormatter) {
            if (review == null) return;

            ratingBar.setRating(review.getRating());
            commentTextView.setText(review.getComment());
            // Display Tutee Name, fallback to "Anonymous" if null/empty
            String name = review.getTuteeName();
            tuteeNameTextView.setText("- " + (name != null && !name.trim().isEmpty() ? name : "Anonymous"));

            // Format and display Timestamp
            Timestamp ts = review.getTimestamp();
            if (ts != null) {
                timestampTextView.setText(dateFormatter.format(ts.toDate()));
            } else {
                timestampTextView.setText(""); // Hide if no timestamp
            }

            // Show/hide comment view based on content
            commentTextView.setVisibility(TextUtils.isEmpty(review.getComment()) ? View.GONE : View.VISIBLE);
        }
    }
}