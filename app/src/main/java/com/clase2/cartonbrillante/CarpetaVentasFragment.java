package com.clase2.cartonbrillante;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CarpetaVentasFragment extends Fragment {

    private RecyclerView recyclerCarpeta;
    private DBHelper dbHelper;
    private CarpetaAdapter adapter;
    private List<CartaCarpeta> listaCartas = new ArrayList<>();

    private byte[] imagenSeleccionadaBytes = null;
    private ImageView ivPreviewDialogoActual = null;

    // 1. Launcher para elegir imagen desde la Galería
    private final ActivityResultLauncher<String> selectorGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    procesarUriImagen(uri);
                }
            }
    );

    // 2. Launcher para tomar foto directamente con la Cámara
    private final ActivityResultLauncher<Void> tomarFotoCamaraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    procesarBitmapDirecto(bitmap);
                }
            }
    );

    // 3. Launcher para pedir permiso de cámara si aún no se ha concedido
    private final ActivityResultLauncher<String> permisoCamaraCartaLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            concedido -> {
                if (concedido) {
                    tomarFotoCamaraLauncher.launch(null);
                } else {
                    Toast.makeText(getContext(), "Necesitas dar permiso de cámara para tomar la foto", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_carpeta_ventas, container, false);

        recyclerCarpeta = view.findViewById(R.id.recyclerCarpeta);
        dbHelper = new DBHelper(getContext());

        recyclerCarpeta.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new CarpetaAdapter(listaCartas, new CarpetaAdapter.OnCartaInteractionListener() {
            @Override
            public void onAgregarNuevaCartaClick() {
                // Casilla "+" (Índice 0): Abre directo el formulario para crear
                mostrarDialogoCrearOEditarCarta(null);
            }

            @Override
            public void onCartaClick(CartaCarpeta carta) {
                // Tocar una carta guardada: Abre primero el Modo Vista con opción de Editar
                mostrarDialogoVerCarta(carta);
            }

            @Override
            public void onCartaLongClick(CartaCarpeta carta) {
                confirmarEliminacion(carta);
            }
        });
        recyclerCarpeta.setAdapter(adapter);

        cargarCartasDesdeSQLite();

        return view;
    }

    private void cargarCartasDesdeSQLite() {
        listaCartas = dbHelper.obtenerTodasLasCartas();
        adapter.actualizarLista(listaCartas);
    }

    // ==================== 1. MODO VISTA (VER DETALLE + BOTÓN EDITAR) ====================
    private void mostrarDialogoVerCarta(CartaCarpeta carta) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_ver_carta, null);

        ImageView ivImagen = view.findViewById(R.id.ivVerImagenCarta);
        TextView tvRegulacion = view.findViewById(R.id.tvVerRegulacionCarta);
        TextView tvCantidad = view.findViewById(R.id.tvVerCantidadCarta);
        TextView tvCodigoSet = view.findViewById(R.id.tvVerCodigoSet);
        TextView tvRareza = view.findViewById(R.id.tvVerRareza);
        TextView tvCondicion = view.findViewById(R.id.tvVerCondicion);
        TextView tvPrecio = view.findViewById(R.id.tvVerPrecio);

        byte[] blob = carta.getImagen();
        if (blob != null && blob.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length);
            ivImagen.setImageBitmap(bmp);
        } else {
            ivImagen.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        String reg = (carta.getRegulacion() != null) ? carta.getRegulacion() : "TCG";
        tvRegulacion.setText(reg);
        if ("OCG".equalsIgnoreCase(reg)) {
            tvRegulacion.setBackgroundColor(Color.parseColor("#D32F2F"));
        } else {
            tvRegulacion.setBackgroundColor(Color.parseColor("#5C6BC0"));
        }

        tvCantidad.setText("Disponibles: x" + carta.getCantidad());

        String codigo = (carta.getCodigoSet() == null || carta.getCodigoSet().isEmpty()) ? "Sin especificar" : carta.getCodigoSet();
        String rareza = (carta.getRareza() == null || carta.getRareza().isEmpty()) ? "Sin especificar" : carta.getRareza();
        tvCodigoSet.setText("Código de Set: " + codigo);
        tvRareza.setText("Rareza: " + rareza);
        tvCondicion.setText("Condición / Estado: " + carta.getCondicion());

        if (carta.getPrecioReferencia() > 0) {
            tvPrecio.setText("$" + carta.getPrecioReferencia() + " CLP");
        } else {
            tvPrecio.setText("Solo Cambio / Colección");
        }

        new AlertDialog.Builder(getContext())
                .setTitle(carta.getNombre())
                .setView(view)
                .setPositiveButton("Editar Carta", (dialog, which) -> {
                    // Pasa al formulario de edición con los datos de esta carta
                    mostrarDialogoCrearOEditarCarta(carta);
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // ==================== 2. MODO CREACIÓN / EDICIÓN ====================
    private void mostrarDialogoCrearOEditarCarta(@Nullable CartaCarpeta cartaExistente) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_agregar_carta, null);

        ivPreviewDialogoActual = dialogView.findViewById(R.id.ivPreviewCarta);
        Button btnTomarFoto = dialogView.findViewById(R.id.btnTomarFotoCarta);
        Button btnSeleccionarFoto = dialogView.findViewById(R.id.btnSeleccionarFoto);
        EditText etNombre = dialogView.findViewById(R.id.etNombreCarta);
        RadioButton rbTcg = dialogView.findViewById(R.id.rbCartaTcg);
        RadioButton rbOcg = dialogView.findViewById(R.id.rbCartaOcg);
        EditText etCodigo = dialogView.findViewById(R.id.etCodigoSet);
        EditText etRareza = dialogView.findViewById(R.id.etRareza);
        EditText etCondicion = dialogView.findViewById(R.id.etCondicion);
        EditText etCantidad = dialogView.findViewById(R.id.etCantidad);
        EditText etPrecio = dialogView.findViewById(R.id.etPrecio);

        imagenSeleccionadaBytes = null;

        if (cartaExistente != null) {
            etNombre.setText(cartaExistente.getNombre());
            if ("OCG".equalsIgnoreCase(cartaExistente.getRegulacion())) {
                rbOcg.setChecked(true);
            } else {
                rbTcg.setChecked(true);
            }
            etCodigo.setText(cartaExistente.getCodigoSet());
            etRareza.setText(cartaExistente.getRareza());
            etCondicion.setText(cartaExistente.getCondicion());
            etCantidad.setText(String.valueOf(cartaExistente.getCantidad()));
            etPrecio.setText(String.valueOf(cartaExistente.getPrecioReferencia()));
            imagenSeleccionadaBytes = cartaExistente.getImagen();

            if (imagenSeleccionadaBytes != null && imagenSeleccionadaBytes.length > 0) {
                Bitmap bmp = BitmapFactory.decodeByteArray(imagenSeleccionadaBytes, 0, imagenSeleccionadaBytes.length);
                ivPreviewDialogoActual.setImageBitmap(bmp);
            }
        }

        // Opción 1: Sacar foto con la cámara
        btnTomarFoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                tomarFotoCamaraLauncher.launch(null);
            } else {
                permisoCamaraCartaLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Opción 2: Elegir foto de la galería
        btnSeleccionarFoto.setOnClickListener(v -> selectorGaleria.launch("image/*"));

        String tituloDialogo = (cartaExistente == null) ? "Agregar Carta a la Carpeta" : "Editar: " + cartaExistente.getNombre();

        new AlertDialog.Builder(getContext())
                .setTitle(tituloDialogo)
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(getContext(), "El nombre de la carta es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String regulacion = rbOcg.isChecked() ? "OCG" : "TCG";
                    String codigo = etCodigo.getText().toString().trim();
                    String rareza = etRareza.getText().toString().trim();
                    String condicion = etCondicion.getText().toString().trim();
                    if (condicion.isEmpty()) condicion = "NM";

                    int cantidad = parsearEntero(etCantidad.getText().toString(), 1);
                    int precio = parsearEntero(etPrecio.getText().toString(), 0);

                    if (cartaExistente == null) {
                        CartaCarpeta nueva = new CartaCarpeta(nombre, codigo, rareza, condicion, cantidad, precio, regulacion, imagenSeleccionadaBytes);
                        dbHelper.insertarCarta(nueva);
                        Toast.makeText(getContext(), "Carta agregada (" + regulacion + ")", Toast.LENGTH_SHORT).show();
                    } else {
                        cartaExistente.setNombre(nombre);
                        cartaExistente.setRegulacion(regulacion);
                        cartaExistente.setCodigoSet(codigo);
                        cartaExistente.setRareza(rareza);
                        cartaExistente.setCondicion(condicion);
                        cartaExistente.setCantidad(cantidad);
                        cartaExistente.setPrecioReferencia(precio);
                        cartaExistente.setImagen(imagenSeleccionadaBytes);
                        dbHelper.actualizarCarta(cartaExistente);
                        Toast.makeText(getContext(), "Carta actualizada", Toast.LENGTH_SHORT).show();
                    }

                    cargarCartasDesdeSQLite();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminacion(CartaCarpeta carta) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Carta")
                .setMessage("¿Deseas sacar \"" + carta.getNombre() + "\" de tu carpeta?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dbHelper.eliminarCarta(carta.getIdCarta());
                    cargarCartasDesdeSQLite();
                    Toast.makeText(getContext(), "Carta eliminada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void procesarUriImagen(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (original != null) {
                procesarBitmapDirecto(original);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    // Escala y comprime tanto las fotos de la Cámara como las de la Galería
    private void procesarBitmapDirecto(Bitmap original) {
        try {
            int anchoObjetivo = 400;
            int altoObjetivo = (int) (original.getHeight() * ((float) anchoObjetivo / original.getWidth()));
            Bitmap escalado = Bitmap.createScaledBitmap(original, anchoObjetivo, altoObjetivo, true);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            escalado.compress(Bitmap.CompressFormat.JPEG, 75, stream);
            imagenSeleccionadaBytes = stream.toByteArray();

            if (ivPreviewDialogoActual != null) {
                ivPreviewDialogoActual.setImageBitmap(escalado);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al procesar la foto", Toast.LENGTH_SHORT).show();
        }
    }

    private int parsearEntero(String texto, int valorPorDefecto) {
        if (texto == null || texto.trim().isEmpty()) return valorPorDefecto;
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return valorPorDefecto;
        }
    }
}