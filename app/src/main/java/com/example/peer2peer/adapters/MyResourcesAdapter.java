package com.example.peer2peer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.R;
import com.example.peer2peer.Resource; // Your Resource model

import java.text.SimpleDateFormat;
import java.util.ArrayList; // Import ArrayList
import java.util.List;
import java.util.Locale;
// Ensure com.google.firebase.Timestamp is available if needed directly, though it's through Resource model
// import com.google.firebase.Timestamp;


public class MyResourcesAdapter extends RecyclerView.Adapter<MyResourcesAdapter.ResourceViewHolder> {

    private List<Resource> resourceList;
    private final Context context;
    private final OnResourceActionsListener listener;

    public interface OnResourceActionsListener {
        void onDeleteClick(Resource resource, int position);
    }

    public MyResourcesAdapter(Context context, List<Resource> resourceList, OnResourceActionsListener listener) {
        this.context = context;
        // Initialize with a new ArrayList to prevent issues if the passed list is modified elsewhere or is null
        this.resourceList = new ArrayList<>(resourceList != null ? resourceList : new ArrayList<>());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_my_resource, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        Resource resource = resourceList.get(position);
        holder.bind(resource, listener, position);
    }

    @Override
    public int getItemCount() {
        return resourceList == null ? 0 : resourceList.size();
    }

    public void submitList(List<Resource> newResources) {
        this.resourceList.clear();
        if (newResources != null) {
            this.resourceList.addAll(newResources);
        }
        notifyDataSetChanged();
    }

    static class ResourceViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewResourceIcon;
        TextView textViewResourceTitle;
        TextView textViewResourceModule;
        TextView textViewResourceTypeDetails;
        TextView textViewResourceDate;
        ImageButton buttonDeleteResource;

        ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewResourceIcon = itemView.findViewById(R.id.image_view_resource_icon);
            textViewResourceTitle = itemView.findViewById(R.id.text_view_resource_title);
            textViewResourceModule = itemView.findViewById(R.id.text_view_resource_module);
            textViewResourceTypeDetails = itemView.findViewById(R.id.text_view_resource_type_details);
            textViewResourceDate = itemView.findViewById(R.id.text_view_resource_date);
            buttonDeleteResource = itemView.findViewById(R.id.button_delete_resource);
        }

        void bind(final Resource resource, final OnResourceActionsListener listener, final int position) {
            if (resource == null) return;

            textViewResourceTitle.setText(resource.getTitle());
            textViewResourceModule.setText("Module: " + (resource.getModuleCode() != null ? resource.getModuleCode() : "N/A"));

            if ("file".equalsIgnoreCase(resource.getType())) {
                imageViewResourceIcon.setImageResource(R.drawable.ic_file);
                // Use getFileName() from your Resource.java
                String fileName = resource.getFileName() != null ? resource.getFileName() : "Unnamed file";
                textViewResourceTypeDetails.setText("File: " + fileName);
            } else if ("link".equalsIgnoreCase(resource.getType())) {
                imageViewResourceIcon.setImageResource(R.drawable.ic_link);
                String url = resource.getUrl() != null ? resource.getUrl() : "No URL";
                if (url.length() > 40) {
                    url = url.substring(0, 37) + "...";
                }
                textViewResourceTypeDetails.setText("Link: " + url);
            } else {
                imageViewResourceIcon.setImageResource(R.drawable.ic_info_outline); // A default icon
                textViewResourceTypeDetails.setText("Type: Unknown");
            }

            if (resource.getUploadedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                // Convert Firebase Timestamp to java.util.Date for formatting
                textViewResourceDate.setText("Uploaded: " + sdf.format(resource.getUploadedAt().toDate()));
            } else {
                textViewResourceDate.setText("Uploaded: N/A");
            }

            buttonDeleteResource.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(resource, position);
                }
            });
        }
    }
}