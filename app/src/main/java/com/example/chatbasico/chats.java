package com.example.chatbasico;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class chats extends AppCompatActivity {

    private static final String TAG = "ChatsActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    // Variables de la interfaz
    private TextView textUsername;
    private ImageView imageSignOut;
    private RecyclerView conversationRecyclerView;
    private EditText chatMessageInput;
    private ImageButton messageSendBtn;
    private ImageButton imageSelectBtn;
    
    // Variables para el chat
    private String receiverUserId;
    private String receiverUserName;
    private String currentUserId;
    private String currentUserName;
    private String conversationId;
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore database;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    
    // Adaptador y lista de mensajes
    private List<Message> messages;
    private ChatAdapter chatAdapter;
    
    // Para seleccionar imágenes
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        
        // Inicializar selector de imágenes
        initImagePicker();
        
        // Obtener datos del Intent
        getIntentData();
        
        // Inicializar vistas
        initViews();
        
        // Configurar RecyclerView
        setupRecyclerView();
        
        // Configurar listeners
        setListeners();
        
        // Cargar mensajes
        loadMessages();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        receiverUserId = intent.getStringExtra("usuario_id");
        receiverUserName = intent.getStringExtra("usuario_seleccionado");
        currentUserName = intent.getStringExtra("current_user_name");
        
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }
        
        // Generar ID de conversación
        if (currentUserId != null && receiverUserId != null) {
            conversationId = Message.generateConversationId(currentUserId, receiverUserId);
        }
        
        Log.d(TAG, "Datos del chat:");
        Log.d(TAG, "- Usuario actual: " + currentUserName + " (ID: " + currentUserId + ")");
        Log.d(TAG, "- Usuario receptor: " + receiverUserName + " (ID: " + receiverUserId + ")");
        Log.d(TAG, "- ID conversación: " + conversationId);
    }

    private void initViews() {
        textUsername = findViewById(R.id.textUsername);
        imageSignOut = findViewById(R.id.imageSignOut);
        conversationRecyclerView = findViewById(R.id.conversationRecyclerView);
        chatMessageInput = findViewById(R.id.chat_message_input);
        messageSendBtn = findViewById(R.id.message_send_btn);
        imageSelectBtn = findViewById(R.id.image_select_btn);
        
        // Mostrar el nombre del usuario con quien se está chateando
        if (receiverUserName != null) {
            textUsername.setText("Chat con " + receiverUserName);
        }
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages, currentUserId);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Para que los mensajes nuevos aparezcan abajo
        
        conversationRecyclerView.setLayoutManager(layoutManager);
        conversationRecyclerView.setAdapter(chatAdapter);
        conversationRecyclerView.setVisibility(View.VISIBLE);
    }

    private void setListeners() {
        messageSendBtn.setOnClickListener(v -> sendMessage());
        
        imageSelectBtn.setOnClickListener(v -> selectImage());
        
        imageSignOut.setOnClickListener(v -> {
            finish(); // Regresar a la pantalla anterior
        });
        
        // También permitir enviar mensaje con Enter
        chatMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String messageText = chatMessageInput.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (conversationId == null || currentUserId == null || receiverUserId == null) {
            Toast.makeText(this, "Error: Datos de conversación inválidos", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Crear el mensaje
        Message message = new Message(
            messageText,
            currentUserId,
            currentUserName,
            receiverUserId,
            receiverUserName,
            conversationId
        );
        
        // Crear un Map para enviar a Firestore
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("text", message.getText());
        messageMap.put("senderId", message.getSenderId());
        messageMap.put("senderName", message.getSenderName());
        messageMap.put("receiverId", message.getReceiverId());
        messageMap.put("receiverName", message.getReceiverName());
        messageMap.put("conversationId", message.getConversationId());
        messageMap.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        // Guardar en Firestore
        database.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(messageMap)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Mensaje enviado con ID: " + documentReference.getId());
                    chatMessageInput.setText(""); // Limpiar el input
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al enviar mensaje", e);
                    Toast.makeText(this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        if (conversationId == null) {
            Log.e(TAG, "No se puede cargar mensajes: conversationId es null");
            return;
        }
        
        Log.d(TAG, "Cargando mensajes para conversación: " + conversationId);
        
        // Escuchar mensajes en tiempo real
        database.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Error al escuchar mensajes", error);
                            return;
                        }
                        
                        if (value != null) {
                            Log.d(TAG, "Cambios en mensajes detectados");
                            
                            for (DocumentChange documentChange : value.getDocumentChanges()) {
                                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                    // Nuevo mensaje
                                    Message message = documentChange.getDocument().toObject(Message.class);
                                    message.setId(documentChange.getDocument().getId());
                                    
                                    Log.d(TAG, "Nuevo mensaje: " + message.getText());
                                    chatAdapter.addMessage(message);
                                    
                                    // Scroll al último mensaje
                                    conversationRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verificar autenticación
        if (mAuth.getCurrentUser() == null) {
            // Redirigir al login si no está autenticado
            Intent intent = new Intent(this, Inicio_seccion.class);
            startActivity(intent);
            finish();
        }
    }
    
    // Métodos para manejar imágenes
    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadImageToFirebase(imageUri);
                    }
                }
            }
        );
    }
    
    private void selectImage() {
        if (checkPermissions()) {
            openImagePicker();
        } else {
            requestPermissions();
        }
    }
    
    private boolean checkPermissions() {
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int mediaImagesPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES);
        
        // Para Android 13+ (API 33+) solo necesitamos READ_MEDIA_IMAGES
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return mediaImagesPermission == PackageManager.PERMISSION_GRANTED;
        } else {
            return readPermission == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permiso necesario para seleccionar imágenes", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Seleccionar imagen"));
    }
    
    private void uploadImageToFirebase(Uri imageUri) {
        if (conversationId == null || currentUserId == null || receiverUserId == null) {
            Toast.makeText(this, "Error: Datos de conversación inválidos", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar progreso
        Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show();
        
        // Crear referencia única para la imagen
        String imageId = UUID.randomUUID().toString();
        String imagePath = "chat_images/" + conversationId + "/" + imageId + ".jpg";
        StorageReference imageRef = storageReference.child(imagePath);
        
        // Subir imagen
        imageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                // Obtener URL de descarga
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    // Crear mensaje de imagen
                    Message imageMessage = new Message(
                        currentUserId,
                        currentUserName,
                        receiverUserId,
                        receiverUserName,
                        conversationId,
                        downloadUri.toString(),
                        imageId + ".jpg"
                    );
                    
                    // Crear Map para Firestore
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("senderId", imageMessage.getSenderId());
                    messageMap.put("senderName", imageMessage.getSenderName());
                    messageMap.put("receiverId", imageMessage.getReceiverId());
                    messageMap.put("receiverName", imageMessage.getReceiverName());
                    messageMap.put("conversationId", imageMessage.getConversationId());
                    messageMap.put("messageType", Message.TYPE_IMAGE);
                    messageMap.put("imageUrl", imageMessage.getImageUrl());
                    messageMap.put("imageName", imageMessage.getImageName());
                    messageMap.put("text", "");
                    messageMap.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    
                    // Guardar en Firestore
                    database.collection("conversations")
                            .document(conversationId)
                            .collection("messages")
                            .add(messageMap)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Imagen enviada con ID: " + documentReference.getId());
                                Toast.makeText(this, "Imagen enviada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al enviar imagen", e);
                                Toast.makeText(this, "Error al enviar imagen", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener URL de descarga", e);
                    Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show();
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al subir imagen", e);
                Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
            });
    }
}