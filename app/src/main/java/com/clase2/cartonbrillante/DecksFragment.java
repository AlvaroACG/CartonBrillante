package com.clase2.cartonbrillante;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DecksFragment extends Fragment {

    private RecyclerView recyclerDecks;
    private DBHelper dbHelper;
    private DecksAdapter adapter;
    private List<Deck> listaDecks = new ArrayList<>();

    // Listas temporales mientras el diálogo de edición está abierto
    private List<byte[]> tempFotosMain = new ArrayList<>();
    private List<byte[]> tempFotosExtra = new ArrayList<>();
    private List<byte[]> tempFotosSide = new ArrayList<>();

    private DeckFotosAdapter adapterMain, adapterExtra, adapterSide;
    private TextView tvContadorMainDialog, tvContadorExtraDialog, tvContadorSideDialog;

    // 1 = MAIN, 2 = EXTRA, 3 = SIDE
    private int seccionActivaSubida = 1;

    private final ActivityResultLauncher<String> selectorMultipleFotos = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    procesarMultiplesImagenes(uris);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_vista_decks, container, false);

        recyclerDecks = view.findViewById(R.id.recyclerDecks);
        dbHelper = new DBHelper(getContext());

        recyclerDecks.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new DecksAdapter(listaDecks, new DecksAdapter.OnDeckInteractionListener() {
            @Override
            public void onAgregarNuevoClick() {
                // Casilla "+": Va directo a crear un Deck nuevo
                mostrarDialogoCrearOEditarDeck(null);
            }

            @Override
            public void onDeckClick(Deck deck) {
                // Tocar un Deck existente: Abre primero el Modo Vista (Solo lectura + Botón Editar)
                dbHelper.cargarFotosDeUnDeck(deck);
                mostrarDialogoVerDeck(deck);
            }

            @Override
            public void onDeckLongClick(Deck deck) {
                confirmarEliminarDeck(deck);
            }
        });
        recyclerDecks.setAdapter(adapter);

        cargarDecksDesdeSQLite();

        return view;
    }

    private void cargarDecksDesdeSQLite() {
        listaDecks = dbHelper.obtenerTodosLosDecks();
        adapter.actualizarLista(listaDecks);
    }

    // ==================== 1. MODO VISTA (SOLO LECTURA + ZOOM + BOTÓN EDITAR) ====================
    private void mostrarDialogoVerDeck(Deck deck) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_ver_deck, null);

        TextView tvRegulacion = view.findViewById(R.id.tvVerRegulacion);
        TextView tvVisibilidad = view.findViewById(R.id.tvVerVisibilidad);
        TextView tvProtectores = view.findViewById(R.id.tvVerProtectores);

        TextView tvTituloMain = view.findViewById(R.id.tvVerTituloMain);
        TextView tvTituloExtra = view.findViewById(R.id.tvVerTituloExtra);
        TextView tvTituloSide = view.findViewById(R.id.tvVerTituloSide);

        RecyclerView rvVerMain = view.findViewById(R.id.rvVerMain);
        RecyclerView rvVerExtra = view.findViewById(R.id.rvVerExtra);
        RecyclerView rvVerSide = view.findViewById(R.id.rvVerSide);

        String reg = (deck.getRegulacion() != null) ? deck.getRegulacion() : "TCG";
        tvRegulacion.setText(reg);
        if ("OCG".equalsIgnoreCase(reg)) {
            tvRegulacion.setBackgroundColor(Color.parseColor("#D32F2F"));
        } else {
            tvRegulacion.setBackgroundColor(Color.parseColor("#5C6BC0"));
        }

        if (deck.isEsPublico()) {
            tvVisibilidad.setText("🌐 Público en Perfil");
            tvVisibilidad.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvVisibilidad.setText("🔒 Privado");
            tvVisibilidad.setTextColor(Color.parseColor("#FFD700"));
        }

        String prot = (deck.getTipoProtectores() == null || deck.getTipoProtectores().isEmpty())
                ? "Sin protectores registrados"
                : deck.getTipoProtectores() + " (" + deck.getCantidadProtectores() + " uds.)";
        tvProtectores.setText("Protectores: " + prot);

        tvTituloMain.setText("Main Deck (" + deck.getFotosMain().size() + ")");
        tvTituloExtra.setText("Extra Deck (" + deck.getFotosExtra().size() + ")");
        tvTituloSide.setText("Side Deck (" + deck.getFotosSide().size() + ")");

        // En Modo Vista, al tocar una carta se amplía en vez de borrarse
        rvVerMain.setLayoutManager(new GridLayoutManager(getContext(), 5));
        rvVerMain.setAdapter(new DeckFotosAdapter(deck.getFotosMain(),
                pos -> mostrarCartaAmpliada(deck.getFotosMain().get(pos), "Main Deck - Carta #" + (pos + 1))));

        rvVerExtra.setLayoutManager(new GridLayoutManager(getContext(), 5));
        rvVerExtra.setAdapter(new DeckFotosAdapter(deck.getFotosExtra(),
                pos -> mostrarCartaAmpliada(deck.getFotosExtra().get(pos), "Extra Deck - Carta #" + (pos + 1))));

        rvVerSide.setLayoutManager(new GridLayoutManager(getContext(), 5));
        rvVerSide.setAdapter(new DeckFotosAdapter(deck.getFotosSide(),
                pos -> mostrarCartaAmpliada(deck.getFotosSide().get(pos), "Side Deck - Carta #" + (pos + 1))));

        new AlertDialog.Builder(getContext())
                .setTitle(deck.getNombreDeck())
                .setView(view)
                .setPositiveButton("Editar Deck", (dialog, which) -> {
                    // Al presionar Editar, abre el formulario de modificación con todos los datos cargados
                    mostrarDialogoCrearOEditarDeck(deck);
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // Muestra la carta en grande cuando la tocas dentro del Modo Vista
    private void mostrarCartaAmpliada(byte[] bytesCarta, String titulo) {
        if (bytesCarta == null || bytesCarta.length == 0) return;

        Bitmap bmp = BitmapFactory.decodeByteArray(bytesCarta, 0, bytesCarta.length);
        ImageView ivZoom = new ImageView(getContext());
        ivZoom.setImageBitmap(bmp);
        ivZoom.setAdjustViewBounds(true);

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);

        LinearLayout contenedor = new LinearLayout(getContext());
        contenedor.setOrientation(LinearLayout.VERTICAL);
        contenedor.setGravity(Gravity.CENTER);
        contenedor.setBackgroundColor(Color.parseColor("#151522"));
        contenedor.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (240 * density),
                (int) (350 * density)
        );
        ivZoom.setLayoutParams(params);
        contenedor.addView(ivZoom);

        new AlertDialog.Builder(getContext())
                .setTitle(titulo)
                .setView(contenedor)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    // ==================== 2. MODO CREACIÓN / EDICIÓN ====================
    private void mostrarDialogoCrearOEditarDeck(@Nullable Deck deckExistente) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_crear_deck, null);

        EditText etNombre = dialogView.findViewById(R.id.etNombreDeck);
        TextView tvEstadoVisibilidad = dialogView.findViewById(R.id.tvEstadoVisibilidadDeck);
        SwitchCompat swPublico = dialogView.findViewById(R.id.swDeckPublico);
        RadioButton rbTcg = dialogView.findViewById(R.id.rbDeckTcg);
        RadioButton rbOcg = dialogView.findViewById(R.id.rbDeckOcg);
        EditText etTipoProt = dialogView.findViewById(R.id.etTipoProtectores);
        EditText etCantProt = dialogView.findViewById(R.id.etCantidadProtectores);

        tvContadorMainDialog = dialogView.findViewById(R.id.tvContadorMain);
        tvContadorExtraDialog = dialogView.findViewById(R.id.tvContadorExtra);
        tvContadorSideDialog = dialogView.findViewById(R.id.tvContadorSide);

        Button btnMain = dialogView.findViewById(R.id.btnAgregarFotosMain);
        Button btnExtra = dialogView.findViewById(R.id.btnAgregarFotosExtra);
        Button btnSide = dialogView.findViewById(R.id.btnAgregarFotosSide);

        RecyclerView rvMain = dialogView.findViewById(R.id.rvFotosMain);
        RecyclerView rvExtra = dialogView.findViewById(R.id.rvFotosExtra);
        RecyclerView rvSide = dialogView.findViewById(R.id.rvFotosSide);

        tempFotosMain = new ArrayList<>();
        tempFotosExtra = new ArrayList<>();
        tempFotosSide = new ArrayList<>();

        if (deckExistente != null) {
            etNombre.setText(deckExistente.getNombreDeck());
            swPublico.setChecked(deckExistente.isEsPublico());
            if ("OCG".equalsIgnoreCase(deckExistente.getRegulacion())) {
                rbOcg.setChecked(true);
            } else {
                rbTcg.setChecked(true);
            }
            etTipoProt.setText(deckExistente.getTipoProtectores());
            etCantProt.setText(String.valueOf(deckExistente.getCantidadProtectores()));

            if (deckExistente.getFotosMain() != null) tempFotosMain.addAll(deckExistente.getFotosMain());
            if (deckExistente.getFotosExtra() != null) tempFotosExtra.addAll(deckExistente.getFotosExtra());
            if (deckExistente.getFotosSide() != null) tempFotosSide.addAll(deckExistente.getFotosSide());
        }

        actualizarTextoSwitch(tvEstadoVisibilidad, swPublico.isChecked());
        swPublico.setOnCheckedChangeListener((buttonView, isChecked) ->
                actualizarTextoSwitch(tvEstadoVisibilidad, isChecked)
        );

        // En Modo Edición, tocar una carta la quita de la lista
        rvMain.setLayoutManager(new GridLayoutManager(getContext(), 5));
        adapterMain = new DeckFotosAdapter(tempFotosMain, pos -> {
            tempFotosMain.remove(pos);
            adapterMain.notifyItemRemoved(pos);
            actualizarContadoresUI();
        });
        rvMain.setAdapter(adapterMain);

        rvExtra.setLayoutManager(new GridLayoutManager(getContext(), 5));
        adapterExtra = new DeckFotosAdapter(tempFotosExtra, pos -> {
            tempFotosExtra.remove(pos);
            adapterExtra.notifyItemRemoved(pos);
            actualizarContadoresUI();
        });
        rvExtra.setAdapter(adapterExtra);

        rvSide.setLayoutManager(new GridLayoutManager(getContext(), 5));
        adapterSide = new DeckFotosAdapter(tempFotosSide, pos -> {
            tempFotosSide.remove(pos);
            adapterSide.notifyItemRemoved(pos);
            actualizarContadoresUI();
        });
        rvSide.setAdapter(adapterSide);

        actualizarContadoresUI();

        btnMain.setOnClickListener(v -> {
            if (tempFotosMain.size() >= 60) {
                Toast.makeText(getContext(), "El Main Deck ya alcanzó el máximo de 60 cartas", Toast.LENGTH_SHORT).show();
                return;
            }
            seccionActivaSubida = 1;
            selectorMultipleFotos.launch("image/*");
        });

        btnExtra.setOnClickListener(v -> {
            if (tempFotosExtra.size() >= 15) {
                Toast.makeText(getContext(), "El Extra Deck ya alcanzó el máximo de 15 cartas", Toast.LENGTH_SHORT).show();
                return;
            }
            seccionActivaSubida = 2;
            selectorMultipleFotos.launch("image/*");
        });

        btnSide.setOnClickListener(v -> {
            if (tempFotosSide.size() >= 15) {
                Toast.makeText(getContext(), "El Side Deck ya alcanzó el máximo de 15 cartas", Toast.LENGTH_SHORT).show();
                return;
            }
            seccionActivaSubida = 3;
            selectorMultipleFotos.launch("image/*");
        });

        String titulo = (deckExistente == null) ? "Registrar Nuevo Deck" : "Editar: " + deckExistente.getNombreDeck();

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(titulo)
                .setView(dialogView)
                .setPositiveButton("Guardar Cambios", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(v -> {
                String nombre = etNombre.getText().toString().trim();
                if (nombre.isEmpty()) {
                    Toast.makeText(getContext(), "El nombre del deck es obligatorio", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (tempFotosMain.size() < 40 || tempFotosMain.size() > 60) {
                    Toast.makeText(getContext(),
                            "El Main Deck debe tener entre 40 y 60 cartas (Actual: " + tempFotosMain.size() + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (tempFotosExtra.size() > 15) {
                    Toast.makeText(getContext(), "El Extra Deck no puede superar las 15 cartas", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (tempFotosSide.size() > 15) {
                    Toast.makeText(getContext(), "El Side Deck no puede superar las 15 cartas", Toast.LENGTH_SHORT).show();
                    return;
                }

                String regulacion = rbOcg.isChecked() ? "OCG" : "TCG";
                boolean esPublico = swPublico.isChecked();
                String tipoProt = etTipoProt.getText().toString().trim();
                int cantProt = 70;
                try {
                    cantProt = Integer.parseInt(etCantProt.getText().toString().trim());
                } catch (NumberFormatException ignored) {}

                if (deckExistente == null) {
                    Deck nuevo = new Deck(nombre, tipoProt, cantProt, regulacion, esPublico,
                            tempFotosMain, tempFotosExtra, tempFotosSide);
                    dbHelper.insertarDeck(nuevo);
                    Toast.makeText(getContext(), "Deck guardado exitosamente", Toast.LENGTH_SHORT).show();
                } else {
                    deckExistente.setNombreDeck(nombre);
                    deckExistente.setRegulacion(regulacion);
                    deckExistente.setEsPublico(esPublico);
                    deckExistente.setTipoProtectores(tipoProt);
                    deckExistente.setCantidadProtectores(cantProt);
                    deckExistente.setFotosMain(tempFotosMain);
                    deckExistente.setFotosExtra(tempFotosExtra);
                    deckExistente.setFotosSide(tempFotosSide);
                    dbHelper.actualizarDeck(deckExistente);
                    Toast.makeText(getContext(), "Deck actualizado", Toast.LENGTH_SHORT).show();
                }

                cargarDecksDesdeSQLite();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void actualizarTextoSwitch(TextView tv, boolean esPublico) {
        if (esPublico) {
            tv.setText("Visibilidad: 🌐 Público en mi Perfil");
            tv.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tv.setText("Visibilidad: 🔒 Privado (Solo en este dispositivo)");
            tv.setTextColor(Color.parseColor("#FFD700"));
        }
    }

    private void actualizarContadoresUI() {
        if (tvContadorMainDialog != null) {
            int cMain = tempFotosMain.size();
            tvContadorMainDialog.setText("Main Deck: " + cMain + "/60 (Mín. 40)");
            if (cMain >= 40 && cMain <= 60) {
                tvContadorMainDialog.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                tvContadorMainDialog.setTextColor(Color.parseColor("#FF5252"));
            }
        }
        if (tvContadorExtraDialog != null) {
            tvContadorExtraDialog.setText("Extra Deck: " + tempFotosExtra.size() + "/15");
        }
        if (tvContadorSideDialog != null) {
            tvContadorSideDialog.setText("Side Deck: " + tempFotosSide.size() + "/15");
        }
    }

    private void procesarMultiplesImagenes(List<Uri> uris) {
        int agregadas = 0;

        for (Uri uri : uris) {
            if (seccionActivaSubida == 1 && tempFotosMain.size() >= 60) break;
            if (seccionActivaSubida == 2 && tempFotosExtra.size() >= 15) break;
            if (seccionActivaSubida == 3 && tempFotosSide.size() >= 15) break;

            byte[] comprimida = comprimirImagenCartaDeck(uri);
            if (comprimida != null) {
                if (seccionActivaSubida == 1) {
                    tempFotosMain.add(comprimida);
                } else if (seccionActivaSubida == 2) {
                    tempFotosExtra.add(comprimida);
                } else if (seccionActivaSubida == 3) {
                    tempFotosSide.add(comprimida);
                }
                agregadas++;
            }
        }

        if (seccionActivaSubida == 1 && adapterMain != null) adapterMain.notifyDataSetChanged();
        if (seccionActivaSubida == 2 && adapterExtra != null) adapterExtra.notifyDataSetChanged();
        if (seccionActivaSubida == 3 && adapterSide != null) adapterSide.notifyDataSetChanged();

        actualizarContadoresUI();

        if (agregadas > 0) {
            Toast.makeText(getContext(), agregadas + " carta(s) añadida(s)", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] comprimirImagenCartaDeck(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (original != null) {
                int anchoObjetivo = 180;
                int altoObjetivo = (int) (original.getHeight() * ((float) anchoObjetivo / original.getWidth()));
                Bitmap escalado = Bitmap.createScaledBitmap(original, anchoObjetivo, altoObjetivo, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                escalado.compress(Bitmap.CompressFormat.JPEG, 65, stream);
                return stream.toByteArray();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void confirmarEliminarDeck(Deck deck) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Deck")
                .setMessage("¿Seguro que deseas eliminar \"" + deck.getNombreDeck() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dbHelper.eliminarDeck(deck.getIdDeck());
                    cargarDecksDesdeSQLite();
                    Toast.makeText(getContext(), "Deck eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}