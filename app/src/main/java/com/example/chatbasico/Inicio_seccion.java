package com.example.chatbasico;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Inicio_seccion extends AppCompatActivity {

    EditText nombre;
    EditText contrasena;
    Button iniciar_seccion;
    TextView texto_registrarse;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inicio_seccion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Referenciar elementos del layout
        nombre = findViewById(R.id.nombre_inicioSeccion);
        contrasena = findViewById(R.id.Contasena_inicioSeccion);
        iniciar_seccion = findViewById(R.id.Boton_iniciarSeccion);
        texto_registrarse = findViewById(R.id.texto_registrarse);

        // Configurar click listener para el botón de iniciar sesión
        iniciar_seccion.setOnClickListener(v -> {
            String nombreUsuario = nombre.getText().toString().trim();
            String password = contrasena.getText().toString().trim();
            
            if (validarEntradas(nombreUsuario, password)) {
                iniciarSesionFirebase(nombreUsuario, password);
            }
        });

        // Configurar click listener para ir al registro
        texto_registrarse.setOnClickListener(v -> {
            Intent intent = new Intent(Inicio_seccion.this, registrarte.class);
            startActivity(intent);
        });
    }

    private boolean validarEntradas(String nombreUsuario, String password) {
        if (nombreUsuario.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa tu nombre de usuario", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa tu contraseña", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    private void iniciarSesionFirebase(String nombreUsuario, String password) {
        // Generar el email basado en el nombre del usuario (mismo formato que en el registro)
        String email = nombreUsuario.toLowerCase().replaceAll("\\s+", "") + "@chatbasico.com";
        
        // Mostrar loading
        Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show();
        iniciar_seccion.setEnabled(false);
        
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    iniciar_seccion.setEnabled(true);
                    
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(Inicio_seccion.this, 
                                "¡Bienvenido " + nombreUsuario + "!", 
                                Toast.LENGTH_LONG).show();
                            
                            // Navegar a la pantalla principal del chat
                            Intent intent = new Intent(Inicio_seccion.this, MainChats.class);
                            intent.putExtra("nombre_usuario", nombreUsuario);
                            startActivity(intent);
                            finish(); // Cierra esta actividad
                        }
                    } else {
                        // Error al iniciar sesión
                        String errorMessage = "Usuario o contraseña incorrecta";
                        Toast.makeText(Inicio_seccion.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // La verificación de autenticación automática se maneja ahora en PantallaCarga
        // para evitar redirecciones duplicadas
    }
}