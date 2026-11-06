package com.citomoltd.banglagoststory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.citomoltd.banglagoststory.R;

import java.util.List;

public class TextChunkAdapter extends RecyclerView.Adapter<TextChunkAdapter.TextChunkViewHolder> {

    private final Context context;
    private final List<String> textChunks;
    private float textSize = 18f;
    private int textColor = 0;

    public TextChunkAdapter(Context context, List<String> textChunks) {
        this.context = context;
        this.textChunks = textChunks;
    }

    public void setTextColor(int color) {
        this.textColor = color;
        notifyDataSetChanged();
    }

    public void setTextSize(float newSize) {
        if (Math.abs(this.textSize - newSize) > 0.1f) {
            this.textSize = newSize;
            notifyDataSetChanged();
        }
    }

    public void setTextSizeWithAnimation(float newSize) {
        setTextSize(newSize);
    }

    public float getTextSize() {
        return textSize;
    }

    @NonNull
    @Override
    public TextChunkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_chunk, parent, false);
        return new TextChunkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TextChunkViewHolder holder, int position) {
        holder.chunkTextView.setText(textChunks.get(position));
        holder.chunkTextView.setTextSize(textSize);
        if (textColor != 0) {
            holder.chunkTextView.setTextColor(textColor);
        }
        holder.chunkTextView.setKeyListener(null);
        holder.chunkTextView.setCursorVisible(false);
        holder.chunkTextView.setFocusable(false);
        holder.chunkTextView.setFocusableInTouchMode(false);
    }

    @Override
    public int getItemCount() {
        return textChunks.size();
    }

    static class TextChunkViewHolder extends RecyclerView.ViewHolder {
        TextView chunkTextView;

        TextChunkViewHolder(View itemView) {
            super(itemView);
            chunkTextView = itemView.findViewById(R.id.chunkTextView);
            chunkTextView.setKeyListener(null);
            chunkTextView.setCursorVisible(false);
            chunkTextView.setFocusable(false);
            chunkTextView.setFocusableInTouchMode(false);
        }
    }
}
