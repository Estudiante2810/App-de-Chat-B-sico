package com.example.chatbasico;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    
    private List<MainChats.Usuario> usuarios;
    private OnUserClickListener listener;
    
    public interface OnUserClickListener {
        void onUserClick(MainChats.Usuario usuario);
    }
    
    public UsersAdapter(List<MainChats.Usuario> usuarios, OnUserClickListener listener) {
        this.usuarios = usuarios;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        MainChats.Usuario usuario = usuarios.get(position);
        holder.bind(usuario);
    }
    
    @Override
    public int getItemCount() {
        return usuarios.size();
    }
    
    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textNombre;
        
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textNombre = itemView.findViewById(R.id.textNombre);
        }
        
        void bind(MainChats.Usuario usuario) {
            textNombre.setText(usuario.nombre);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(usuario);
                }
            });
        }
    }
}