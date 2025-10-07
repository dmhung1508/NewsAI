package com.example.newsai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.newsai.R;
import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
    
    private final List<String> imageUrls;
    
    public ImagePagerAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_pager, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        
        // Clear previous image to prevent flash
        holder.imageView.setImageDrawable(null);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.imageView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.color.white)
                    .error(android.R.color.white)
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            holder.imageView.setBackgroundColor(
                holder.imageView.getContext().getResources().getColor(android.R.color.white)
            );
        }
    }
    
    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
