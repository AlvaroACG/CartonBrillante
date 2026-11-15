package com.clase2.cartonbrillante;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class vistaMesas extends AppCompatActivity {

    private RecyclerView recyclerMesas;
    private Button btnVolverMesas, btnGenerarRonda;
    private TextView tvTituloEstadoMesas, tvSubtituloRolMesas;
    private View cardPanelControlMesas;

    private String rolActual = "JUGADOR";
    private int mesaAsignadaJugador = 4; // Mesa predeterminada de prueba para jugador

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_mesas);

        btnVolverMesas = findViewById(R.id.btnVolverMesas);
        btnGenerarRonda = findViewById(R.id.btnGenerarRonda);
        recyclerMesas = findViewById(R.id.recyclerMesas);
        tvTituloEstadoMesas = findViewById(R.id.tvTituloEstadoMesas);
        tvSubtituloRolMesas = findViewById(R.id.tvSubtituloRolMesas);
        cardPanelControlMesas = findViewById(R.id.cardPanelControlMesas);

        btnVolverMesas.setOnClickListener(v -> finish());
        recyclerMesas.setLayoutManager(new GridLayoutManager(this, 2));

        // 1. Obtener el ID del usuario activo desde SharedPreferences (guardado al hacer login)
        SharedPreferences prefs = getSharedPreferences("CartonGoPrefs", MODE_PRIVATE);
        int idUsuarioLogueado = prefs.getInt("id_usuario_activo", 1); // 1 por defecto para pruebas iniciales

        // 2. Consultar el rol real directamente a la base de datos SQLite
        DBHelper dbHelper = new DBHelper(this);
        rolActual = dbHelper.obtenerRolUsuarioPorId(idUsuarioLogueado);

        // 3. Aplicar los permisos y visibilidad según el rol obtenido de la tabla
        configurarVistaSegunRol();

        // 4. Acción exclusiva de Jueces y Tiendas para generar rondas
        btnGenerarRonda.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Generar Rondas")
                    .setMessage("¿Deseas calcular los emparejamientos y actualizar la distribución de mesas?")
                    .setPositiveButton("Generar", (dialog, which) -> {
                        Toast.makeText(this, "¡Mesas y emparejamientos generados con éxito!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void configurarVistaSegunRol() {
        cardPanelControlMesas.setVisibility(View.GONE);
        String rolLimpio = rolActual != null ? rolActual.toUpperCase() : "JUGADOR";

        if (rolLimpio.contains("TIENDA") || rolLimpio.contains("ORGANIZADOR")) {
            tvSubtituloRolMesas.setText("Modo Tienda • Administración Global");
            cardPanelControlMesas.setVisibility(View.VISIBLE); // Activa panel inferior
            cargarListaMesas(12, rolLimpio);

        } else if (rolLimpio.contains("JUEZ") || rolLimpio.contains("JUDGE")) {
            tvSubtituloRolMesas.setText("Modo Juez Oficial • Control de Rondas");
            cardPanelControlMesas.setVisibility(View.VISIBLE); // Activa panel inferior
            cargarListaMesas(12, rolLimpio);

        } else {
            tvSubtituloRolMesas.setText("Tu Mesa Asignada: #" + mesaAsignadaJugador);
            cargarListaMesas(10, rolLimpio);
        }
    }

    private void cargarListaMesas(int totalMesas, String rolActual) {
        List<String> listaMesas = new ArrayList<>();
        for (int i = 1; i <= totalMesas; i++) {
            if ("JUGADOR".equalsIgnoreCase(rolActual) && i == mesaAsignadaJugador) {
                listaMesas.add("⭐ MESA #" + i + " (TU TURNO) ⭐");
            } else {
                listaMesas.add("Mesa #" + i + " \nEstado: En Curso ⚔️");
            }
        }
    }
}