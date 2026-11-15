package com.clase2.cartonbrillante;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class vistaMapa extends AppCompatActivity {

    private static class MarcaCloud {
        String origen;
        String idDoc;
        String autorUid;
        String autor;
        String tipo;
        String titulo;
        String detalle;
        String precio;
        String formato;
        String paisReg;
        double lat;
        double lon;
        String fecha;
    }

    private MapView mapView;
    private TextView tvSubtituloMapa;
    private DBHelper dbHelper;
    private FirebaseFirestore dbCloud;
    private FirebaseAuth auth;
    private String filtroActual = "TODOS";

    private final List<MarcaCloud> marcasMapaCloud = new ArrayList<>();
    private final List<MarcaCloud> marcasForoCloud = new ArrayList<>();
    private ListenerRegistration listenerMapaCloud;
    private ListenerRegistration listenerForoCloud;

    private MyLocationNewOverlay overlayUbicacion;
    private ActivityResultLauncher<String> selectorPermisoUbicacion;
    private boolean hayFoco = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vista_mapa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DBHelper(this);
        dbCloud = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvSubtituloMapa = findViewById(R.id.tvSubtituloMapa);
        mapView = findViewById(R.id.mapViewCartonPass);

        Button btnVolverMapa = findViewById(R.id.btnVolverMapa);
        btnVolverMapa.setOnClickListener(v -> finish());

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.5);

        hayFoco = getIntent() != null && getIntent().hasExtra("FOCUS_LAT");

        GeoPoint puntoInicial = new GeoPoint(-36.8270, -73.0503);
        if (hayFoco) {
            double lat = getIntent().getDoubleExtra("FOCUS_LAT", -36.8270);
            double lon = getIntent().getDoubleExtra("FOCUS_LON", -73.0503);
            puntoInicial = new GeoPoint(lat, lon);
            mapView.getController().setZoom(17.0);
        }
        mapView.getController().setCenter(puntoInicial);

        selectorPermisoUbicacion = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    if (concedido) {
                        iniciarUbicacionEnVivo();
                    } else {
                        Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.btnFiltroTodos).setOnClickListener(v -> cambiarFiltro("TODOS"));
        findViewById(R.id.btnFiltroDuelos).setOnClickListener(v -> cambiarFiltro("DUELO"));
        findViewById(R.id.btnFiltroVentas).setOnClickListener(v -> cambiarFiltro("VENTA"));
        findViewById(R.id.btnFiltroBusquedas).setOnClickListener(v -> cambiarFiltro("BUSQUEDA"));
        findViewById(R.id.btnFiltroEventos).setOnClickListener(v -> cambiarFiltro("EVENTO"));
        findViewById(R.id.btnFiltroCartonPass).setOnClickListener(v -> cambiarFiltro("CARTONPASS"));

        findViewById(R.id.btnCentrarUbicacion).setOnClickListener(v -> centrarEnMiUbicacion());

        Button btnPublicar = findViewById(R.id.btnPublicarEnMapa);
        btnPublicar.setOnClickListener(v -> mostrarDialogoPublicarEnMapa());

        if (tienePermisoUbicacion()) {
            iniciarUbicacionEnVivo();
        } else {
            selectorPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        cargarMarcadoresEnMapa();
        sincronizarMarcasCloudEnVivo();
        sincronizarPublicacionesForoEnVivo();
    }

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void iniciarUbicacionEnVivo() {
        if (overlayUbicacion == null) {
            overlayUbicacion = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            if (!hayFoco) {
                overlayUbicacion.runOnFirstFix(() -> runOnUiThread(() -> {
                    GeoPoint posicion = overlayUbicacion.getMyLocation();
                    if (posicion != null) {
                        mapView.getController().animateTo(posicion);
                    }
                }));
            }
            mapView.getOverlays().add(overlayUbicacion);
        }
        overlayUbicacion.enableMyLocation();
        mapView.invalidate();
    }

    private void centrarEnMiUbicacion() {
        if (!tienePermisoUbicacion()) {
            selectorPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        iniciarUbicacionEnVivo();

        GeoPoint posicion = overlayUbicacion.getMyLocation();
        if (posicion != null) {
            mapView.getController().setZoom(17.0);
            mapView.getController().animateTo(posicion);
        } else {
            Toast.makeText(this, "Obteniendo tu ubicación...", Toast.LENGTH_SHORT).show();
            overlayUbicacion.runOnFirstFix(() -> runOnUiThread(() -> {
                GeoPoint primera = overlayUbicacion.getMyLocation();
                if (primera != null) {
                    mapView.getController().setZoom(17.0);
                    mapView.getController().animateTo(primera);
                }
            }));
        }
    }

    private void cambiarFiltro(String nuevoFiltro) {
        this.filtroActual = nuevoFiltro;
        cargarMarcadoresEnMapa();
    }

    private boolean pasaFiltro(String tipo) {
        if (filtroActual.equals("CARTONPASS")) return false;
        return filtroActual.equals("TODOS") || filtroActual.equals(tipo);
    }

    private void cargarMarcadoresEnMapa() {
        mapView.getOverlays().clear();
        int totalMarcadores = 0;

        if (filtroActual.equals("TODOS") || filtroActual.equals("CARTONPASS")) {
            List<EncuentroCartonPass> encuentros = dbHelper.obtenerEncuentrosConUbicacion();
            for (EncuentroCartonPass e : encuentros) {
                GeoPoint punto = new GeoPoint(e.getLatitud(), e.getLongitud());
                Marker marker = new Marker(mapView);
                marker.setPosition(punto);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle("🟢 CartonPass: " + e.getNickname() + " (" + e.getPais() + " • " + e.getRegulacion() + ")");

                String resumen = "💬 “" + e.getSaludo() + "”\n" +
                        "🎴 Deck: " + e.getDeckCompartido() + "\n" +
                        "✨ Invocación: " + e.getInvocacionPreferida() + "\n" +
                        "📍 Lugar: " + e.getNombreLugar() + "\n" +
                        "🤝 Cruces: x" + e.getVecesEncontrado();
                marker.setSnippet(resumen);

                marker.setOnMarkerClickListener((m, map) -> {
                    new AlertDialog.Builder(this)
                            .setTitle(m.getTitle())
                            .setMessage(resumen + "\n🕒 Fecha: " + e.getFechaHora())
                            .setPositiveButton("Cerrar", null)
                            .show();
                    return true;
                });

                mapView.getOverlays().add(marker);
                totalMarcadores++;
            }
        }

        if (!filtroActual.equals("CARTONPASS")) {
            List<String[]> publicaciones = dbHelper.obtenerPublicacionesMapa();
            for (String[] pub : publicaciones) {
                int idPub = Integer.parseInt(pub[0]);
                String autor = pub[1];
                String tipo = pub[2];
                String titulo = pub[3];
                String detalle = pub[4];
                String paisReg = pub[5];
                double lat = Double.parseDouble(pub[6]);
                double lon = Double.parseDouble(pub[7]);
                String fecha = pub[8];

                if (!filtroActual.equals("TODOS") && !filtroActual.equals(tipo)) {
                    continue;
                }

                String icono = "⚔️ Busco Duelo";
                if ("VENTA".equals(tipo)) icono = "💰 Venta de Carta";
                else if ("BUSQUEDA".equals(tipo)) icono = "🔍 Busco Carta";

                GeoPoint punto = new GeoPoint(lat, lon);
                Marker marker = new Marker(mapView);
                marker.setPosition(punto);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle(icono + " (Local): " + titulo);

                String cuerpo = "👤 Duelista: " + autor + " (" + paisReg + ")\n" +
                        "📌 Carta / Formato: " + titulo + "\n" +
                        "📝 Detalles / Precio: " + detalle + "\n" +
                        "🕒 Publicado: " + fecha;
                marker.setSnippet(cuerpo);

                marker.setOnMarkerClickListener((m, map) -> {
                    new AlertDialog.Builder(this)
                            .setTitle(m.getTitle())
                            .setMessage(cuerpo)
                            .setPositiveButton("Cerrar", null)
                            .setNeutralButton("Eliminar Publicación", (d, w) -> {
                                dbHelper.eliminarPublicacionMapa(idPub);
                                cargarMarcadoresEnMapa();
                                Toast.makeText(this, "Publicación eliminada del mapa", Toast.LENGTH_SHORT).show();
                            })
                            .show();
                    return true;
                });

                mapView.getOverlays().add(marker);
                totalMarcadores++;
            }

            for (MarcaCloud marca : marcasMapaCloud) {
                if (!pasaFiltro(marca.tipo)) continue;
                agregarMarcadorCloud(marca);
                totalMarcadores++;
            }

            for (MarcaCloud marca : marcasForoCloud) {
                if (!pasaFiltro(marca.tipo)) continue;
                agregarMarcadorCloud(marca);
                totalMarcadores++;
            }
        }

        if (overlayUbicacion != null) {
            mapView.getOverlays().add(overlayUbicacion);
        }

        tvSubtituloMapa.setText("Mostrando: " + filtroActual + " (" + totalMarcadores + " marcadores activos)");
        mapView.invalidate();
    }

    private void agregarMarcadorCloud(MarcaCloud marca) {
        boolean esForo = "FORO".equals(marca.origen);

        String icono = esForo ? "🌍 [Foro] ⚔️ Duelo" : "🌍 [Cloud] ⚔️ Duelo";
        if ("VENTA".equals(marca.tipo)) icono = esForo ? "🌍 [Foro] 💰 Venta" : "🌍 [Cloud] 💰 Venta";
        else if ("BUSQUEDA".equals(marca.tipo)) icono = esForo ? "🌍 [Foro] 🔍 Búsqueda" : "🌍 [Cloud] 🔍 Búsqueda";
        else if ("EVENTO".equals(marca.tipo)) icono = "🌍 [Foro] 🏪 Evento de Tienda";

        GeoPoint punto = new GeoPoint(marca.lat, marca.lon);
        Marker marker = new Marker(mapView);
        marker.setPosition(punto);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(icono + ": " + marca.titulo);

        StringBuilder sb = new StringBuilder();
        sb.append("👤 Duelista Global: ").append(marca.autor);
        if (marca.paisReg != null && !marca.paisReg.isEmpty()) {
            sb.append(" (").append(marca.paisReg).append(")");
        }
        sb.append("\n📌 Elemento: ").append(marca.titulo);
        if (marca.precio != null && !marca.precio.isEmpty()) {
            sb.append("\n💲 Precio: ").append(marca.precio);
        }
        if (marca.formato != null && !marca.formato.isEmpty() && !"N/A".equals(marca.formato)) {
            sb.append("\n🎴 Formato: ").append(marca.formato);
        }
        sb.append("\n📝 Nota: ").append(marca.detalle != null ? marca.detalle : "");
        sb.append("\n🕒 Publicado: ").append(marca.fecha != null ? marca.fecha : "");
        String cuerpo = sb.toString();
        marker.setSnippet(cuerpo);

        FirebaseUser usuario = auth.getCurrentUser();
        boolean puedeEliminar = esForo && usuario != null && marca.autorUid != null
                && marca.autorUid.equals(usuario.getUid()) && marca.idDoc != null;

        marker.setOnMarkerClickListener((m, map) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(m.getTitle())
                    .setMessage(cuerpo)
                    .setPositiveButton("Cerrar", null);
            if (puedeEliminar) {
                builder.setNeutralButton("Eliminar Publicación", (d, w) ->
                        dbCloud.collection("publicaciones").document(marca.idDoc).delete()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "Publicación eliminada", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "No se pudo eliminar la publicación", Toast.LENGTH_SHORT).show()));
            }
            builder.show();
            return true;
        });

        mapView.getOverlays().add(marker);
    }

    private void sincronizarMarcasCloudEnVivo() {
        listenerMapaCloud = dbCloud.collection("mapa_publicaciones")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    marcasMapaCloud.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Double latObj = doc.getDouble("latitud");
                        Double lonObj = doc.getDouble("longitud");
                        double lat = latObj != null ? latObj : 0.0;
                        double lon = lonObj != null ? lonObj : 0.0;
                        if (lat == 0.0 && lon == 0.0) continue;

                        MarcaCloud marca = new MarcaCloud();
                        marca.origen = "MAPA";
                        marca.idDoc = doc.getId();
                        marca.autor = doc.getString("autor_nick");
                        marca.tipo = doc.getString("tipo_actividad");
                        marca.titulo = doc.getString("titulo");
                        marca.detalle = doc.getString("detalle");
                        marca.paisReg = doc.getString("pais_regulacion");
                        marca.fecha = doc.getString("fecha_hora");
                        marca.lat = lat;
                        marca.lon = lon;
                        marcasMapaCloud.add(marca);
                    }
                    cargarMarcadoresEnMapa();
                });
    }

    private void sincronizarPublicacionesForoEnVivo() {
        listenerForoCloud = dbCloud.collection("publicaciones")
                .whereEqualTo("en_mapa", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    marcasForoCloud.clear();
                    SimpleDateFormat formato = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
                    for (QueryDocumentSnapshot doc : value) {
                        Double latObj = doc.getDouble("latitud");
                        Double lonObj = doc.getDouble("longitud");
                        if (latObj == null || lonObj == null) continue;
                        if (latObj == 0.0 && lonObj == 0.0) continue;

                        String tipoForo = doc.getString("tipo_actividad");
                        String codigoTipo;
                        if ("Vender Carta".equals(tipoForo)) codigoTipo = "VENTA";
                        else if ("Buscar Carta".equals(tipoForo)) codigoTipo = "BUSQUEDA";
                        else if ("Publicar Evento de Tienda".equals(tipoForo)) codigoTipo = "EVENTO";
                        else continue;

                        String autor = doc.getString("autor_nick");
                        if (autor == null || autor.isEmpty()) autor = doc.getString("autor_nombre");
                        if (autor == null || autor.isEmpty()) autor = "Duelista";

                        Long timestamp = doc.getLong("timestamp");
                        String fecha = timestamp != null ? formato.format(new Date(timestamp)) : "";

                        MarcaCloud marca = new MarcaCloud();
                        marca.origen = "FORO";
                        marca.idDoc = doc.getId();
                        marca.autorUid = doc.getString("autor_uid");
                        marca.autor = autor;
                        marca.tipo = codigoTipo;
                        marca.titulo = doc.getString("titulo");
                        marca.detalle = doc.getString("detalle");
                        marca.precio = doc.getString("precio");
                        marca.formato = doc.getString("formato");
                        marca.paisReg = "";
                        marca.fecha = fecha;
                        marca.lat = latObj;
                        marca.lon = lonObj;
                        marcasForoCloud.add(marca);
                    }
                    cargarMarcadoresEnMapa();
                });
    }

    private void mostrarDialogoPublicarEnMapa() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);
        layout.setBackgroundColor(Color.parseColor("#1B1B28"));

        TextView tvLabelTipo = new TextView(this);
        tvLabelTipo.setText("¿Qué deseas publicar en el mapa?");
        tvLabelTipo.setTextColor(Color.parseColor("#FFD700"));
        layout.addView(tvLabelTipo);

        String[] tiposPublicacion = {
                "⚔️ Busco Duelo (Disponible para jugar)",
                "💰 Venta de Carta",
                "🔍 Búsqueda de Carta"
        };
        Spinner spTipo = new Spinner(this);
        spTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tiposPublicacion));
        layout.addView(spTipo);

        EditText etTitulo = new EditText(this);
        etTitulo.setHint("Nombre de la Carta o Formato de Duelo *");
        etTitulo.setTextColor(Color.WHITE);
        etTitulo.setHintTextColor(Color.GRAY);
        layout.addView(etTitulo);

        EditText etDetalle = new EditText(this);
        etDetalle.setHint("Precio, Rareza, Mesa o Punto de referencia");
        etDetalle.setTextColor(Color.WHITE);
        etDetalle.setHintTextColor(Color.GRAY);
        layout.addView(etDetalle);

        CheckBox cbPublicarNube = new CheckBox(this);
        cbPublicarNube.setText("Hacer público en el Mapa Global (Cloud)");
        cbPublicarNube.setTextColor(Color.parseColor("#F2E6D8"));
        cbPublicarNube.setChecked(true);
        layout.addView(cbPublicarNube);

        new AlertDialog.Builder(this)
                .setTitle("Publicar en el Mapa")
                .setView(layout)
                .setPositiveButton("Publicar Marcador", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    if (titulo.isEmpty()) {
                        Toast.makeText(this, "Ingresa el nombre de la carta o formato", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String detalle = etDetalle.getText().toString().trim();
                    if (detalle.isEmpty()) detalle = "Disponible en esta ubicación";

                    int pos = spTipo.getSelectedItemPosition();
                    String codigoTipo = (pos == 0) ? "DUELO" : (pos == 1) ? "VENTA" : "BUSQUEDA";

                    GeoPoint centroActual = (GeoPoint) mapView.getMapCenter();
                    double offsetLat = (new Random().nextDouble() - 0.5) * 0.0012;
                    double offsetLon = (new Random().nextDouble() - 0.5) * 0.0012;
                    double lat = centroActual.getLatitude() + offsetLat;
                    double lon = centroActual.getLongitude() + offsetLon;

                    String autor = dbHelper.obtenerNicknameLocal();
                    String paisReg = RegulacionHelper.obtenerPaisUsuario(this) + " • " + RegulacionHelper.obtenerRegulacionActual(this);
                    String fecha = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());

                    boolean esPublicoCloud = cbPublicarNube.isChecked();

                    dbHelper.insertarPublicacionMapa(autor, codigoTipo, titulo, detalle, paisReg, lat, lon, fecha);

                    if (esPublicoCloud) {
                        CloudSyncService.publicarMarcaMapaCloud(autor, codigoTipo, titulo, detalle, paisReg, lat, lon, fecha);
                    }

                    Toast.makeText(this, "📍 Publicación creada", Toast.LENGTH_SHORT).show();

                    cargarMarcadoresEnMapa();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (overlayUbicacion != null) overlayUbicacion.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (overlayUbicacion != null) overlayUbicacion.disableMyLocation();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerMapaCloud != null) listenerMapaCloud.remove();
        if (listenerForoCloud != null) listenerForoCloud.remove();
        if (overlayUbicacion != null) overlayUbicacion.disableMyLocation();
    }
}