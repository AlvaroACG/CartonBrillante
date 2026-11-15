package com.clase2.cartonbrillante;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Foro extends AppCompatActivity {

    private static final int MAX_LADO_PX = 800;
    private static final int CALIDAD_JPEG = 70;
    private static final int MAX_CARACTERES_BASE64_TOTAL = 900000;
    private static final int MAX_CARACTERES_COMENTARIO = 500;
    private static final String TIPO_VENTA = "Vender Carta";

    private LinearLayout llPublicaciones;
    private Button btnVlvr;
    private FloatingActionButton fabNuevaPublicacion;

    private List<Uri> imagenesSeleccionadas = new ArrayList<>();
    private ImageView imagenDialogoActual = null;
    private TextView tvContadorImagenesDialogo = null;
    private SwitchCompat swMapaDialogo = null;
    private TextView tvUbicacionDialogo = null;
    private Double latitudPublicacion = null;
    private Double longitudPublicacion = null;

    private ActivityResultLauncher<String> selectorImagenes;
    private ActivityResultLauncher<String> selectorPermisoUbicacion;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_foro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        llPublicaciones = findViewById(R.id.ll_publicaciones);
        btnVlvr = findViewById(R.id.btnVolver);
        fabNuevaPublicacion = findViewById(R.id.fab_nueva_publicacion);

        btnVlvr.setOnClickListener(v -> {
            Intent vol = new Intent(this, MenuPrincipal.class);
            startActivity(vol);
        });

        selectorImagenes = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        if (uris.size() > 10) {
                            Toast.makeText(this, "Máximo 10 fotos permitidas. Se tomarán las primeras 10.", Toast.LENGTH_SHORT).show();
                            imagenesSeleccionadas = new ArrayList<>(uris.subList(0, 10));
                        } else {
                            imagenesSeleccionadas = new ArrayList<>(uris);
                        }

                        if (imagenDialogoActual != null && !imagenesSeleccionadas.isEmpty()) {
                            imagenDialogoActual.setImageURI(imagenesSeleccionadas.get(0));
                            imagenDialogoActual.setVisibility(View.VISIBLE);
                        }
                        if (tvContadorImagenesDialogo != null) {
                            tvContadorImagenesDialogo.setText(imagenesSeleccionadas.size() + " foto(s) seleccionada(s)");
                            tvContadorImagenesDialogo.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        selectorPermisoUbicacion = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    if (concedido) {
                        obtenerUbicacionActual();
                    } else {
                        desactivarInterruptorMapa("Permiso de ubicación denegado.");
                    }
                }
        );

        fabNuevaPublicacion.setOnClickListener(v -> mostrarDialogoPublicacion());

        cargarPublicacionesDesdeCloud();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ejecutor.shutdown();
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void desactivarInterruptorMapa(String mensaje) {
        latitudPublicacion = null;
        longitudPublicacion = null;
        if (swMapaDialogo != null) swMapaDialogo.setChecked(false);
        if (tvUbicacionDialogo != null) tvUbicacionDialogo.setText("📍 Ubicación: No seleccionada");
        if (mensaje != null) Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    private void aplicarUbicacion(Location ubicacion) {
        if (swMapaDialogo == null || !swMapaDialogo.isChecked()) return;
        latitudPublicacion = ubicacion.getLatitude();
        longitudPublicacion = ubicacion.getLongitude();
        if (tvUbicacionDialogo != null) {
            tvUbicacionDialogo.setText("📍 Ubicación fijada en el mapa");
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionActual() {
        if (!tienePermisoUbicacion()) return;

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            desactivarInterruptorMapa("No se pudo acceder a la ubicación.");
            return;
        }

        Location mejor = null;
        for (String proveedor : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(proveedor);
            if (l != null && (mejor == null || l.getAccuracy() < mejor.getAccuracy())) {
                mejor = l;
            }
        }

        if (mejor != null) {
            aplicarUbicacion(mejor);
            return;
        }

        String proveedor = null;
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            proveedor = LocationManager.GPS_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            proveedor = LocationManager.NETWORK_PROVIDER;
        }

        if (proveedor == null) {
            desactivarInterruptorMapa("Activa la ubicación del dispositivo.");
            return;
        }

        if (tvUbicacionDialogo != null) tvUbicacionDialogo.setText("📍 Obteniendo ubicación...");

        lm.requestSingleUpdate(proveedor, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                aplicarUbicacion(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        }, Looper.getMainLooper());
    }

    @SuppressWarnings("unchecked")
    private void cargarPublicacionesDesdeCloud() {
        db.collection("publicaciones")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    llPublicaciones.removeAllViews();
                    for (QueryDocumentSnapshot doc : value) {
                        String idDoc = doc.getId();
                        String titulo = doc.getString("titulo");
                        String cuerpoTexto = doc.getString("detalle");
                        String tipo = doc.getString("tipo_actividad");
                        String precio = doc.getString("precio");
                        if (precio == null) precio = "";
                        String formato = doc.getString("formato");
                        if (formato == null) formato = "N/A";
                        String autorUid = doc.getString("autor_uid");

                        String autorNick = doc.getString("autor_nick");
                        if (autorNick == null || autorNick.isEmpty()) autorNick = doc.getString("autor_nombre");
                        if (autorNick == null || autorNick.isEmpty()) autorNick = "Duelista";

                        Double condDouble = doc.getDouble("condicion");
                        float condicion = condDouble != null ? condDouble.floatValue() : 0f;

                        Boolean negociableObj = doc.getBoolean("negociable");
                        boolean negociable = negociableObj != null && negociableObj;

                        Boolean enMapaObj = doc.getBoolean("en_mapa");
                        Double latObj = doc.getDouble("latitud");
                        Double lonObj = doc.getDouble("longitud");
                        boolean enMapa = enMapaObj != null && enMapaObj && latObj != null && lonObj != null;

                        String imagenBase64 = null;
                        List<String> listaImagenes = (List<String>) doc.get("imagenes_base64");
                        if (listaImagenes != null && !listaImagenes.isEmpty()) {
                            imagenBase64 = listaImagenes.get(0);
                        }

                        crearVistaPublicacion(idDoc, titulo, cuerpoTexto, tipo, precio, formato, condicion, negociable, imagenBase64, autorUid, autorNick, enMapa, latObj, lonObj);
                    }
                });
    }

    private Bitmap decodificarBase64(String base64, int muestreo) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = muestreo;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opciones);
        } catch (Exception e) {
            return null;
        }
    }

    private String uriABase64Jpeg(Uri uri) {
        try {
            BitmapFactory.Options limites = new BitmapFactory.Options();
            limites.inJustDecodeBounds = true;
            InputStream in1 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in1, null, limites);
            if (in1 != null) in1.close();

            int muestreo = 1;
            while (limites.outWidth / muestreo > MAX_LADO_PX * 2 || limites.outHeight / muestreo > MAX_LADO_PX * 2) {
                muestreo *= 2;
            }

            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = muestreo;
            InputStream in2 = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(in2, null, opciones);
            if (in2 != null) in2.close();
            if (original == null) return null;

            float escala = Math.min(1f, (float) MAX_LADO_PX / Math.max(original.getWidth(), original.getHeight()));
            Bitmap reducida = original;
            if (escala < 1f) {
                reducida = Bitmap.createScaledBitmap(original,
                        Math.round(original.getWidth() * escala),
                        Math.round(original.getHeight() * escala), true);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            reducida.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
            return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarDialogoPublicacion() {
        Dialog dialogo = new Dialog(this);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.dialog_nueva_publicacion);

        Window window = dialogo.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RadioGroup rgTipoPublicacion = dialogo.findViewById(R.id.rg_tipo_publicacion);
        LinearLayout llCamposExtra = dialogo.findViewById(R.id.ll_campos_extra);
        EditText etTitulo = dialogo.findViewById(R.id.et_titulo);
        EditText etCuerpoTexto = dialogo.findViewById(R.id.et_cuerpo_texto);
        EditText etPrecio = dialogo.findViewById(R.id.et_precio);
        Button btnCargarImagen = dialogo.findViewById(R.id.btn_cargar_imagen);

        imagenDialogoActual = dialogo.findViewById(R.id.iv_imagen_publicacion);
        tvContadorImagenesDialogo = dialogo.findViewById(R.id.tv_contador_imagenes);
        swMapaDialogo = dialogo.findViewById(R.id.sw_mapa);
        tvUbicacionDialogo = dialogo.findViewById(R.id.tv_ubicacion_seleccionada);
        latitudPublicacion = null;
        longitudPublicacion = null;

        RatingBar rbCondicion = dialogo.findViewById(R.id.rb_condicion);
        CheckBox cbNegociable = dialogo.findViewById(R.id.cb_negociable);
        RadioGroup rgFormato = dialogo.findViewById(R.id.rg_formato);
        Button btnGuardarPublicacion = dialogo.findViewById(R.id.btn_guardar_publicacion);

        dialogo.setOnDismissListener(d -> {
            imagenesSeleccionadas.clear();
            imagenDialogoActual = null;
            tvContadorImagenesDialogo = null;
            swMapaDialogo = null;
            tvUbicacionDialogo = null;
            latitudPublicacion = null;
            longitudPublicacion = null;
        });

        rgTipoPublicacion.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_carpeta) {
                dialogo.dismiss();
                Intent intent = new Intent(this, MenuPrincipal.class);
                intent.putExtra("abrir_destino", "carpeta_ventas");
                startActivity(intent);
                return;
            }
            if (checkedId == R.id.rb_simple) {
                llCamposExtra.setVisibility(View.GONE);
            } else {
                llCamposExtra.setVisibility(View.VISIBLE);
            }
        });

        swMapaDialogo.setOnCheckedChangeListener((boton, activado) -> {
            if (!activado) {
                latitudPublicacion = null;
                longitudPublicacion = null;
                tvUbicacionDialogo.setText("📍 Ubicación: No seleccionada");
                return;
            }
            if (tienePermisoUbicacion()) {
                obtenerUbicacionActual();
            } else {
                selectorPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        btnCargarImagen.setOnClickListener(v -> selectorImagenes.launch("image/*"));

        btnGuardarPublicacion.setOnClickListener(v -> {
            FirebaseUser usuario = auth.getCurrentUser();
            if (usuario == null) {
                Toast.makeText(this, "Debes iniciar sesión para publicar.", Toast.LENGTH_SHORT).show();
                return;
            }

            int idTipo = rgTipoPublicacion.getCheckedRadioButtonId();
            boolean esSimple = idTipo == R.id.rb_simple;
            boolean esVenta = idTipo == R.id.rb_vender;
            boolean esBusqueda = idTipo == R.id.rb_buscar;
            boolean esEvento = idTipo == R.id.rb_evento;

            if (!esSimple && !esVenta && !esBusqueda && !esEvento) {
                Toast.makeText(this, "Selecciona qué deseas hacer.", Toast.LENGTH_SHORT).show();
                return;
            }

            String titulo = etTitulo.getText().toString().trim();
            String cuerpoTexto = etCuerpoTexto.getText().toString().trim();

            if (titulo.length() <= 2) {
                Toast.makeText(this, "El título debe tener más de 2 caracteres.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cuerpoTexto.isEmpty()) {
                Toast.makeText(this, "Escribe el contenido de la publicación.", Toast.LENGTH_SHORT).show();
                return;
            }

            String precioCalculado = "";
            String formatoCalculado = "N/A";
            float condicionCalculada = 0f;
            boolean negociableCalculado = false;
            boolean enMapaCalculado = false;

            if (!esSimple) {
                String precioStr = etPrecio.getText().toString().trim();
                if (precioStr.isEmpty()) {
                    if (esVenta) {
                        Toast.makeText(this, "El precio es obligatorio para vender una carta.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    try {
                        int precio = Integer.parseInt(precioStr);
                        if (precio <= 0) {
                            Toast.makeText(this, "El precio debe ser mayor a 0.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (precio % 10 != 0) {
                            Toast.makeText(this, "El precio debe ser un múltiplo válido para CLP (ej. múltiplo de 10).", Toast.LENGTH_LONG).show();
                            return;
                        }
                        precioCalculado = "CLP $" + precio;
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Por favor ingrese un precio numérico válido.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                int idFormato = rgFormato.getCheckedRadioButtonId();
                if (idFormato == -1) {
                    Toast.makeText(this, "Selecciona el formato (TCG u OCG).", Toast.LENGTH_SHORT).show();
                    return;
                }
                RadioButton rbFormato = dialogo.findViewById(idFormato);
                formatoCalculado = rbFormato.getText().toString();

                if (esVenta) {
                    if (rbCondicion.getRating() < 1f) {
                        Toast.makeText(this, "Indica la condición de la carta.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (imagenesSeleccionadas.isEmpty()) {
                        Toast.makeText(this, "Agrega al menos una foto de la carta.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    condicionCalculada = rbCondicion.getRating();
                    negociableCalculado = cbNegociable.isChecked();
                }

                if (esEvento && !swMapaDialogo.isChecked()) {
                    Toast.makeText(this, "Activa el mapa para fijar la ubicación del evento.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (swMapaDialogo.isChecked()) {
                    if (latitudPublicacion == null || longitudPublicacion == null) {
                        Toast.makeText(this, "Aún no se fija la ubicación. Espera un momento o desactiva el mapa.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    enMapaCalculado = true;
                }
            }

            String tipoCalculado = "Discusión General";
            if (!esSimple) {
                RadioButton rbTipo = dialogo.findViewById(idTipo);
                tipoCalculado = rbTipo.getText().toString();
            }

            final String precioFinal = precioCalculado;
            final String tipoFinal = tipoCalculado;
            final String formatoFinal = formatoCalculado;
            final float condicion = condicionCalculada;
            final boolean negociable = negociableCalculado;
            final boolean enMapa = enMapaCalculado;
            final Double lat = latitudPublicacion;
            final Double lon = longitudPublicacion;
            final List<Uri> urisCopia = new ArrayList<>(imagenesSeleccionadas);
            final String uid = usuario.getUid();
            final String nick = new DBHelper(this).obtenerNicknameLocal();

            btnGuardarPublicacion.setEnabled(false);
            btnGuardarPublicacion.setText("Publicando...");

            ejecutor.execute(() -> {
                List<String> imagenesBase64 = new ArrayList<>();
                int total = 0;
                for (Uri uri : urisCopia) {
                    String b64 = uriABase64Jpeg(uri);
                    if (b64 == null) continue;
                    if (total + b64.length() > MAX_CARACTERES_BASE64_TOTAL) break;
                    total += b64.length();
                    imagenesBase64.add(b64);
                }

                Map<String, Object> pubCloud = new HashMap<>();
                pubCloud.put("titulo", titulo);
                pubCloud.put("detalle", cuerpoTexto);
                pubCloud.put("tipo_actividad", tipoFinal);
                pubCloud.put("precio", precioFinal);
                pubCloud.put("formato", formatoFinal);
                pubCloud.put("condicion", condicion);
                pubCloud.put("negociable", negociable);
                pubCloud.put("imagenes_base64", imagenesBase64);
                pubCloud.put("autor_uid", uid);
                pubCloud.put("autor_nick", nick);
                pubCloud.put("en_mapa", enMapa);
                pubCloud.put("likes_uids", new ArrayList<String>());
                if (enMapa && lat != null && lon != null) {
                    pubCloud.put("latitud", lat);
                    pubCloud.put("longitud", lon);
                }
                pubCloud.put("timestamp", System.currentTimeMillis());

                runOnUiThread(() -> {
                    db.collection("publicaciones").add(pubCloud)
                            .addOnSuccessListener(ref ->
                                    Toast.makeText(this, "Publicación creada", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "No se pudo crear la publicación", Toast.LENGTH_SHORT).show()
                            );

                    dialogo.dismiss();
                });
            });
        });

        dialogo.show();
    }

    private void crearVistaPublicacion(String idDoc, String titulo, String cuerpoTexto, String tipo, String precio, String formato, float condicion, boolean negociable, String imagenBase64, String autorUid, String autorNick, boolean enMapa, Double lat, Double lon) {
        androidx.cardview.widget.CardView tarjetaPublicacion = new androidx.cardview.widget.CardView(this);
        LinearLayout.LayoutParams parametrosTarjeta = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        parametrosTarjeta.setMargins(0, 0, 0, 24);
        tarjetaPublicacion.setLayoutParams(parametrosTarjeta);
        tarjetaPublicacion.setCardBackgroundColor(Color.parseColor("#3D2C45"));
        tarjetaPublicacion.setRadius(24f);
        tarjetaPublicacion.setCardElevation(6f);
        tarjetaPublicacion.setUseCompatPadding(true);

        LinearLayout contenidoTarjeta = new LinearLayout(this);
        contenidoTarjeta.setOrientation(LinearLayout.HORIZONTAL);
        contenidoTarjeta.setPadding(24, 24, 24, 24);
        contenidoTarjeta.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ImageView imagen = new ImageView(this);
        LinearLayout.LayoutParams parametrosImagen = new LinearLayout.LayoutParams(220, 220);
        imagen.setLayoutParams(parametrosImagen);
        imagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagen.setBackgroundColor(Color.parseColor("#1A1A24"));
        Bitmap miniatura = decodificarBase64(imagenBase64, 2);
        if (miniatura != null) {
            imagen.setImageBitmap(miniatura);
        }
        contenidoTarjeta.addView(imagen);

        LinearLayout cajaTexto = new LinearLayout(this);
        cajaTexto.setOrientation(LinearLayout.VERTICAL);
        cajaTexto.setPadding(24, 0, 0, 0);
        cajaTexto.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView textoTitulo = new TextView(this);
        textoTitulo.setText(titulo);
        textoTitulo.setTextColor(Color.parseColor("#E1B86A"));
        textoTitulo.setTextSize(18);
        textoTitulo.setTypeface(null, Typeface.BOLD);
        cajaTexto.addView(textoTitulo);

        TextView textoDetalles = new TextView(this);
        StringBuilder sbDetalles = new StringBuilder(tipo != null ? tipo : "");
        if (!precio.isEmpty()) {
            sbDetalles.append(" | ").append(precio);
        }
        if (!formato.equals("N/A")) {
            sbDetalles.append(" | ").append(formato);
        }
        if (negociable) {
            sbDetalles.append(" | Negociable");
        }
        if (enMapa) {
            sbDetalles.append(" | 📍 En el mapa");
        }
        String detallesCompletos = sbDetalles.toString();
        textoDetalles.setText(detallesCompletos);
        textoDetalles.setTextColor(Color.parseColor("#F2E6D8"));
        textoDetalles.setPadding(0, 10, 0, 10);
        cajaTexto.addView(textoDetalles);

        TextView textoAutor = new TextView(this);
        textoAutor.setText("Por: " + autorNick);
        textoAutor.setTextColor(Color.parseColor("#E1B86A"));
        textoAutor.setTextSize(11);
        cajaTexto.addView(textoAutor);

        if (TIPO_VENTA.equals(tipo)) {
            RatingBar barraCondicion = new RatingBar(this, null, android.R.attr.ratingBarStyleSmall);
            barraCondicion.setNumStars(5);
            barraCondicion.setRating(condicion);
            barraCondicion.setIsIndicator(true);
            barraCondicion.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E1B86A")));
            barraCondicion.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            cajaTexto.addView(barraCondicion);
        }

        contenidoTarjeta.addView(cajaTexto);
        tarjetaPublicacion.addView(contenidoTarjeta);
        llPublicaciones.addView(tarjetaPublicacion);

        tarjetaPublicacion.setOnClickListener(v ->
                mostrarDialogoVerPublicacion(idDoc, titulo, cuerpoTexto, detallesCompletos, imagenBase64, autorUid, autorNick,
                        enMapa ? lat : null, enMapa ? lon : null));
    }

    private void agregarVistaComentario(LinearLayout contenedor, DocumentReference refPub, String idComentario, String nick, String texto, Long timestamp, boolean puedeBorrar) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
        fila.setPadding(0, dp(6), 0, dp(6));

        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        String fecha = timestamp != null
                ? new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(timestamp))
                : "";
        TextView tvNick = new TextView(this);
        tvNick.setText((nick != null && !nick.isEmpty() ? nick : "Duelista") + (fecha.isEmpty() ? "" : " · " + fecha));
        tvNick.setTextColor(Color.parseColor("#E1B86A"));
        tvNick.setTextSize(11);
        tvNick.setTypeface(null, Typeface.BOLD);
        caja.addView(tvNick);

        TextView tvTexto = new TextView(this);
        tvTexto.setText(texto != null ? texto : "");
        tvTexto.setTextColor(Color.parseColor("#F2E6D8"));
        tvTexto.setTextSize(13);
        caja.addView(tvTexto);

        fila.addView(caja);

        if (puedeBorrar) {
            TextView tvBorrar = new TextView(this);
            tvBorrar.setText("🗑️");
            tvBorrar.setTextSize(16);
            tvBorrar.setPadding(dp(10), dp(6), dp(4), dp(6));
            tvBorrar.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar comentario")
                            .setMessage("¿Seguro que quieres eliminar este comentario?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Eliminar", (d, w) ->
                                    refPub.collection("comentarios").document(idComentario).delete()
                                            .addOnSuccessListener(unused ->
                                                    Toast.makeText(this, "Comentario eliminado", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "No se pudo eliminar el comentario", Toast.LENGTH_SHORT).show())
                            )
                            .show()
            );
            fila.addView(tvBorrar);
        }

        contenedor.addView(fila);
    }

    @SuppressWarnings("unchecked")
    private void mostrarDialogoVerPublicacion(String idDoc, String titulo, String cuerpoTexto, String detalles, String imagenBase64, String autorUid, String autorNick, Double lat, Double lon) {
        Dialog dialogo = new Dialog(this);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.dialog_ver_publicacion);

        Window window = dialogo.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1A1A24")));
        }

        ImageView ivImagen = dialogo.findViewById(R.id.iv_ver_imagen);
        TextView tvTitulo = dialogo.findViewById(R.id.tv_ver_titulo);
        TextView tvDetalles = dialogo.findViewById(R.id.tv_ver_detalles);
        TextView tvCuerpo = dialogo.findViewById(R.id.tv_ver_cuerpo);
        Button btnLike = dialogo.findViewById(R.id.btn_like);
        Button btnEliminar = dialogo.findViewById(R.id.btn_eliminar_publicacion);
        Button btnVerMapa = dialogo.findViewById(R.id.btn_ver_en_mapa);
        LinearLayout llListaComentarios = dialogo.findViewById(R.id.ll_lista_comentarios);
        EditText etNuevoComentario = dialogo.findViewById(R.id.et_nuevo_comentario);
        Button btnEnviarComentario = dialogo.findViewById(R.id.btn_enviar_comentario);
        Button btnCerrar = dialogo.findViewById(R.id.btn_cerrar_dialogo);

        Bitmap bitmap = decodificarBase64(imagenBase64, 1);
        if (bitmap != null) {
            ivImagen.setImageBitmap(bitmap);
            ivImagen.setVisibility(View.VISIBLE);
        }

        tvTitulo.setText(titulo);
        tvDetalles.setText(detalles + " | Por: " + autorNick);
        tvCuerpo.setText(cuerpoTexto);

        final FirebaseUser usuarioActual = auth.getCurrentUser();
        final String miUid = usuarioActual != null ? usuarioActual.getUid() : null;
        final boolean esAutor = miUid != null && autorUid != null && autorUid.equals(miUid);
        final DocumentReference refPub = db.collection("publicaciones").document(idDoc);

        btnEliminar.setVisibility(esAutor ? View.VISIBLE : View.GONE);

        if (lat != null && lon != null) {
            btnVerMapa.setVisibility(View.VISIBLE);
            btnVerMapa.setOnClickListener(v -> {
                dialogo.dismiss();
                Intent intentMapa = new Intent(this, vistaMapa.class);
                intentMapa.putExtra("FOCUS_LAT", lat.doubleValue());
                intentMapa.putExtra("FOCUS_LON", lon.doubleValue());
                startActivity(intentMapa);
            });
        } else {
            btnVerMapa.setVisibility(View.GONE);
        }

        btnEliminar.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Eliminar publicación")
                        .setMessage("¿Seguro que quieres eliminar esta publicación? Esta acción no se puede deshacer.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar", (d, w) ->
                                refPub.collection("comentarios").get()
                                        .addOnSuccessListener(comentarios -> {
                                            WriteBatch lote = db.batch();
                                            for (DocumentSnapshot c : comentarios.getDocuments()) {
                                                lote.delete(c.getReference());
                                            }
                                            lote.delete(refPub);
                                            lote.commit()
                                                    .addOnSuccessListener(unused -> {
                                                        Toast.makeText(this, "Publicación eliminada", Toast.LENGTH_SHORT).show();
                                                        dialogo.dismiss();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "No se pudo eliminar la publicación", Toast.LENGTH_SHORT).show());
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "No se pudo eliminar la publicación", Toast.LENGTH_SHORT).show())
                        )
                        .show()
        );

        final boolean[] dioLike = {false};

        final ListenerRegistration registroLikes = refPub.addSnapshotListener((snap, error) -> {
            if (error != null || snap == null) return;
            if (!snap.exists()) {
                dialogo.dismiss();
                return;
            }
            List<String> uids = (List<String>) snap.get("likes_uids");
            int total = uids != null ? uids.size() : 0;
            dioLike[0] = uids != null && miUid != null && uids.contains(miUid);
            btnLike.setText("❤️ Me gusta (" + total + ")");
            btnLike.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(dioLike[0] ? "#B84A3A" : "#3D2C45")));
        });

        btnLike.setOnClickListener(v -> {
            if (miUid == null) {
                Toast.makeText(this, "Debes iniciar sesión para dar Me gusta.", Toast.LENGTH_SHORT).show();
                return;
            }
            Object cambio = dioLike[0] ? FieldValue.arrayRemove(miUid) : FieldValue.arrayUnion(miUid);
            refPub.update("likes_uids", cambio)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "No se pudo registrar tu Me gusta", Toast.LENGTH_SHORT).show());
        });

        final ListenerRegistration registroComentarios = refPub.collection("comentarios")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    llListaComentarios.removeAllViews();
                    for (QueryDocumentSnapshot c : value) {
                        String uidComentario = c.getString("autor_uid");
                        boolean puedeBorrar = miUid != null && (miUid.equals(uidComentario) || esAutor);
                        agregarVistaComentario(llListaComentarios, refPub, c.getId(),
                                c.getString("autor_nick"), c.getString("texto"), c.getLong("timestamp"), puedeBorrar);
                    }
                });

        btnEnviarComentario.setOnClickListener(v -> {
            if (miUid == null) {
                Toast.makeText(this, "Debes iniciar sesión para comentar.", Toast.LENGTH_SHORT).show();
                return;
            }
            String textoComentario = etNuevoComentario.getText().toString().trim();
            if (textoComentario.isEmpty()) return;
            if (textoComentario.length() > MAX_CARACTERES_COMENTARIO) {
                Toast.makeText(this, "El comentario no puede superar " + MAX_CARACTERES_COMENTARIO + " caracteres.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> comentario = new HashMap<>();
            comentario.put("texto", textoComentario);
            comentario.put("autor_uid", miUid);
            comentario.put("autor_nick", new DBHelper(this).obtenerNicknameLocal());
            comentario.put("timestamp", System.currentTimeMillis());

            etNuevoComentario.setText("");
            refPub.collection("comentarios").add(comentario)
                    .addOnSuccessListener(ref ->
                            Toast.makeText(this, "Comentario añadido", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "No se pudo enviar el comentario", Toast.LENGTH_SHORT).show());
        });

        btnCerrar.setOnClickListener(v -> dialogo.dismiss());

        dialogo.setOnDismissListener(d -> {
            registroLikes.remove();
            registroComentarios.remove();
        });

        dialogo.show();
    }
}