package com.clase2.cartonbrillante;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class vistaPerfil extends AppCompatActivity {

    private ImageView ivAvatarPerfil;
    private TextView tvNombrePerfil, tvRegulacionPerfil, tvDescripcionPerfil;
    private TextView tvKonamiIdPerfil, tvEstadisticasPerfil, tvDecksPerfil, tvDatosJuezPerfil;
    private View cardDescPerfil, cardPanelJuez;
    private LinearLayout llPanelJuez;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_perfil);

        // 1. Inicializar botón Volver
        Button btnVolver = findViewById(R.id.btnVolverPerfil);
        btnVolver.setOnClickListener(v -> finish());

        // 2. Vincular vistas del XML rediseñado
        ivAvatarPerfil = findViewById(R.id.ivAvatarPerfil);
        tvNombrePerfil = findViewById(R.id.tvNombrePerfil);
        tvRegulacionPerfil = findViewById(R.id.tvRegulacionPerfil);
        tvDescripcionPerfil = findViewById(R.id.tvDescripcionPerfil);
        cardDescPerfil = findViewById(R.id.cardDescPerfil);
        tvKonamiIdPerfil = findViewById(R.id.tvKonamiIdPerfil);
        tvEstadisticasPerfil = findViewById(R.id.tvEstadisticasPerfil);
        tvDecksPerfil = findViewById(R.id.tvDecksPerfil);
        cardPanelJuez = findViewById(R.id.cardPanelJuez);
        llPanelJuez = findViewById(R.id.llPanelJuez);
        tvDatosJuezPerfil = findViewById(R.id.tvDatosJuezPerfil);

        // 3. Cargar datos reales desde SQLite
        cargarDatosUsuarioActivo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar por si el usuario editó su tarjeta o decks antes de volver
        cargarDatosUsuarioActivo();
    }

    private void cargarDatosUsuarioActivo() {
        // Leer sesión activa (revisa SesionUsuario y CartonPassPrefs como respaldo)
        SharedPreferences prefsSesion = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        int idUsuarioActivo = prefsSesion.getInt("id_usuario", -1);
        String usuarioActivoStr = prefsSesion.getString("usuario_activo", null);
        if (usuarioActivoStr == null) {
            usuarioActivoStr = prefsSesion.getString("nickname", null);
        }

        SharedPreferences prefsCp = getSharedPreferences("CartonPassPrefs", MODE_PRIVATE);

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            if (idUsuarioActivo != -1) {
                cursor = db.rawQuery(
                        "SELECT * FROM usuarios WHERE id_usuario = ? LIMIT 1",
                        new String[]{String.valueOf(idUsuarioActivo)}
                );
            } else if (usuarioActivoStr != null && !usuarioActivoStr.isEmpty()) {
                cursor = db.rawQuery(
                        "SELECT * FROM usuarios WHERE correo = ? OR nickname = ? LIMIT 1",
                        new String[]{usuarioActivoStr, usuarioActivoStr}
                );
            } else {
                // Si no se guardó el ID en SharedPreferences, toma el último usuario registrado/activo en SQLite
                cursor = db.rawQuery("SELECT * FROM usuarios ORDER BY id_usuario DESC LIMIT 1", null);
            }

            if (cursor != null && cursor.moveToFirst()) {
                idUsuarioActivo = obtenerIntSeguro(cursor, "id_usuario", idUsuarioActivo);

                // Soporta tanto "nickname" como "nick" según cómo esté definida tu columna en DBHelper
                String nickname = obtenerTextoSeguro(cursor, "nickname",
                        obtenerTextoSeguro(cursor, "nick", prefsCp.getString("cp_nick", "Duelista")));
                String nombreCompleto = obtenerTextoSeguro(cursor, "nombre_completo", "");
                String kid = obtenerTextoSeguro(cursor, "kid",
                        obtenerTextoSeguro(cursor, "konami_id", "Sin Konami ID"));
                String pais = obtenerTextoSeguro(cursor, "pais", "Chile");
                String regulacion = obtenerTextoSeguro(cursor, "regulacion", calcularRegulacionPorPais(pais));
                String tipoCuenta = obtenerTextoSeguro(cursor, "tipo_cuenta", "Individual");
                String descripcion = obtenerTextoSeguro(cursor, "descripcion",
                        prefsCp.getString("cp_saludo", ""));
                String fotoPerfil = obtenerTextoSeguro(cursor, "foto_perfil", "");
                String estadoJuez = obtenerTextoSeguro(cursor, "es_juez",
                        obtenerTextoSeguro(cursor, "estado_juez", "0"));

                // 1. Mostrar Nickname (y nombre completo si aplica)
                tvNombrePerfil.setText(nickname);

                // 2. Mostrar País, Regulación y Tipo de Cuenta
                tvRegulacionPerfil.setText(pais + " • " + regulacion + " | " + tipoCuenta);

                // 3. Mostrar Konami ID
                tvKonamiIdPerfil.setText(kid.isEmpty() ? "Sin Konami ID" : kid);

                // 4. Mostrar Descripción o Saludo de Duelista
                if (descripcion != null && !descripcion.trim().isEmpty()) {
                    cardDescPerfil.setVisibility(View.VISIBLE);
                    tvDescripcionPerfil.setText("“" + descripcion + "”");
                } else if (!nombreCompleto.isEmpty()) {
                    cardDescPerfil.setVisibility(View.VISIBLE);
                    tvDescripcionPerfil.setText("Duelista: " + nombreCompleto);
                } else {
                    cardDescPerfil.setVisibility(View.GONE);
                }

                // 5. Cargar Avatar (desde URI guardada o desde Base64 de CartonPass)
                cargarAvatar(fotoPerfil, prefsCp.getString("cp_avatar_base64", ""));

                // 6. Mostrar u ocultar Panel de Juez según SQLite
                if ("1".equals(estadoJuez) || "true".equalsIgnoreCase(estadoJuez)
                        || "Verificado".equalsIgnoreCase(estadoJuez)
                        || "Pendiente".equalsIgnoreCase(estadoJuez)
                        || tipoCuenta.toLowerCase().contains("juez")) {
                    if (cardPanelJuez != null) cardPanelJuez.setVisibility(View.VISIBLE);
                    llPanelJuez.setVisibility(View.VISIBLE);

                    String estadoMostrado = ("1".equals(estadoJuez) || "true".equalsIgnoreCase(estadoJuez))
                            ? "Verificado" : estadoJuez;
                    tvDatosJuezPerfil.setText("Estado Konami Judge: " + estadoMostrado + "\nKonami ID: " + kid);
                } else {
                    if (cardPanelJuez != null) cardPanelJuez.setVisibility(View.GONE);
                    llPanelJuez.setVisibility(View.GONE);
                }
            } else {
                // Si la tabla usuarios aún está vacía, usar datos de CartonPassPrefs si existen
                tvNombrePerfil.setText(prefsCp.getString("cp_nick", "Sin Usuario Activo"));
                tvRegulacionPerfil.setText("Chile • TCG");
                tvKonamiIdPerfil.setText("Sin registrar");
                cardDescPerfil.setVisibility(View.GONE);
                if (cardPanelJuez != null) cardPanelJuez.setVisibility(View.GONE);
                llPanelJuez.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        // 7. Cargar Decks reales desde la tabla "decks" de SQLite
        cargarDecksDesdeSQLite(db, idUsuarioActivo, prefsCp.getString("cp_deck", ""));

        // 8. Cargar Estadísticas / Actividad real desde SQLite
        cargarEstadisticasDesdeSQLite(db, idUsuarioActivo);

        db.close();
    }

    private void cargarDecksDesdeSQLite(SQLiteDatabase db, int idUsuario, String deckCartonPass) {
        StringBuilder sbDecks = new StringBuilder();
        Cursor cDecks = null;
        try {
            // Intentar filtrar por id_usuario si la columna existe en la tabla decks
            cDecks = db.rawQuery("SELECT * FROM decks LIMIT 4", null);
            int idxNombre = cDecks.getColumnIndex("nombre_deck");
            int idxUsuario = cDecks.getColumnIndex("id_usuario");

            int contador = 1;
            while (cDecks.moveToNext()) {
                if (idxUsuario != -1 && idUsuario != -1 && !cDecks.isNull(idxUsuario)) {
                    if (cDecks.getInt(idxUsuario) != idUsuario) continue;
                }
                if (idxNombre != -1) {
                    String nombreDeck = cDecks.getString(idxNombre);
                    if (nombreDeck != null && !nombreDeck.trim().isEmpty()) {
                        if (sbDecks.length() > 0) sbDecks.append("\n");
                        sbDecks.append(contador++).append(". ").append(nombreDeck);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cDecks != null) cDecks.close();
        }

        if (sbDecks.length() > 0) {
            tvDecksPerfil.setText(sbDecks.toString());
        } else if (deckCartonPass != null && !deckCartonPass.isEmpty() && !deckCartonPass.equals("No especificado")) {
            tvDecksPerfil.setText("1. " + deckCartonPass);
        } else {
            tvDecksPerfil.setText("Sin decks creados");
        }
    }

    private void cargarEstadisticasDesdeSQLite(SQLiteDatabase db, int idUsuario) {
        int victorias = 0;
        int derrotas = 0;
        int encuentrosCp = 0;

        // Revisar si existen columnas de victorias/derrotas en usuarios
        try (Cursor cUser = db.rawQuery(
                "SELECT * FROM usuarios WHERE id_usuario = ?",
                new String[]{String.valueOf(idUsuario)})) {
            if (cUser.moveToFirst()) {
                victorias = obtenerIntSeguro(cUser, "victorias", 0);
                derrotas = obtenerIntSeguro(cUser, "derrotas", 0);
            }
        } catch (Exception ignored) {}

        // Contar cruces registrados en cartonpass_encuentros como actividad complementaria
        try (Cursor cCp = db.rawQuery("SELECT COUNT(*) FROM cartonpass_encuentros", null)) {
            if (cCp.moveToFirst()) {
                encuentrosCp = cCp.getInt(0);
            }
        } catch (Exception ignored) {}

        if (victorias > 0 || derrotas > 0) {
            tvEstadisticasPerfil.setText("Victorias: " + victorias + "\nDerrotas: " + derrotas);
        } else {
            tvEstadisticasPerfil.setText("Victorias: 0\nDerrotas: 0\nCruces CP: " + encuentrosCp);
        }
    }

    private void cargarAvatar(String fotoUriStr, String base64Cp) {
        if (fotoUriStr != null && !fotoUriStr.isEmpty()) {
            try {
                ivAvatarPerfil.setPadding(0, 0, 0, 0);
                ivAvatarPerfil.setImageURI(Uri.parse(fotoUriStr));
                return;
            } catch (Exception ignored) {}
        }
        if (base64Cp != null && !base64Cp.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(base64Cp, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    ivAvatarPerfil.setPadding(0, 0, 0, 0);
                    ivAvatarPerfil.setImageBitmap(bmp);
                }
            } catch (Exception ignored) {}
        }
    }

    private String calcularRegulacionPorPais(String pais) {
        if (pais == null) return "TCG";
        String p = pais.toLowerCase();
        if (p.contains("japón") || p.contains("japon") || p.contains("corea") || p.contains("china")) {
            return "OCG";
        }
        return "TCG";
    }

    private String obtenerTextoSeguro(Cursor c, String columna, String porDefecto) {
        int index = c.getColumnIndex(columna);
        if (index != -1 && !c.isNull(index)) {
            String val = c.getString(index);
            if (val != null && !val.trim().isEmpty()) {
                return val;
            }
        }
        return porDefecto;
    }

    private int obtenerIntSeguro(Cursor c, String columna, int porDefecto) {
        int index = c.getColumnIndex(columna);
        if (index != -1 && !c.isNull(index)) {
            return c.getInt(index);
        }
        return porDefecto;
    }
}