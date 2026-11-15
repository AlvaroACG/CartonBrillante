package com.clase2.cartonbrillante;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class vistaEvento extends AppCompatActivity {

    private TextView tvNombreEventoDetalle, tvRolUsuarioInfo, tvMesaAsignadaInfo;
    private ImageView ivBannerEvento;
    private LinearLayout layoutSeccionJugador, layoutSeccionJuez, layoutSeccionTienda;
    private Button btnVolverDetalle, btnVerMesasGlobales, btnGestionarRondas, btnReportarResultado;

    private String rolActualUsuario = "JUGADOR"; // Puede ser "JUGADOR", "JUEZ", "TIENDA"
    private boolean estaInscrito = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_evento);

        // Vincular vistas
        tvNombreEventoDetalle = findViewById(R.id.tvNombreEventoDetalle);
        tvRolUsuarioInfo = findViewById(R.id.tvRolUsuarioInfo);
        tvMesaAsignadaInfo = findViewById(R.id.tvMesaAsignadaInfo);
        ivBannerEvento = findViewById(R.id.ivBannerEvento);

        layoutSeccionJugador = findViewById(R.id.layoutSeccionJugador);
        layoutSeccionJuez = findViewById(R.id.layoutSeccionJuez);
        layoutSeccionTienda = findViewById(R.id.layoutSeccionTienda);

        btnVolverDetalle = findViewById(R.id.btnVolverDetalle);
        btnVerMesasGlobales = findViewById(R.id.btnVerMesasGlobales);
        btnGestionarRondas = findViewById(R.id.btnGestionarRondas);
        btnReportarResultado = findViewById(R.id.btnReportarResultado);

        btnVolverDetalle.setOnClickListener(v -> finish());

        // Cargar imagen de banner por defecto o placeholder de evento
        ivBannerEvento.setImageResource(R.drawable.logo_carton_go);

        // Configurar visibilidades según el rol
        configurarPermisosYVisibilidad();

        // 🟢 ACCIÓN CLAVE: Tocar la información de la mesa te lleva directo a la vista de mesas
        tvMesaAsignadaInfo.setOnClickListener(v -> {
            Intent intent = new Intent(vistaEvento.this, vistaMesas.class);
            startActivity(intent);
        });

        btnReportarResultado.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reportar Resultado de Mesa")
                    .setMessage("¿Deseas confirmar el resultado de tu enfrentamiento actual?")
                    .setPositiveButton("Victoria (2-0)", (d, w) -> {
                        Toast.makeText(this, "Resultado enviado al Juez", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnVerMesasGlobales.setOnClickListener(v -> {
            Intent intent = new Intent(vistaEvento.this, vistaMesas.class);
            startActivity(intent);
        });

        btnGestionarRondas.setOnClickListener(v -> {
            Toast.makeText(this, "Panel de control de Rondas y Parejamientos (Juez/Tienda)", Toast.LENGTH_LONG).show();
        });
    }

    private void configurarPermisosYVisibilidad() {
        tvRolUsuarioInfo.setText("Rol actual: " + rolActualUsuario);

        layoutSeccionJugador.setVisibility(View.GONE);
        layoutSeccionJuez.setVisibility(View.GONE);
        layoutSeccionTienda.setVisibility(View.GONE);

        if ("TIENDA".equalsIgnoreCase(rolActualUsuario)) {
            layoutSeccionTienda.setVisibility(View.VISIBLE);
            layoutSeccionJuez.setVisibility(View.VISIBLE);
            tvMesaAsignadaInfo.setText("🏢 Panel de Administración de Tienda Oficial • Toca para ver Mesas");

        } else if ("JUEZ".equalsIgnoreCase(rolActualUsuario)) {
            layoutSeccionJuez.setVisibility(View.VISIBLE);
            layoutSeccionJugador.setVisibility(View.VISIBLE);
            tvMesaAsignadaInfo.setText("⚖️ Herramientas de Judge Oficial • Toca para ver Mesas");

        } else {
            if (estaInscrito) {
                layoutSeccionJugador.setVisibility(View.VISIBLE);
                tvMesaAsignadaInfo.setText("🎴 Estás inscrito • Mesa Asignada: #4 • Toca para Ver Mesas");
            } else {
                tvMesaAsignadaInfo.setText("⚠️ No estás inscrito en este torneo.");
                btnReportarResultado.setEnabled(false);
            }
        }
    }
}