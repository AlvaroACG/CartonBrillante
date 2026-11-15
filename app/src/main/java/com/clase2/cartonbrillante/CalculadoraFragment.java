package com.clase2.cartonbrillante;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Random;

public class CalculadoraFragment extends Fragment {

    private int lpJ1 = 8000;
    private int lpJ2 = 8000;
    private int victoriasJ1 = 0;
    private int victoriasJ2 = 0;
    private int firmasConfirmadas = 0;
    private boolean esMatchBo3 = true; // true = Mejor de 3 (2 victorias), false = Single (1 victoria)
    private boolean dueloCerrado = false;

    private TextView tvLpJ1, tvLpJ2, tvVictoriasJ1, tvVictoriasJ2, tvHistorial, tvEstadoFirmas;
    private EditText etNombreJ1, etNombreJ2, etMontoLp;
    private Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_vista_calculadora, container, false);

        tvLpJ1 = view.findViewById(R.id.tvLpJ1);
        tvLpJ2 = view.findViewById(R.id.tvLpJ2);
        tvVictoriasJ1 = view.findViewById(R.id.tvVictoriasJ1);
        tvVictoriasJ2 = view.findViewById(R.id.tvVictoriasJ2);
        tvHistorial = view.findViewById(R.id.tvHistorialDuelo);
        tvEstadoFirmas = view.findViewById(R.id.tvEstadoFirmas);

        etNombreJ1 = view.findViewById(R.id.etNombreJ1);
        etNombreJ2 = view.findViewById(R.id.etNombreJ2);
        etMontoLp = view.findViewById(R.id.etMontoLp);

        Button btnVolver = view.findViewById(R.id.btnVolverCalc);
        RadioGroup rgTipoEncuentro = view.findViewById(R.id.rgTipoEncuentro);

        // Botón Volver: sale de la calculadora y restaura pantalla vertical
        btnVolver.setOnClickListener(v -> salirDeCalculadora());

        rgTipoEncuentro.setOnCheckedChangeListener((group, checkedId) -> {
            esMatchBo3 = (checkedId == R.id.rbMatchBo3);
            reiniciarEncuentroCompleto();
        });

        // Botones rápidos de cantidad
        view.findViewById(R.id.btnRapido100).setOnClickListener(v -> etMontoLp.setText("100"));
        view.findViewById(R.id.btnRapido500).setOnClickListener(v -> etMontoLp.setText("500"));
        view.findViewById(R.id.btnRapido1000).setOnClickListener(v -> etMontoLp.setText("1000"));

        // Controles Jugador 1
        view.findViewById(R.id.btnRestarJ1).setOnClickListener(v -> modificarLp(1, -obtenerMonto()));
        view.findViewById(R.id.btnSumarJ1).setOnClickListener(v -> modificarLp(1, obtenerMonto()));
        view.findViewById(R.id.btnMitadJ1).setOnClickListener(v -> dividirMitadLp(1));
        view.findViewById(R.id.btnGanaJuegoJ1).setOnClickListener(v -> otorgarVictoriaJuego(1));

        // Controles Jugador 2
        view.findViewById(R.id.btnRestarJ2).setOnClickListener(v -> modificarLp(2, -obtenerMonto()));
        view.findViewById(R.id.btnSumarJ2).setOnClickListener(v -> modificarLp(2, obtenerMonto()));
        view.findViewById(R.id.btnMitadJ2).setOnClickListener(v -> dividirMitadLp(2));
        view.findViewById(R.id.btnGanaJuegoJ2).setOnClickListener(v -> otorgarVictoriaJuego(2));

        // Dado, Moneda, Reiniciar y Firmar
        view.findViewById(R.id.btnDado).setOnClickListener(v -> {
            int dado = random.nextInt(6) + 1;
            agregarLog("🎲 Dado: salió [" + dado + "]");
            Toast.makeText(getContext(), "🎲 Dado: " + dado, Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnMoneda).setOnClickListener(v -> {
            String cara = random.nextBoolean() ? "CARA" : "SELLO";
            agregarLog("🪙 Moneda: salió [" + cara + "]");
            Toast.makeText(getContext(), "🪙 Moneda: " + cara, Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnReiniciarMatch).setOnClickListener(v -> reiniciarEncuentroCompleto());
        view.findViewById(R.id.btnFirmarResultado).setOnClickListener(v -> registrarFirmaJugador());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            // Forzar pantalla horizontal completa
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            ocultarBarrasMenuPrincipal(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() != null) {
            // Restaurar pantalla vertical y barras al salir
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ocultarBarrasMenuPrincipal(false);
        }
    }

    private void ocultarBarrasMenuPrincipal(boolean ocultar) {
        if (getActivity() == null) return;
        int visibilidad = ocultar ? View.GONE : View.VISIBLE;

        // Oculta la barra superior a través de su contenedor padre
        View badgeRegulacion = getActivity().findViewById(R.id.tvBadgeRegulacionGlobal);
        if (badgeRegulacion != null && badgeRegulacion.getParent() instanceof View) {
            ((View) badgeRegulacion.getParent()).setVisibility(visibilidad);
        }

        // Oculta la barra de navegación inferior
        View bottomNav = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setVisibility(visibilidad);
        }
    }

    private void salirDeCalculadora() {
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ocultarBarrasMenuPrincipal(false);
            getParentFragmentManager().popBackStack();
        }
    }

    private int obtenerMonto() {
        try {
            return Integer.parseInt(etMontoLp.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void modificarLp(int jugador, int cambio) {
        if (dueloCerrado || cambio == 0) return;

        if (jugador == 1) {
            lpJ1 = Math.max(0, lpJ1 + cambio);
            actualizarVistaLp(tvLpJ1, lpJ1);
            agregarLog(etNombreJ1.getText() + ": " + (cambio > 0 ? "+" : "") + cambio + " -> " + lpJ1 + " LP");
            if (lpJ1 == 0) otorgarVictoriaJuego(2);
        } else {
            lpJ2 = Math.max(0, lpJ2 + cambio);
            actualizarVistaLp(tvLpJ2, lpJ2);
            agregarLog(etNombreJ2.getText() + ": " + (cambio > 0 ? "+" : "") + cambio + " -> " + lpJ2 + " LP");
            if (lpJ2 == 0) otorgarVictoriaJuego(1);
        }
    }

    private void dividirMitadLp(int jugador) {
        if (dueloCerrado) return;
        if (jugador == 1) {
            lpJ1 = (lpJ1 + 1) / 2;
            actualizarVistaLp(tvLpJ1, lpJ1);
            agregarLog(etNombreJ1.getText() + ": LP a la mitad -> " + lpJ1 + " LP");
        } else {
            lpJ2 = (lpJ2 + 1) / 2;
            actualizarVistaLp(tvLpJ2, lpJ2);
            agregarLog(etNombreJ2.getText() + ": LP a la mitad -> " + lpJ2 + " LP");
        }
    }

    private void actualizarVistaLp(TextView tv, int lp) {
        tv.setText(String.valueOf(lp));
        if (lp <= 2000) {
            tv.setTextColor(Color.parseColor("#FF5252"));
        } else if (lp <= 4000) {
            tv.setTextColor(Color.parseColor("#FFD700"));
        } else {
            tv.setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    private void otorgarVictoriaJuego(int jugadorGanador) {
        if (dueloCerrado) return;

        int metaVictorias = esMatchBo3 ? 2 : 1;
        String nombreGanador = (jugadorGanador == 1) ? etNombreJ1.getText().toString() : etNombreJ2.getText().toString();

        if (jugadorGanador == 1) {
            victoriasJ1++;
            tvVictoriasJ1.setText("Juegos Ganados: " + victoriasJ1);
        } else {
            victoriasJ2++;
            tvVictoriasJ2.setText("Juegos Ganados: " + victoriasJ2);
        }

        agregarLog("🏆 Juego ganado por " + nombreGanador + " (" + victoriasJ1 + " - " + victoriasJ2 + ")");

        if (victoriasJ1 >= metaVictorias || victoriasJ2 >= metaVictorias) {
            dueloCerrado = true;
            agregarLog("✅ Encuentro finalizado. Ganador: " + nombreGanador + ". Pendiente 2 firmas.");
            Toast.makeText(getContext(), "¡" + nombreGanador + " gana el encuentro! Firmen el acta.", Toast.LENGTH_LONG).show();
        } else {
            // Reiniciar LP a 8000 para el siguiente duelo del Match
            lpJ1 = 8000;
            lpJ2 = 8000;
            actualizarVistaLp(tvLpJ1, lpJ1);
            actualizarVistaLp(tvLpJ2, lpJ2);
            agregarLog("--- Nuevo Juego Iniciado (8000 LP) ---");
        }
    }

    private void registrarFirmaJugador() {
        if (firmasConfirmadas >= 2) {
            Toast.makeText(getContext(), "El acta ya fue cerrada con ambas firmas", Toast.LENGTH_SHORT).show();
            return;
        }

        firmasConfirmadas++;
        tvEstadoFirmas.setText("Firmas: " + firmasConfirmadas + "/2");
        agregarLog("✍️ Firma registrada (" + firmasConfirmadas + "/2)");

        if (firmasConfirmadas == 2) {
            dueloCerrado = true;
            new AlertDialog.Builder(getContext())
                    .setTitle("Acta de Duelo Cerrada (2/2)")
                    .setMessage("Ambos jugadores han firmado el resultado final:\n\n" +
                            etNombreJ1.getText() + ": " + victoriasJ1 + "\n" +
                            etNombreJ2.getText() + ": " + victoriasJ2)
                    .setPositiveButton("Aceptar", null)
                    .show();
        }
    }

    private void reiniciarEncuentroCompleto() {
        lpJ1 = 8000;
        lpJ2 = 8000;
        victoriasJ1 = 0;
        victoriasJ2 = 0;
        firmasConfirmadas = 0;
        dueloCerrado = false;

        actualizarVistaLp(tvLpJ1, lpJ1);
        actualizarVistaLp(tvLpJ2, lpJ2);
        tvVictoriasJ1.setText("Juegos Ganados: 0");
        tvVictoriasJ2.setText("Juegos Ganados: 0");
        tvEstadoFirmas.setText("Firmas: 0/2");
        tvHistorial.setText("--- Duelo Reiniciado (8000 LP) ---");
    }

    private void agregarLog(String linea) {
        String actual = tvHistorial.getText().toString();
        tvHistorial.setText(linea + "\n" + actual);
    }
}