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

public class RegistrarteContrasena extends AppCompatActivity {

    String nombre;
    EditText contrasena;
    Button botonFinalizar;
    FirebaseAuth mAuth;

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

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Obtener el nombre del usuario desde el Intent
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
            
            // Crear usuario en Firebase
            crearUsuarioFirebase(nombre, password);
        });

    }

    private void crearUsuarioFirebase(String nombreUsuario, String password) {
        // Generar un email único basado en el nombre del usuario
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
                            Toast.makeText(RegistrarteContrasena.this, 
                                "Usuario creado exitosamente: " + nombreUsuario, 
                                Toast.LENGTH_LONG).show();
                            
                            // Aquí puedes navegar a la siguiente pantalla
                            // Por ejemplo, regresar al login o ir directamente al chat
                            finish(); // Cierra esta actividad
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
}