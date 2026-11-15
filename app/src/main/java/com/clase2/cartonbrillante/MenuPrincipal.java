package com.clase2.cartonbrillante;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MenuPrincipal extends AppCompatActivity {

    BottomNavigationView bottomNavigation;
    Spinner spFiltro;
    TextView tvBadgeRegulacionGlobal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu_principal);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvBadgeRegulacionGlobal = findViewById(R.id.tvBadgeRegulacionGlobal);
        mostrarRegulacionLigadaAlRegistro();

        spFiltro = findViewById(R.id.spFiltro);
        String[] opcionesFiltro = {"Global (Todos)", "Avanzado", "Time Wizard", "Speed Duel", "Rush Duel"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opcionesFiltro);
        spFiltro.setAdapter(adapter);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment fragmentoSeleccionado = null;

            if (itemId == R.id.nav_menu) {
                fragmentoSeleccionado = new InicioFragment();
            } else if (itemId == R.id.nav_scaner) {
                fragmentoSeleccionado = new ScanerFragment();
            } else if (itemId == R.id.nav_carpeta_ventas) {
                fragmentoSeleccionado = new CarpetaVentasFragment();
            } else if (itemId == R.id.nav_decks) {
                fragmentoSeleccionado = new DecksFragment();
            }

            if (fragmentoSeleccionado != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragmentoSeleccionado)
                        .commit();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            String destino = getIntent().getStringExtra("abrir_destino");
            if ("carpeta_ventas".equals(destino)) {
                bottomNavigation.setSelectedItemId(R.id.nav_carpeta_ventas);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new InicioFragment())
                        .commit();
            }
        }
    }

    private void mostrarRegulacionLigadaAlRegistro() {
        String pais = RegulacionHelper.obtenerPaisUsuario(this);
        String regulacion = RegulacionHelper.obtenerRegulacionPorPais(pais);

        tvBadgeRegulacionGlobal.setText(pais + " • " + regulacion);
        if ("OCG".equals(regulacion)) {
            tvBadgeRegulacionGlobal.setBackgroundColor(Color.parseColor("#D32F2F"));
        } else {
            tvBadgeRegulacionGlobal.setBackgroundColor(Color.parseColor("#5C6BC0"));
        }
    }
}