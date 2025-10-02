package com.example.chatbasico;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PantallaCarga extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private static final int SPLASH_DELAY = 3000; // 3 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pantalla_carga);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Esperar un poco para mostrar la pantalla de carga, luego verificar autenticación
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuthenticationAndRedirect();
            }
        }, SPLASH_DELAY);
    }

    private void checkAuthenticationAndRedirect() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        if (currentUser != null) {
            // Usuario está logueado, ir directamente a MainChats
            redirectToMainChats(currentUser);
        } else {
            // Usuario no está logueado, ir a pantalla de inicio de sesión
            redirectToLogin();
        }
    }

    private void redirectToMainChats(FirebaseUser user) {
        Intent intent = new Intent(PantallaCarga.this, MainChats.class);
        
        // Obtener el nombre del usuario desde el email
        String userName = user.getEmail();
        if (userName != null && userName.contains("@")) {
            // Extraer el nombre del usuario del email (antes del @)
            userName = userName.substring(0, userName.indexOf("@"));
            // Formatear el nombre para que se vea mejor
            userName = formatUserName(userName);
        } else {
            userName = "Usuario"; // Valor por defecto
        }
        
        intent.putExtra("nombre_usuario", userName);
        startActivity(intent);
        finish();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(PantallaCarga.this, Inicio_seccion.class);
        startActivity(intent);
        finish();
    }

    private String formatUserName(String userName) {
        if (userName == null || userName.isEmpty()) {
            return "Usuario";
        }
        
        // Capitalizar la primera letra
        return userName.substring(0, 1).toUpperCase() + userName.substring(1).toLowerCase();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verificar si Firebase Auth está listo
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
    }
}