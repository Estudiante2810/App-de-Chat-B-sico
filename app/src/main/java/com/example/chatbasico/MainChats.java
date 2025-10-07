package com.example.chatbasico;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class MainChats extends AppCompatActivity implements UsersAdapter.OnUserClickListener {

    private static final String TAG = "MainChats";

    // Variables de la interfaz
    private TextView textUsername;
    private ImageView imageSignOut;
    private RecyclerView conversationRecyclerView;
    private ProgressBar progressBar;

    // Variables para Firebase y datos
    private FirebaseAuth mAuth;
    private FirebaseFirestore database;
    private String nombreUsuario;

    // Lista de usuarios y adapter
    private List<Usuario> usuarios;
    private UsersAdapter usersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_chats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Obtener el nombre del usuario del Intent
        Intent intent = getIntent();
        nombreUsuario = intent.getStringExtra("nombre_usuario");

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        // Inicializar componentes
        init();
        loadUserDetails();
        getToken();
        setListeners();
        loadUsers();

        // NUEVO: Iniciar listener de notificaciones
        startNotificationListener();
    }

    /**
     * NUEVO MÉTODO: Inicia el listener de notificaciones de Firestore
     */
    private void startNotificationListener() {
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "No se puede iniciar listener: usuario no autenticado");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "=== INICIANDO LISTENER DE NOTIFICACIONES ===");
        Log.d(TAG, "Usuario ID: " + userId);

        database.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error escuchando notificaciones", error);
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "📬 Cambios detectados en notificaciones. Total: " + snapshots.getDocumentChanges().size());

                        snapshots.getDocumentChanges().forEach(change -> {
                            if (change.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                String senderName = change.getDocument().getString("senderName");
                                String messageText = change.getDocument().getString("messageText");

                                Log.d(TAG, "🔔 Nueva notificación de: " + senderName);
                                Log.d(TAG, "Mensaje: " + messageText);

                                // Mostrar notificación local
                                String notificationId = change.getDocument().getId();
                                NotificationHelper.showLocalNotification(
                                    this,
                                    senderName + " te envió un mensaje",
                                    messageText,
                                    notificationId
                                );

                                // Marcar como leída
                                change.getDocument().getReference().update("read", true)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✅ Notificación marcada como leída");
                                    });
                            }
                        });
                    }
                });

        Log.d(TAG, "✅ Listener de notificaciones iniciado correctamente");
    }

    private void init() {
        Log.d(TAG, "=== Inicializando componentes ===");

        // Inicializar vistas
        textUsername = findViewById(R.id.textUsername);
        imageSignOut = findViewById(R.id.imageSignOut);
        conversationRecyclerView = findViewById(R.id.conversationRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        Log.d(TAG, "Vistas inicializadas:");
        Log.d(TAG, "- textUsername: " + (textUsername != null ? "OK" : "NULL"));
        Log.d(TAG, "- imageSignOut: " + (imageSignOut != null ? "OK" : "NULL"));
        Log.d(TAG, "- conversationRecyclerView: " + (conversationRecyclerView != null ? "OK" : "NULL"));
        Log.d(TAG, "- progressBar: " + (progressBar != null ? "OK" : "NULL"));

        // Inicializar lista de usuarios
        usuarios = new ArrayList<>();
        Log.d(TAG, "Lista de usuarios inicializada");

        // Inicializar adapter
        usersAdapter = new UsersAdapter(usuarios, this);
        Log.d(TAG, "Adapter inicializado: " + (usersAdapter != null ? "OK" : "NULL"));

        // Configurar RecyclerView
        if (conversationRecyclerView != null) {
            conversationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            conversationRecyclerView.setAdapter(usersAdapter);
            Log.d(TAG, "RecyclerView configurado con adapter");
        } else {
            Log.e(TAG, "ERROR: conversationRecyclerView es null!");
        }

        Log.d(TAG, "=== Inicialización completada ===");
    }

    private void setListeners() {
        if (imageSignOut != null) {
            imageSignOut.setOnClickListener(v -> signOut());
        }
    }

    private void loadUserDetails() {
        if (textUsername != null && nombreUsuario != null) {
            textUsername.setText(nombreUsuario);
        }
    }

    private void loadUsers() {
        Log.d(TAG, "=== Iniciando carga de usuarios ===");

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (conversationRecyclerView != null) {
            conversationRecyclerView.setVisibility(View.GONE);
        }

        // Verificar autenticación
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "ERROR: Usuario no autenticado");
            showToast("Error: Usuario no autenticado");
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Usuario actual ID: " + currentUserId);
        Log.d(TAG, "Consultando colección 'users' en Firestore...");

        // Cargar usuarios desde Firestore
        database.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "Consulta completada. Éxito: " + task.isSuccessful());

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d(TAG, "Documentos encontrados: " + task.getResult().size());

                        usuarios.clear();
                        int usuariosAgregados = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "--- Procesando documento ---");
                            Log.d(TAG, "ID del documento: " + document.getId());
                            Log.d(TAG, "Datos: " + document.getData().toString());

                            // No mostrar el usuario actual en la lista
                            if (!document.getId().equals(currentUserId)) {
                                Usuario usuario = new Usuario();
                                usuario.id = document.getString("id");
                                usuario.nombre = document.getString("nombre");
                                usuario.email = document.getString("email");

                                Log.d(TAG, "Usuario procesado - ID: " + usuario.id + ", Nombre: " + usuario.nombre);

                                if (usuario.nombre != null && !usuario.nombre.isEmpty()) {
                                    usuarios.add(usuario);
                                    usuariosAgregados++;
                                    Log.d(TAG, "Usuario agregado correctamente");
                                } else {
                                    Log.w(TAG, "Usuario con nombre vacío, no agregado");
                                }
                            } else {
                                Log.d(TAG, "Usuario actual excluido de la lista");
                            }
                        }

                        Log.d(TAG, "Total usuarios en lista: " + usuarios.size());
                        Log.d(TAG, "Usuarios agregados en esta carga: " + usuariosAgregados);

                        if (usersAdapter != null) {
                            usersAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Adapter notificado de cambios");
                        } else {
                            Log.e(TAG, "ERROR: Adapter es null");
                        }

                        if (conversationRecyclerView != null) {
                            conversationRecyclerView.setVisibility(View.VISIBLE);
                            Log.d(TAG, "RecyclerView visible");
                        }

                        showToast("Usuarios cargados: " + usuarios.size());
                    } else {
                        // Error en la consulta
                        String errorMsg = "Error desconocido";
                        if (task.getException() != null) {
                            errorMsg = task.getException().getMessage();
                            Log.e(TAG, "Error en consulta Firestore: " + errorMsg);
                            task.getException().printStackTrace();
                        }

                        showToast("Error al cargar usuarios: " + errorMsg);

                        if (conversationRecyclerView != null) {
                            conversationRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                });

        Log.d(TAG, "=== Consulta Firestore iniciada ===");
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            DocumentReference documentReference = database.collection("users")
                    .document(userId);

            // Primero obtenemos el documento actual para verificar si ya tiene tokens guardados
            documentReference.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        List<String> fcmTokens = new ArrayList<>();

                        // Si ya existe una lista de tokens, la recuperamos
                        if (documentSnapshot.contains("fcmTokens")) {
                            List<String> existingTokens = (List<String>) documentSnapshot.get("fcmTokens");
                            if (existingTokens != null) {
                                fcmTokens.addAll(existingTokens);
                            }
                        }

                        // Verificamos si el token ya existe en la lista para evitar duplicados
                        if (!fcmTokens.contains(token)) {
                            fcmTokens.add(token);

                            // Actualizamos el documento con la lista actualizada
                            documentReference.update("fcmTokens", fcmTokens)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Token agregado a la lista de FCM tokens: " + token);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error al actualizar la lista de tokens", e);
                                    });

                            // También actualizamos el campo fcmToken individual por compatibilidad
                            documentReference.update("fcmToken", token);
                        } else {
                            Log.d(TAG, "Token ya existe en la lista de dispositivos de este usuario");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al obtener documento de usuario para actualizar tokens", e);
                    });
        }
    }

    private void signOut() {
        showToast("Cerrando sesión...");

        if (mAuth != null) {
            mAuth.signOut();
            Intent intent = new Intent(getApplicationContext(), Inicio_seccion.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }



    public void onUserClick(Usuario usuario) {
        Log.d(TAG, "Click en usuario: " + usuario.nombre + " (ID: " + usuario.id + ")");
        showToast("Abriendo chat con " + usuario.nombre);

        // Abrir chat con este usuario
        Intent intent = new Intent(this, chats.class);
        intent.putExtra("usuario_seleccionado", usuario.nombre);
        intent.putExtra("usuario_id", usuario.id);
        intent.putExtra("current_user_name", nombreUsuario);
        startActivity(intent);
    }

    // Clase simple para representar un usuario
    public static class Usuario {
        public String id;
        public String nombre;
        public String email;
        public List<String> fcmTokens;

        public Usuario() {
            // Default constructor required for Firestore
            fcmTokens = new ArrayList<>();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public List<String> getFcmTokens() {
            return fcmTokens != null ? fcmTokens : new ArrayList<>();
        }

        public void setFcmTokens(List<String> fcmTokens) {
            this.fcmTokens = fcmTokens;
        }

        public void addFcmToken(String token) {
            if (fcmTokens == null) {
                fcmTokens = new ArrayList<>();
            }
            if (token != null && !fcmTokens.contains(token)) {
                fcmTokens.add(token);
            }
        }

        public void removeFcmToken(String token) {
            if (fcmTokens != null) {
                fcmTokens.remove(token);
            }
        }

        public String getFcmToken() {
            // For backward compatibility, return the first token if available
            return fcmTokens != null && !fcmTokens.isEmpty() ? fcmTokens.get(0) : null;
        }
    }
}
