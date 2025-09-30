package com.example.chatbasico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class registrarte extends AppCompatActivity {


    EditText nombre;
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

        nombre = findViewById(R.id.nombre_usuario);
        botonContinuar = findViewById(R.id.boton_continuar);

        botonContinuar.setOnClickListener(v -> {
            // Obtener el nombre escrito
            String nombreUsuario = nombre.getText().toString().trim();

            if (nombreUsuario.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa tu nombre", Toast.LENGTH_SHORT).show();
            } else {
                // Aquí guardas el nombre, por ejemplo en SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MisDatos", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("nombre_usuario", nombreUsuario);
                editor.apply();

                Toast.makeText(this, "Nombre guardado: " + nombreUsuario, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this,RegistrarteContrasena.class);
                intent.putExtra("nombre_usuario", nombreUsuario);
                startActivity(intent);
            }
        });

    }
}