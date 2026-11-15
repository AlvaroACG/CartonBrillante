package com.clase2.cartonbrillante;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class InicioFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inicio, container, false);

        Button btnCalculadora = view.findViewById(R.id.btnCalculadora);
        Button btnCartonPass = view.findViewById(R.id.btnCartonPass);
        Button btnForo = view.findViewById(R.id.btnForo);
        Button btnEventos = view.findViewById(R.id.btnEventos);
        Button btnMapa = view.findViewById(R.id.btnMapa);
        Button btnPerfil = view.findViewById(R.id.btnPerfil);

        // 1. Abrir Calculadora de Duelos (Fragment)
        btnCalculadora.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CalculadoraFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 2. Abrir CartonPass (Activity Local con SQLite)
        btnCartonPass.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), vistaCartonPass.class);
            startActivity(intent);
        });

        // 3. Abrir Foro
        btnForo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Foro.class);
            startActivity(intent);
        });

        // 4. Abrir Eventos
        btnEventos.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), vistaEvento.class);
            startActivity(intent);
        });

        // 5. Abrir Mapa
        btnMapa.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), vistaMapa.class);
            startActivity(intent);
        });

        // 6. Abrir Perfil
        btnPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), vistaPerfil.class);
            startActivity(intent);
        });

        return view;
    }
}