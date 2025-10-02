package com.example.chatbasico;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistrarteContrasena extends AppCompatActivity {

    String nombre;
    EditText contrasena;
    Button botonFinalizar;
    FirebaseAuth mAuth;
    FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registrarte_contrasena);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        nombre = intent.getStringExtra("nombre_usuario");

        contrasena = findViewById(R.id.Contasena_usuario);
        botonFinalizar = findViewById(R.id.Boton_finalizar);

        botonFinalizar.setOnClickListener(v -> {
            String password = contrasena.getText().toString().trim();
            
            if (password.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa una contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (password.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (nombre == null || nombre.isEmpty()) {
                Toast.makeText(this, "Error: No se pudo obtener el nombre del usuario", Toast.LENGTH_SHORT).show();
                return;
            }

            crearUsuarioFirebase(nombre, password);
        });

    }

    private void crearUsuarioFirebase(String nombreUsuario, String password) {
        String email = nombreUsuario.toLowerCase().replaceAll("\\s+", "") + "@chatbasico.com";
        
        // Mostrar loading
        Toast.makeText(this, "Creando usuario...", Toast.LENGTH_SHORT).show();
        botonFinalizar.setEnabled(false);
        
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    botonFinalizar.setEnabled(true);
                    
                    if (task.isSuccessful()) {
                        // Usuario creado exitosamente
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Guardar usuario en Firestore
                            guardarUsuarioEnFirestore(user.getUid(), nombreUsuario, email);
                            Intent intent = new Intent(this, Inicio_seccion.class);
                            startActivity(intent);
                        }
                    } else {
                        // Error al crear usuario
                        String errorMessage = "Error al crear usuario";
                        if (task.getException() != null) {
                            String exception = task.getException().getMessage();
                            if (exception != null) {
                                if (exception.contains("email address is already in use")) {
                                    errorMessage = "Este nombre de usuario ya está en uso";
                                } else if (exception.contains("weak password")) {
                                    errorMessage = "La contraseña es muy débil";
                                } else {
                                    errorMessage = "Error: " + exception;
                                }
                            }
                        }
                        Toast.makeText(RegistrarteContrasena.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarUsuarioEnFirestore(String userId, String nombreUsuario, String email) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("id", userId);
        usuario.put("nombre", nombreUsuario);
        usuario.put("email", email);
        usuario.put("fcmToken", ""); // Se actualizará después
        usuario.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        database.collection("users")
                .document(userId)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegistrarteContrasena.this, 
                        "Usuario creado exitosamente: " + nombreUsuario, 
                        Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegistrarteContrasena.this, 
                        "Error al guardar datos del usuario", 
                        Toast.LENGTH_LONG).show();
                });
    }
}