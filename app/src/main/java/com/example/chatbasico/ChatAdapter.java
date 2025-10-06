package com.example.chatbasico;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT_TEXT = 1;
    private static final int VIEW_TYPE_RECEIVED_TEXT = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;
    
    private List<Message> messages;
    private String currentUserId;
    private SimpleDateFormat dateFormat;
    
    public ChatAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);
        boolean isImage = message.isImageMessage();
        
        if (isSent && isImage) {
            return VIEW_TYPE_SENT_IMAGE;
        } else if (isSent && !isImage) {
            return VIEW_TYPE_SENT_TEXT;
        } else if (!isSent && isImage) {
            return VIEW_TYPE_RECEIVED_IMAGE;
        } else {
            return VIEW_TYPE_RECEIVED_TEXT;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_SENT_TEXT:
                View sentTextView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageViewHolder(sentTextView);
                
            case VIEW_TYPE_RECEIVED_TEXT:
                View receivedTextView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageViewHolder(receivedTextView);
                
            case VIEW_TYPE_SENT_IMAGE:
                View sentImageView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_image_sent, parent, false);
                return new SentImageViewHolder(sentImageView);
                
            case VIEW_TYPE_RECEIVED_IMAGE:
                View receivedImageView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_image_received, parent, false);
                return new ReceivedImageViewHolder(receivedImageView);
                
            default:
                View defaultView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageViewHolder(defaultView);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        
        switch (getItemViewType(position)) {
            case VIEW_TYPE_SENT_TEXT:
                ((SentMessageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_RECEIVED_TEXT:
                ((ReceivedMessageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_SENT_IMAGE:
                ((SentImageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_RECEIVED_IMAGE:
                ((ReceivedImageViewHolder) holder).bind(message);
                break;
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    // ViewHolder para mensajes enviados
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textDateTime;
        
        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textDateTime = itemView.findViewById(R.id.textDateTime);
        }
        
        void bind(Message message) {
            textMessage.setText(message.getText());
            
            if (message.getTimestamp() != null) {
                textDateTime.setText(dateFormat.format(message.getTimestamp()));
            } else {
                textDateTime.setText("Enviando...");
            }
        }
    }
    
    // ViewHolder para mensajes recibidos
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textSenderName;
        TextView textMessage;
        TextView textDateTime;
        
        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textSenderName = itemView.findViewById(R.id.textSenderName);
            textMessage = itemView.findViewById(R.id.textMessage);
            textDateTime = itemView.findViewById(R.id.textDateTime);
        }
        
        void bind(Message message) {
            textSenderName.setText(message.getSenderName());
            textMessage.setText(message.getText());
            
            if (message.getTimestamp() != null) {
                textDateTime.setText(dateFormat.format(message.getTimestamp()));
            } else {
                textDateTime.setText("...");
            }
        }
    }
    
    // ViewHolder para imágenes enviadas
    class SentImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageMessage;
        TextView textDateTime;
        ProgressBar progressBar;
        
        SentImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageMessage = itemView.findViewById(R.id.imageMessage);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
        
        void bind(Message message) {
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                progressBar.setVisibility(View.GONE);
                Glide.with(itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(imageMessage);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                imageMessage.setImageResource(R.drawable.ic_image);
            }
            
            if (message.getTimestamp() != null) {
                textDateTime.setText(dateFormat.format(message.getTimestamp()));
            } else {
                textDateTime.setText("Enviando...");
            }
        }
    }
    
    // ViewHolder para imágenes recibidas
    class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        TextView textSenderName;
        ImageView imageMessage;
        TextView textDateTime;
        ProgressBar progressBar;
        
        ReceivedImageViewHolder(@NonNull View itemView) {
            super(itemView);
            textSenderName = itemView.findViewById(R.id.textSenderName);
            imageMessage = itemView.findViewById(R.id.imageMessage);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
        
        void bind(Message message) {
            textSenderName.setText(message.getSenderName());
            
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                progressBar.setVisibility(View.GONE);
                Glide.with(itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(imageMessage);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                imageMessage.setImageResource(R.drawable.ic_image);
            }
            
            if (message.getTimestamp() != null) {
                textDateTime.setText(dateFormat.format(message.getTimestamp()));
            } else {
                textDateTime.setText("...");
            }
        }
    }
    
    // Método para actualizar la lista de mensajes
    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }
    
    // Método para agregar un mensaje nuevo
    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
}