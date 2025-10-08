package com.example.chatbasico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class registrarte extends AppCompatActivity {

    EditText emailInput;
    Button botonContinuar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registrarte);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar vistas
        emailInput = findViewById(R.id.nombre_usuario);
        botonContinuar = findViewById(R.id.boton_continuar);

        // Configurar el botón continuar
        botonContinuar.setOnClickListener(v -> validarYContinuar());

    }

    private void validarYContinuar() {
        String email = emailInput.getText().toString().trim();

        // Validar que el campo no esté vacío
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa tu email", Toast.LENGTH_SHORT).show();
            emailInput.requestFocus();
            return;
        }

        // Validar formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor ingresa un email válido", Toast.LENGTH_SHORT).show();
            emailInput.requestFocus();
            return;
        }

        // Validar que termine en un dominio conocido
        if (!esEmailValido(email)) {
            Toast.makeText(this, "Por favor ingresa un email con un dominio válido (ej: @gmail.com, @yahoo.com)", Toast.LENGTH_LONG).show();
            emailInput.requestFocus();
            return;
        }

        // Si llegamos aquí, el email es válido
        guardarEmailYContinuar(email);
    }

    private boolean esEmailValido(String email) {
        // Lista de dominios comunes válidos
        String[] dominiosValidos = {
            "@gmail.com", "@yahoo.com", "@hotmail.com", "@outlook.com",
            "@live.com", "@icloud.com", "@protonmail.com", "@aol.com"
        };

        String emailLower = email.toLowerCase();
        for (String dominio : dominiosValidos) {
            if (emailLower.endsWith(dominio)) {
                return true;
            }
        }
        return false;
    }

    private void guardarEmailYContinuar(String email) {
        // Guardar email en SharedPreferences
        SharedPreferences preferencias = getSharedPreferences("chat_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferencias.edit();
        editor.putString("email_usuario", email);
        editor.apply();

        // Continuar a la siguiente pantalla
        Intent intent = new Intent(registrarte.this, RegistrarteContrasena.class);
        intent.putExtra("email", email);
        startActivity(intent);
    }
}