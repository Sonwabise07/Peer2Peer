package com.example.peer2peer.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.peer2peer.R; // Your R file
import com.example.peer2peer.Resource; // Your Resource model
import java.util.List;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

    private static final String TAG = "ResourceAdapter";
    private Context context;
    private List<Resource> resourceList;

    public ResourceAdapter(Context context, List<Resource> resourceList) {
        this.context = context;
        this.resourceList = resourceList;
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_resource, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        Resource resource = resourceList.get(position);

        if (resource == null) {
            Log.e(TAG, "Resource object is null at position: " + position);
            // Hide all views or set to default to prevent crashes
            holder.textViewTitle.setText("Error: Null Resource");
            holder.textViewDescription.setVisibility(View.GONE);
            holder.textViewModule.setVisibility(View.GONE);
            holder.textViewFileName.setVisibility(View.GONE);
            holder.iconResourceType.setImageResource(R.drawable.ic_file); // Default error icon
            holder.buttonViewResource.setOnClickListener(null); // Remove listener
            return;
        }

        holder.textViewTitle.setText(TextUtils.isEmpty(resource.getTitle()) ? "Untitled Resource" : resource.getTitle());
        holder.textViewDescription.setText(TextUtils.isEmpty(resource.getDescription()) ? "No description." : resource.getDescription());

        if (!TextUtils.isEmpty(resource.getModuleCode())) {
            holder.textViewModule.setText("Module: " + resource.getModuleCode());
            holder.textViewModule.setVisibility(View.VISIBLE);
        } else {
            holder.textViewModule.setVisibility(View.GONE);
        }

        // Show file name if available and it's a file type
        if ("file".equalsIgnoreCase(resource.getType()) && !TextUtils.isEmpty(resource.getFileName())) {
            holder.textViewFileName.setText("File: " + resource.getFileName());
            holder.textViewFileName.setVisibility(View.VISIBLE);
        } else {
            holder.textViewFileName.setVisibility(View.GONE);
        }

        // Set icon based on type or mimeType
        String type = resource.getType();
        String mimeType = resource.getMimeType();

        if ("link".equalsIgnoreCase(type)) {
            holder.iconResourceType.setImageResource(R.drawable.ic_link); // You'll need ic_link.xml
        } else if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                holder.iconResourceType.setImageResource(R.drawable.ic_image_file); // You'll need ic_image_file.xml
            } else if (mimeType.equals("application/pdf")) {
                holder.iconResourceType.setImageResource(R.drawable.ic_pdf_file); // You'll need ic_pdf_file.xml

            } else { // Other file types
                holder.iconResourceType.setImageResource(R.drawable.ic_file); // Default file icon
            }
        } else { // Default if mimeType is null but type is not "link"
            holder.iconResourceType.setImageResource(R.drawable.ic_file);
        }

        holder.buttonViewResource.setOnClickListener(v -> {
            String url = resource.getUrl();
            if (!TextUtils.isEmpty(url)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    // Check if there's an app to handle this intent
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "No application found to open this resource.", Toast.LENGTH_LONG).show();
                        Log.w(TAG, "No app found to handle URL: " + url);
                    }
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, "No application found to open this resource.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ActivityNotFoundException for URL: " + url, e);
                } catch (Exception e) {
                    Toast.makeText(context, "Could not open resource. Invalid URL or resource type.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Could not open resource URL: " + url, e);
                }
            } else {
                Toast.makeText(context, "Resource URL is not available.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Resource URL is empty or null for resource: " + resource.getTitle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return resourceList != null ? resourceList.size() : 0;
    }

    public void setResources(List<Resource> newResources) {
        this.resourceList.clear();
        if (newResources != null) {
            this.resourceList.addAll(newResources);
        }
        notifyDataSetChanged(); // Consider more efficient notify methods if performance becomes an issue
    }

    static class ResourceViewHolder extends RecyclerView.ViewHolder {
        ImageView iconResourceType;
        TextView textViewTitle;
        TextView textViewDescription;
        TextView textViewModule;
        TextView textViewFileName;
        ImageView buttonViewResource; // Corrected variable name

        public ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            iconResourceType = itemView.findViewById(R.id.image_resource_type_icon);
            textViewTitle = itemView.findViewById(R.id.text_resource_title);
            textViewDescription = itemView.findViewById(R.id.text_resource_description);
            textViewModule = itemView.findViewById(R.id.text_resource_module);
            textViewFileName = itemView.findViewById(R.id.text_resource_file_name);
            buttonViewResource = itemView.findViewById(R.id.button_view_resource); // Corrected ID
        }
    }
}