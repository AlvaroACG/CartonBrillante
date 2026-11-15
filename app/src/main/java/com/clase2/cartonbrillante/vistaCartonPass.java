package com.clase2.cartonbrillante;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class vistaCartonPass extends AppCompatActivity {

    private static final String PREFS_CP = "CartonPassPrefs";
    private static final String KEY_ACTIVO = "cp_activo";
    private static final String KEY_GUARDAR_GPS = "cp_guardar_gps";
    private static final String KEY_NICK = "cp_nick";
    private static final String KEY_SALUDO = "cp_saludo";
    private static final String KEY_DECK = "cp_deck";
    private static final String KEY_INVOCACION = "cp_invocacion";
    private static final String KEY_AVATAR_B64 = "cp_avatar_base64";

    private static final String[] SALUDOS_PREDEFINIDOS = {
            "¡Hola! ¡Es hora del Duelo!",
            "¡Buscando cambios de carpeta!",
            "¡Disponible para duelos amistosos!",
            "¡Testeando para el próximo torneo!",
            "¡Coleccionista de rarezas brillantes!",
            "¡Mi mazo no tiene cartas patéticas!",
            "¡Confiando en el corazón de las cartas!"
    };

    private static final String[] TIPOS_INVOCACION = {
            "Ninguna (No mostrar)",
            "Fusión",
            "Sincronía (Synchro)",
            "Xyz",
            "Péndulo (Pendulum)",
            "Enlace (Link)",
            "Ritual",
            "Tributo / Normal"
    };

    private SwitchCompat swActivarCartonPass, swGuardarUbicacionCp;
    private TextView tvEstadoCartonPass, tvTituloPlazaCp;

    private ImageView ivMiAvatarCp;
    private TextView tvMiNickCp, tvMiPaisRegCp, tvMiSaludoCp, tvMiDeckCp, tvMiInvocacionCp;
    private Button btnEditarMiTarjetaCp, btnSimularCruce, btnCruceManual, btnVerEnMapaCp;
    private RecyclerView rvEncuentros;

    private ImageView ivDialogAvatarPreview;
    private byte[] avatarTemporalBytes = null;

    private DBHelper dbHelper;
    private CartonPassAdapter adapter;
    private List<EncuentroCartonPass> listaEncuentros = new ArrayList<>();
    private List<String> opcionesDecksUsuario = new ArrayList<>();

    private final ActivityResultLauncher<String> selectorAvatarLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    procesarAvatarCuadrado(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> permisoUbicacionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            concedido -> {
                if (!concedido) {
                    Toast.makeText(this, "Sin permiso GPS se usará ubicación referencial cuando el switch esté activo", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vista_carton_pass);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DBHelper(this);
        Button btnVolverCartonPass = findViewById(R.id.btnVolverCartonPass);
        if (btnVolverCartonPass != null) {
            btnVolverCartonPass.setOnClickListener(v -> finish());
        }

        swActivarCartonPass = findViewById(R.id.swActivarCartonPass);
        swGuardarUbicacionCp = findViewById(R.id.swGuardarUbicacionCp);
        tvEstadoCartonPass = findViewById(R.id.tvEstadoCartonPass);
        tvTituloPlazaCp = findViewById(R.id.tvTituloPlazaCp);

        ivMiAvatarCp = findViewById(R.id.ivMiAvatarCp);
        tvMiNickCp = findViewById(R.id.tvMiNickCp);
        tvMiPaisRegCp = findViewById(R.id.tvMiPaisRegCp);
        tvMiSaludoCp = findViewById(R.id.tvMiSaludoCp);
        tvMiDeckCp = findViewById(R.id.tvMiDeckCp);
        tvMiInvocacionCp = findViewById(R.id.tvMiInvocacionCp);

        btnEditarMiTarjetaCp = findViewById(R.id.btnEditarMiTarjetaCp);
        btnSimularCruce = findViewById(R.id.btnSimularCruce);
        btnCruceManual = findViewById(R.id.btnCruceManual);
        btnVerEnMapaCp = findViewById(R.id.btnVerEnMapaCp);
        rvEncuentros = findViewById(R.id.rvEncuentrosCartonPass);

        cargarDecksDisponibles();
        mostrarMiTarjetaEnPantalla();

        rvEncuentros.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartonPassAdapter(listaEncuentros, new CartonPassAdapter.OnEncuentroListener() {
            @Override
            public void onEncuentroClick(EncuentroCartonPass encuentro) {
                mostrarFichaEncuentro(encuentro);
            }

            @Override
            public void onEncuentroLongClick(EncuentroCartonPass encuentro) {
                confirmarBorrarEncuentro(encuentro);
            }
        });
        rvEncuentros.setAdapter(adapter);

        cargarEncuentrosDesdeSQLite();

        // Verificar permiso de GPS si el switch de ubicación está activo
        if (swGuardarUbicacionCp.isChecked()) {
            solicitarPermisoGpsSiFalta();
        }

        swGuardarUbicacionCp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences(PREFS_CP, Context.MODE_PRIVATE).edit().putBoolean(KEY_GUARDAR_GPS, isChecked).apply();
            if (isChecked) {
                solicitarPermisoGpsSiFalta();
            }
        });

        swActivarCartonPass.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_CP, Context.MODE_PRIVATE);
            String nickGuardado = prefs.getString(KEY_NICK, dbHelper.obtenerNicknameLocal());

            if (isChecked && (nickGuardado == null || nickGuardado.trim().isEmpty())) {
                swActivarCartonPass.setChecked(false);
                Toast.makeText(this, "Primero configura tu Nickname en 'Editar Mi Tarjeta'", Toast.LENGTH_LONG).show();
                mostrarDialogoEditarMiTarjeta();
                return;
            }

            prefs.edit().putBoolean(KEY_ACTIVO, isChecked).apply();
            actualizarEstadoVisualRadar(isChecked);
        });

        btnEditarMiTarjetaCp.setOnClickListener(v -> mostrarDialogoEditarMiTarjeta());

        btnSimularCruce.setOnClickListener(v -> {
            if (!swActivarCartonPass.isChecked()) {
                Toast.makeText(this, "Activa CartonPass arriba para detectar duelistas cercanos", Toast.LENGTH_SHORT).show();
                return;
            }
            emularEncuentroCercano();
        });

        btnCruceManual.setOnClickListener(v -> {
            if (!swActivarCartonPass.isChecked()) {
                Toast.makeText(this, "Activa CartonPass primero para intercambiar tarjetas", Toast.LENGTH_SHORT).show();
                return;
            }
            mostrarDialogoCruceDirecto();
        });

        btnVerEnMapaCp.setOnClickListener(v -> {
            Intent intent = new Intent(this, vistaMapa.class);
            startActivity(intent);
        });
    }

    private void solicitarPermisoGpsSiFalta() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permisoUbicacionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // Obtiene las coordenadas actuales si el Switch de Ubicación está encendido y el GPS del teléfono está activo
    @SuppressLint("MissingPermission")
    private double[] obtenerCoordenadasSiUbicacionActiva(double latReferencia, double lonReferencia) {
        if (!swGuardarUbicacionCp.isChecked()) {
            return new double[]{0.0, 0.0}; // No guardar ubicación si el usuario lo apagó
        }

        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEncendido = lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

            if (!gpsEncendido) {
                Toast.makeText(this, "Ubicación del teléfono apagada: encuentro guardado sin coordenadas", Toast.LENGTH_SHORT).show();
                return new double[]{0.0, 0.0};
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) {
                    loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (loc != null) {
                    // Pequeña variación de metros para que varios encuentros en el mismo lugar no se tapen en el mapa
                    double offsetLat = (new Random().nextDouble() - 0.5) * 0.002;
                    double offsetLon = (new Random().nextDouble() - 0.5) * 0.002;
                    return new double[]{loc.getLatitude() + offsetLat, loc.getLongitude() + offsetLon};
                }
            }
        } catch (Exception ignored) {}

        // Si el GPS está encendido pero bajo techo sin señal, usa la coordenada de referencia
        return new double[]{latReferencia, lonReferencia};
    }

    private void cargarDecksDisponibles() {
        opcionesDecksUsuario.clear();
        opcionesDecksUsuario.add("Ninguno (No mostrar)");
        List<Deck> misDecks = dbHelper.obtenerTodosLosDecks();
        for (Deck d : misDecks) {
            opcionesDecksUsuario.add(d.getNombreDeck() + " (" + d.getRegulacion() + ")");
        }
    }

    private void mostrarMiTarjetaEnPantalla() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CP, Context.MODE_PRIVATE);
        boolean activo = prefs.getBoolean(KEY_ACTIVO, false);
        boolean guardarGps = prefs.getBoolean(KEY_GUARDAR_GPS, true);

        String nick = prefs.getString(KEY_NICK, "");
        if (nick.isEmpty()) {
            nick = dbHelper.obtenerNicknameLocal();
            if (!nick.isEmpty()) {
                prefs.edit().putString(KEY_NICK, nick).apply();
            }
        }

        String saludo = prefs.getString(KEY_SALUDO, SALUDOS_PREDEFINIDOS[0]);
        String deck = prefs.getString(KEY_DECK, "Ninguno (No mostrar)");
        String inv = prefs.getString(KEY_INVOCACION, "Ninguna (No mostrar)");
        String pais = RegulacionHelper.obtenerPaisUsuario(this);
        String reg = RegulacionHelper.obtenerRegulacionActual(this);

        tvMiNickCp.setText(nick.isEmpty() ? "Sin Nickname" : nick);
        tvMiPaisRegCp.setText(pais + " • " + reg);
        if ("OCG".equalsIgnoreCase(reg)) {
            tvMiPaisRegCp.setBackgroundColor(Color.parseColor("#D32F2F"));
        } else {
            tvMiPaisRegCp.setBackgroundColor(Color.parseColor("#5C6BC0"));
        }

        tvMiSaludoCp.setText("“" + saludo + "”");

        String textoDeck = (deck.equals("Ninguno (No mostrar)")) ? "Secreto / No mostrar" : deck;
        tvMiDeckCp.setText("🎴 Deck: " + textoDeck);

        String textoInv = (inv.equals("Ninguna (No mostrar)")) ? "Sin preferencia" : inv;
        tvMiInvocacionCp.setText("✨ Inv: " + textoInv);

        String avatarB64 = prefs.getString(KEY_AVATAR_B64, null);
        if (avatarB64 != null && !avatarB64.isEmpty()) {
            byte[] bytes = Base64.decode(avatarB64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            ivMiAvatarCp.setImageBitmap(bmp);
        } else {
            ivMiAvatarCp.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        swActivarCartonPass.setChecked(activo);
        swGuardarUbicacionCp.setChecked(guardarGps);
        actualizarEstadoVisualRadar(activo);
    }

    private void mostrarDialogoEditarMiTarjeta() {
        cargarDecksDisponibles();
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editar_carton_pass, null);

        ivDialogAvatarPreview = dialogView.findViewById(R.id.ivDialogAvatarCp);
        Button btnElegirAvatar = dialogView.findViewById(R.id.btnElegirAvatarCp);
        EditText etNick = dialogView.findViewById(R.id.etDialogNickCp);
        Spinner spSaludo = dialogView.findViewById(R.id.spDialogSaludoCp);
        Spinner spDeck = dialogView.findViewById(R.id.spDialogDeckCp);
        Spinner spInvocacion = dialogView.findViewById(R.id.spDialogInvocacionCp);

        spSaludo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SALUDOS_PREDEFINIDOS));
        spDeck.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opcionesDecksUsuario));
        spInvocacion.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TIPOS_INVOCACION));

        SharedPreferences prefs = getSharedPreferences(PREFS_CP, Context.MODE_PRIVATE);
        String nickActual = prefs.getString(KEY_NICK, dbHelper.obtenerNicknameLocal());
        String saludoActual = prefs.getString(KEY_SALUDO, SALUDOS_PREDEFINIDOS[0]);
        String deckActual = prefs.getString(KEY_DECK, opcionesDecksUsuario.get(0));
        String invActual = prefs.getString(KEY_INVOCACION, TIPOS_INVOCACION[0]);
        String avatarB64 = prefs.getString(KEY_AVATAR_B64, null);

        etNick.setText(nickActual);

        if (avatarB64 != null && !avatarB64.isEmpty()) {
            avatarTemporalBytes = Base64.decode(avatarB64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(avatarTemporalBytes, 0, avatarTemporalBytes.length);
            ivDialogAvatarPreview.setImageBitmap(bmp);
        } else {
            avatarTemporalBytes = null;
        }

        for (int i = 0; i < SALUDOS_PREDEFINIDOS.length; i++) {
            if (SALUDOS_PREDEFINIDOS[i].equals(saludoActual)) {
                spSaludo.setSelection(i);
                break;
            }
        }
        for (int i = 0; i < opcionesDecksUsuario.size(); i++) {
            if (opcionesDecksUsuario.get(i).equals(deckActual)) {
                spDeck.setSelection(i);
                break;
            }
        }
        for (int i = 0; i < TIPOS_INVOCACION.length; i++) {
            if (TIPOS_INVOCACION[i].equals(invActual)) {
                spInvocacion.setSelection(i);
                break;
            }
        }

        btnElegirAvatar.setOnClickListener(v -> selectorAvatarLauncher.launch("image/*"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Editar Mi Tarjeta CartonPass")
                .setView(dialogView)
                .setPositiveButton("Guardar Tarjeta", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(v -> {
                String nuevoNick = etNick.getText().toString().trim();
                if (nuevoNick.isEmpty()) {
                    Toast.makeText(this, "El Nickname y el Saludo son obligatorios", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_NICK, nuevoNick);
                editor.putString(KEY_SALUDO, spSaludo.getSelectedItem().toString());
                editor.putString(KEY_DECK, spDeck.getSelectedItem().toString());
                editor.putString(KEY_INVOCACION, spInvocacion.getSelectedItem().toString());

                if (avatarTemporalBytes != null) {
                    editor.putString(KEY_AVATAR_B64, Base64.encodeToString(avatarTemporalBytes, Base64.DEFAULT));
                }
                editor.apply();

                mostrarMiTarjetaEnPantalla();
                Toast.makeText(this, "Tarjeta CartonPass actualizada", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void procesarAvatarCuadrado(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (original != null) {
                int dimension = Math.min(original.getWidth(), original.getHeight());
                int x = (original.getWidth() - dimension) / 2;
                int y = (original.getHeight() - dimension) / 2;

                Bitmap cuadrado = Bitmap.createBitmap(original, x, y, dimension, dimension);
                Bitmap escalado = Bitmap.createScaledBitmap(cuadrado, 200, 200, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                escalado.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                avatarTemporalBytes = stream.toByteArray();

                if (ivDialogAvatarPreview != null) {
                    ivDialogAvatarPreview.setImageBitmap(escalado);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar el avatar", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarEstadoVisualRadar(boolean activo) {
        if (activo) {
            tvEstadoCartonPass.setText("🟢 Activo (Emitiendo tu Tarjeta en local)");
            tvEstadoCartonPass.setTextColor(Color.parseColor("#4CAF50"));
            btnSimularCruce.setAlpha(1.0f);
            btnCruceManual.setAlpha(1.0f);
        } else {
            tvEstadoCartonPass.setText("⚪ Desactivado (Modo Privado: No compartes datos)");
            tvEstadoCartonPass.setTextColor(Color.parseColor("#BDBDBD"));
            btnSimularCruce.setAlpha(0.5f);
            btnCruceManual.setAlpha(0.5f);
        }
    }

    private void cargarEncuentrosDesdeSQLite() {
        listaEncuentros = dbHelper.obtenerTodosLosEncuentrosCartonPass();
        adapter.actualizarLista(listaEncuentros);
        tvTituloPlazaCp.setText("Duelistas Encontrados (" + listaEncuentros.size() + ")");
    }

    private byte[] generarAvatarCuadradoIniciales(String nickname, String colorHex) {
        Bitmap bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.parseColor(colorHex));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(64f);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);

        String inicial = (nickname != null && !nickname.isEmpty()) ? nickname.substring(0, Math.min(2, nickname.length())).toUpperCase() : "D";
        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(inicial, canvas.getWidth() / 2f, yPos, paint);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void emularEncuentroCercano() {
        // Pool con País, Color y Puntos de Encuentro reales para visualizar en el Mapa
        Object[][] poolDuelistas = {
                {"AlbazFan_CL", "¡Buscando cambios de carpeta!", "Branded Despia (TCG)", "Fusión", "Chile", "#D32F2F", -36.8270, -73.0503, "Plaza Independencia, Concepción"},
                {"MermailKing", "¡Testeando para el próximo torneo!", "Mermail Atlantean (TCG)", "Xyz", "Chile", "#1976D2", -36.8285, -73.0372, "Arco UdeC, Concepción"},
                {"TokyoDuelist", "¡Coleccionista de rarezas brillantes!", "Ice Barrier (OCG)", "Sincronía (Synchro)", "Japón", "#00796B", -36.8252, -73.0480, "Galería Centro, Concepción"},
                {"GhotiMaster", "¡Disponible para duelos amistosos!", "Ghoti Deep Beyond (TCG)", "Sincronía (Synchro)", "Argentina", "#512DA8", -36.8295, -73.0521, "Tienda Local OTS, Concepción"},
                {"ShaddollJudge", "¡Hola! ¡Es hora del Duelo!", "Shaddoll Invoked (TCG)", "Fusión", "Chile", "#689F38", -36.8220, -73.0450, "Parque Ecuador, Concepción"},
                {"SeoulSynchro", "¡Mi mazo no tiene cartas patéticas!", "Predaplant (OCG)", "Fusión", "Corea del Sur", "#7B1FA2", -36.8310, -73.0490, "Torneo Regional, Concepción"}
        };

        Random rnd = new Random();
        Object[] elegido = poolDuelistas[rnd.nextInt(poolDuelistas.length)];

        String nick = (String) elegido[0];
        String saludo = (String) elegido[1];
        String deck = (String) elegido[2];
        String inv = (String) elegido[3];
        String pais = (String) elegido[4];
        String regulacion = RegulacionHelper.obtenerRegulacionPorPais(pais);
        String colorHex = (String) elegido[5];
        double latRef = (double) elegido[6];
        double lonRef = (double) elegido[7];
        String lugarRef = (String) elegido[8];

        double[] coords = obtenerCoordenadasSiUbicacionActiva(latRef, lonRef);
        String lugarFinal = (coords[0] != 0.0 || coords[1] != 0.0) ? lugarRef : "Sin ubicación";

        String fechaActual = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
        byte[] avatarGenerado = generarAvatarCuadradoIniciales(nick, colorHex);

        EncuentroCartonPass nuevo = new EncuentroCartonPass(
                nick, saludo, deck, inv, pais, regulacion, fechaActual, avatarGenerado,
                coords[0], coords[1], lugarFinal
        );

        dbHelper.registrarEncuentroCartonPass(nuevo);
        cargarEncuentrosDesdeSQLite();

        String avisoMapa = (coords[0] != 0.0) ? " (📍 Guardado en el Mapa)" : "";
        Toast.makeText(this, "🟢 ¡CartonPass de " + nick + " (" + pais + " • " + regulacion + ")!" + avisoMapa, Toast.LENGTH_LONG).show();
    }

    private void mostrarDialogoCruceDirecto() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);
        layout.setBackgroundColor(Color.parseColor("#1B1B28"));

        EditText etNickRival = new EditText(this);
        etNickRival.setHint("Nickname del Duelista *");
        etNickRival.setTextColor(Color.WHITE);
        etNickRival.setHintTextColor(Color.GRAY);
        layout.addView(etNickRival);

        // Selector de País para determinar automáticamente si el encuentro es TCG u OCG
        TextView tvLabelPais = new TextView(this);
        tvLabelPais.setText("País del Duelista (Asigna TCG / OCG):");
        tvLabelPais.setTextColor(Color.parseColor("#8C9EFF"));
        tvLabelPais.setPadding(0, 16, 0, 4);
        layout.addView(tvLabelPais);

        Spinner spPaisRival = new Spinner(this);
        spPaisRival.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, RegulacionHelper.LISTA_PAISES_MUNDO));
        layout.addView(spPaisRival);

        Spinner spSaludoRival = new Spinner(this);
        spSaludoRival.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SALUDOS_PREDEFINIDOS));
        layout.addView(spSaludoRival);

        EditText etDeckRival = new EditText(this);
        etDeckRival.setHint("Su Deck (Opcional)");
        etDeckRival.setTextColor(Color.WHITE);
        etDeckRival.setHintTextColor(Color.GRAY);
        layout.addView(etDeckRival);

        Spinner spInvRival = new Spinner(this);
        spInvRival.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TIPOS_INVOCACION));
        layout.addView(spInvRival);

        EditText etLugarCruce = new EditText(this);
        etLugarCruce.setHint("Nombre del Lugar / Tienda (Opcional)");
        etLugarCruce.setTextColor(Color.WHITE);
        etLugarCruce.setHintTextColor(Color.GRAY);
        layout.addView(etLugarCruce);

        new AlertDialog.Builder(this)
                .setTitle("Registrar Cruce Directo Offline")
                .setView(layout)
                .setPositiveButton("Registrar Encuentro", (dialog, which) -> {
                    String nick = etNickRival.getText().toString().trim();
                    if (nick.isEmpty()) {
                        Toast.makeText(this, "El Nickname es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String pais = spPaisRival.getSelectedItem().toString();
                    String regulacion = RegulacionHelper.obtenerRegulacionPorPais(pais);
                    String saludo = spSaludoRival.getSelectedItem().toString();
                    String deck = etDeckRival.getText().toString().trim();
                    String inv = spInvRival.getSelectedItem().toString();
                    String lugar = etLugarCruce.getText().toString().trim();
                    if (lugar.isEmpty()) lugar = "Encuentro Directo";

                    double[] coords = obtenerCoordenadasSiUbicacionActiva(-36.8270, -73.0503);
                    String fecha = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
                    byte[] avatar = generarAvatarCuadradoIniciales(nick, "#5C6BC0");

                    EncuentroCartonPass nuevo = new EncuentroCartonPass(
                            nick, saludo, deck, inv, pais, regulacion, fecha, avatar,
                            coords[0], coords[1], lugar
                    );
                    dbHelper.registrarEncuentroCartonPass(nuevo);
                    cargarEncuentrosDesdeSQLite();
                    Toast.makeText(this, "🟢 Guardado (" + pais + " • " + regulacion + ")", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarFichaEncuentro(EncuentroCartonPass e) {
        String deck = (e.getDeckCompartido() == null || e.getDeckCompartido().isEmpty() || e.getDeckCompartido().equals("Ninguno (No mostrar)"))
                ? "No compartido" : e.getDeckCompartido();
        String inv = (e.getInvocacionPreferida() == null || e.getInvocacionPreferida().isEmpty() || e.getInvocacionPreferida().equals("Ninguna (No mostrar)"))
                ? "No compartida" : e.getInvocacionPreferida();
        String ubicacionTxt = e.tieneUbicacionValida()
                ? e.getNombreLugar() + " (" + String.format(Locale.US, "%.4f, %.4f", e.getLatitud(), e.getLongitud()) + ")"
                : "No registrada";

        String detalle = "💬 Saludo:\n“" + e.getSaludo() + "”\n\n" +
                "🌎 País: " + e.getPais() + "\n" +
                "⚖️ Regulación: " + e.getRegulacion() + "\n" +
                "🎴 Deck: " + deck + "\n" +
                "✨ Invocación Favorita: " + inv + "\n" +
                "📍 Lugar del Encuentro: " + ubicacionTxt + "\n\n" +
                "🤝 Veces que se han cruzado: " + e.getVecesEncontrado() + "\n" +
                "🕒 Último cruce: " + e.getFechaHora();

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("🟢 Duelista: " + e.getNickname())
                .setMessage(detalle)
                .setPositiveButton("Cerrar", null);

        if (e.tieneUbicacionValida()) {
            builder.setNeutralButton("🗺️ Ver en Mapa", (d, w) -> {
                Intent intent = new Intent(this, vistaMapa.class);
                intent.putExtra("FOCUS_LAT", e.getLatitud());
                intent.putExtra("FOCUS_LON", e.getLongitud());
                startActivity(intent);
            });
        }

        builder.show();
    }

    private void confirmarBorrarEncuentro(EncuentroCartonPass e) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Contacto")
                .setMessage("¿Deseas borrar a \"" + e.getNickname() + "\" de tu historial de CartonPass?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dbHelper.eliminarEncuentroCartonPass(e.getIdEncuentro());
                    cargarEncuentrosDesdeSQLite();
                    Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}