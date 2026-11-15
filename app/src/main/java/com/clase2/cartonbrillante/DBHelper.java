package com.clase2.cartonbrillante;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CartonBrillante.db";
    private static final int DATABASE_VERSION = 11; // Versión 11: Soporte completo para usuarios, roles, decks, carpeta y mapa

    // --- TABLA USUARIOS ---
    public static final String TABLE_USUARIOS = "usuarios";
    public static final String COL_USER_ID = "id_usuario";
    public static final String COL_USER_NICK = "nickname";
    public static final String COL_USER_NOMBRE = "nombre_completo";
    public static final String COL_USER_KID = "kid";
    public static final String COL_USER_PAIS = "pais";
    public static final String COL_USER_CORREO = "correo";
    public static final String COL_USER_PASS = "password_hash";
    public static final String COL_USER_NACIMIENTO = "fecha_nacimiento";
    public static final String COL_USER_TIPO = "tipo_cuenta";

    // --- TABLA CARPETA ---
    public static final String TABLE_CARPETA = "carpeta_cartas";
    public static final String COL_ID = "id_carta";
    public static final String COL_NOMBRE = "nombre";
    public static final String COL_CODIGO = "codigo_set";
    public static final String COL_RAREZA = "rareza";
    public static final String COL_CONDICION = "condicion";
    public static final String COL_CANTIDAD = "cantidad";
    public static final String COL_PRECIO = "precio_referencia";
    public static final String COL_CARTA_REGULACION = "regulacion_carta";
    public static final String COL_IMAGEN = "imagen";

    // --- TABLA DECKS ---
    public static final String TABLE_DECKS = "decks";
    public static final String COL_DECK_ID = "id_deck";
    public static final String COL_DECK_NOMBRE = "nombre_deck";
    public static final String COL_TIPO_PROT = "tipo_protectores";
    public static final String COL_CANT_PROT = "cantidad_protectores";
    public static final String COL_REGULACION = "regulacion";
    public static final String COL_ES_PUBLICO = "es_publico";
    public static final String COL_COUNT_MAIN = "conteo_main";
    public static final String COL_COUNT_EXTRA = "conteo_extra";
    public static final String COL_COUNT_SIDE = "conteo_side";

    // --- TABLA FOTOS DE CADA DECK ---
    public static final String TABLE_DECK_FOTOS = "deck_cartas_fotos";
    public static final String COL_FOTO_ID = "id_foto";
    public static final String COL_FOTO_DECK_ID = "id_deck_fk";
    public static final String COL_FOTO_SECCION = "seccion";
    public static final String COL_FOTO_BLOB = "imagen_carta";

    // --- TABLA CARTONPASS ---
    public static final String TABLE_CARTONPASS = "cartonpass_encuentros";
    public static final String COL_CP_ID = "id_encuentro";
    public static final String COL_CP_NICK = "nickname";
    public static final String COL_CP_SALUDO = "saludo";
    public static final String COL_CP_DECK = "deck_compartido";
    public static final String COL_CP_INVOCACION = "invocacion_preferida";
    public static final String COL_CP_PAIS = "pais_encuentro";
    public static final String COL_CP_REGULACION = "regulacion_encuentro";
    public static final String COL_CP_FECHA = "fecha_hora";
    public static final String COL_CP_VECES = "veces_encontrado";
    public static final String COL_CP_AVATAR = "avatar_blob";
    public static final String COL_CP_LAT = "latitud";
    public static final String COL_CP_LON = "longitud";
    public static final String COL_CP_LUGAR = "nombre_lugar";

    // --- TABLA PUBLICACIONES DEL MAPA (Duelo, Venta, Búsqueda) ---
    public static final String TABLE_MAPA_PUBS = "mapa_publicaciones";
    public static final String COL_MP_ID = "id_pub_mapa";
    public static final String COL_MP_AUTOR = "autor_nick";
    public static final String COL_MP_TIPO = "tipo_actividad"; // "DUELO", "VENTA", "BUSQUEDA"
    public static final String COL_MP_TITULO = "titulo_carta_o_formato";
    public static final String COL_MP_DETALLE = "detalle_precio_o_nota";
    public static final String COL_MP_PAIS_REG = "pais_regulacion";
    public static final String COL_MP_LAT = "latitud";
    public static final String COL_MP_LON = "longitud";
    public static final String COL_MP_FECHA = "fecha_hora";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableUsuarios = "CREATE TABLE IF NOT EXISTS " + TABLE_USUARIOS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_NICK + " TEXT, " +
                COL_USER_NOMBRE + " TEXT NOT NULL, " +
                COL_USER_KID + " TEXT, " +
                COL_USER_PAIS + " TEXT NOT NULL, " +
                COL_USER_CORREO + " TEXT UNIQUE, " +
                COL_USER_PASS + " TEXT, " +
                COL_USER_NACIMIENTO + " TEXT, " +
                COL_USER_TIPO + " TEXT)";
        db.execSQL(createTableUsuarios);

        String createTableCarpeta = "CREATE TABLE IF NOT EXISTS " + TABLE_CARPETA + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOMBRE + " TEXT NOT NULL, " +
                COL_CODIGO + " TEXT, " +
                COL_RAREZA + " TEXT, " +
                COL_CONDICION + " TEXT, " +
                COL_CANTIDAD + " INTEGER DEFAULT 1, " +
                COL_PRECIO + " INTEGER DEFAULT 0, " +
                COL_CARTA_REGULACION + " TEXT DEFAULT 'TCG', " +
                COL_IMAGEN + " BLOB)";
        db.execSQL(createTableCarpeta);

        String createTableDecks = "CREATE TABLE IF NOT EXISTS " + TABLE_DECKS + " (" +
                COL_DECK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DECK_NOMBRE + " TEXT NOT NULL, " +
                COL_TIPO_PROT + " TEXT, " +
                COL_CANT_PROT + " INTEGER DEFAULT 55, " +
                COL_REGULACION + " TEXT DEFAULT 'TCG', " +
                COL_ES_PUBLICO + " INTEGER DEFAULT 1, " +
                COL_COUNT_MAIN + " INTEGER DEFAULT 0, " +
                COL_COUNT_EXTRA + " INTEGER DEFAULT 0, " +
                COL_COUNT_SIDE + " INTEGER DEFAULT 0)";
        db.execSQL(createTableDecks);

        String createTableDeckFotos = "CREATE TABLE IF NOT EXISTS " + TABLE_DECK_FOTOS + " (" +
                COL_FOTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FOTO_DECK_ID + " INTEGER NOT NULL, " +
                COL_FOTO_SECCION + " TEXT NOT NULL, " +
                COL_FOTO_BLOB + " BLOB)";
        db.execSQL(createTableDeckFotos);

        String createTableCartonPass = "CREATE TABLE IF NOT EXISTS " + TABLE_CARTONPASS + " (" +
                COL_CP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CP_NICK + " TEXT NOT NULL, " +
                COL_CP_SALUDO + " TEXT NOT NULL, " +
                COL_CP_DECK + " TEXT, " +
                COL_CP_INVOCACION + " TEXT, " +
                COL_CP_PAIS + " TEXT DEFAULT 'Chile', " +
                COL_CP_REGULACION + " TEXT DEFAULT 'TCG', " +
                COL_CP_FECHA + " TEXT, " +
                COL_CP_VECES + " INTEGER DEFAULT 1, " +
                COL_CP_AVATAR + " BLOB, " +
                COL_CP_LAT + " REAL DEFAULT 0.0, " +
                COL_CP_LON + " REAL DEFAULT 0.0, " +
                COL_CP_LUGAR + " TEXT)";
        db.execSQL(createTableCartonPass);

        String createTableMapaPubs = "CREATE TABLE IF NOT EXISTS " + TABLE_MAPA_PUBS + " (" +
                COL_MP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MP_AUTOR + " TEXT NOT NULL, " +
                COL_MP_TIPO + " TEXT NOT NULL, " +
                COL_MP_TITULO + " TEXT NOT NULL, " +
                COL_MP_DETALLE + " TEXT, " +
                COL_MP_PAIS_REG + " TEXT, " +
                COL_MP_LAT + " REAL NOT NULL, " +
                COL_MP_LON + " REAL NOT NULL, " +
                COL_MP_FECHA + " TEXT)";
        db.execSQL(createTableMapaPubs);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIOS);
        }
        if (oldVersion < 6) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DECKS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DECK_FOTOS);
        }
        if (oldVersion < 9) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARTONPASS);
        }
        onCreate(db);
    }

    // ==================== CRUD USUARIO / REGISTRO ====================
    public long guardarUsuarioCompleto(String nick, String nombre, String kid, String pais, String correo, String password, String nacimiento, String tipoCuenta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_NICK, nick);
        values.put(COL_USER_NOMBRE, nombre);
        values.put(COL_USER_KID, kid);
        values.put(COL_USER_PAIS, pais);
        values.put(COL_USER_CORREO, correo);
        values.put(COL_USER_PASS, password);
        values.put(COL_USER_NACIMIENTO, nacimiento);
        values.put(COL_USER_TIPO, tipoCuenta != null ? tipoCuenta.toUpperCase() : "JUGADOR");

        long id = db.insertWithOnConflict(TABLE_USUARIOS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id;
    }

    public String obtenerRolUsuarioPorId(int idUsuario) {
        SQLiteDatabase db = this.getReadableDatabase();
        String rol = "JUGADOR";
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_TIPO + " FROM " + TABLE_USUARIOS + " WHERE " + COL_USER_ID + " = ?", new String[]{String.valueOf(idUsuario)});
        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(COL_USER_TIPO);
            if (index != -1) {
                String valor = cursor.getString(index);
                if (valor != null && !valor.isEmpty()) {
                    rol = valor;
                }
            }
        }
        cursor.close();
        db.close();
        return rol;
    }

    public boolean actualizarRolUsuario(int idUsuario, String nuevoRol) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_TIPO, nuevoRol != null ? nuevoRol.toUpperCase() : "JUGADOR");

        int filasAfectadas = db.update(TABLE_USUARIOS, values, COL_USER_ID + " = ?", new String[]{String.valueOf(idUsuario)});
        db.close();
        return filasAfectadas > 0;
    }

    public boolean existeUsuarioPorCorreo(String correo) {
        if (correo == null || correo.isEmpty()) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USUARIOS + " WHERE " + COL_USER_CORREO + " = ?", new String[]{correo});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        db.close();
        return existe;
    }

    public String obtenerPaisPorCorreo(String correo) {
        if (correo == null || correo.isEmpty()) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_PAIS + " FROM " + TABLE_USUARIOS + " WHERE " + COL_USER_CORREO + " = ?", new String[]{correo});
        String pais = null;
        if (cursor.moveToFirst()) {
            pais = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return pais;
    }

    public String obtenerNicknameLocal() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_NICK + " FROM " + TABLE_USUARIOS + " ORDER BY " + COL_USER_ID + " DESC LIMIT 1", null);
        String nick = "";
        if (cursor.moveToFirst()) {
            nick = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return (nick != null && !nick.isEmpty()) ? nick : "DuelistaLocal";
    }

    // ==================== CRUD CARPETA ====================
    public long insertarCarta(CartaCarpeta carta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOMBRE, carta.getNombre());
        values.put(COL_CODIGO, carta.getCodigoSet());
        values.put(COL_RAREZA, carta.getRareza());
        values.put(COL_CONDICION, carta.getCondicion());
        values.put(COL_CANTIDAD, carta.getCantidad());
        values.put(COL_PRECIO, carta.getPrecioReferencia());
        values.put(COL_CARTA_REGULACION, carta.getRegulacion());
        values.put(COL_IMAGEN, carta.getImagen());
        long id = db.insert(TABLE_CARPETA, null, values);
        db.close();
        return id;
    }

    public List<CartaCarpeta> obtenerTodasLasCartas() {
        List<CartaCarpeta> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CARPETA + " ORDER BY " + COL_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                String regulacion = cursor.getString(cursor.getColumnIndexOrThrow(COL_CARTA_REGULACION));
                if (regulacion == null || regulacion.isEmpty()) regulacion = "TCG";

                lista.add(new CartaCarpeta(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CODIGO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_RAREZA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CONDICION)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CANTIDAD)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRECIO)),
                        regulacion,
                        cursor.getBlob(cursor.getColumnIndexOrThrow(COL_IMAGEN))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    public int actualizarCarta(CartaCarpeta carta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOMBRE, carta.getNombre());
        values.put(COL_CODIGO, carta.getCodigoSet());
        values.put(COL_RAREZA, carta.getRareza());
        values.put(COL_CONDICION, carta.getCondicion());
        values.put(COL_CANTIDAD, carta.getCantidad());
        values.put(COL_PRECIO, carta.getPrecioReferencia());
        values.put(COL_CARTA_REGULACION, carta.getRegulacion());
        values.put(COL_IMAGEN, carta.getImagen());
        int filas = db.update(TABLE_CARPETA, values, COL_ID + " = ?", new String[]{String.valueOf(carta.getIdCarta())});
        db.close();
        return filas;
    }

    public int eliminarCarta(int idCarta) {
        SQLiteDatabase db = this.getWritableDatabase();
        int filas = db.delete(TABLE_CARPETA, COL_ID + " = ?", new String[]{String.valueOf(idCarta)});
        db.close();
        return filas;
    }

    // ==================== CRUD DECKS CON FOTOS ====================
    public long insertarDeck(Deck deck) {
        SQLiteDatabase db = this.getWritableDatabase();
        long idDeck = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COL_DECK_NOMBRE, deck.getNombreDeck());
            values.put(COL_TIPO_PROT, deck.getTipoProtectores());
            values.put(COL_CANT_PROT, deck.getCantidadProtectores());
            values.put(COL_REGULACION, deck.getRegulacion());
            values.put(COL_ES_PUBLICO, deck.isEsPublico() ? 1 : 0);
            values.put(COL_COUNT_MAIN, deck.getFotosMain().size());
            values.put(COL_COUNT_EXTRA, deck.getFotosExtra().size());
            values.put(COL_COUNT_SIDE, deck.getFotosSide().size());

            idDeck = db.insert(TABLE_DECKS, null, values);

            if (idDeck != -1) {
                guardarListaFotosSeccion(db, (int) idDeck, "MAIN", deck.getFotosMain());
                guardarListaFotosSeccion(db, (int) idDeck, "EXTRA", deck.getFotosExtra());
                guardarListaFotosSeccion(db, (int) idDeck, "SIDE", deck.getFotosSide());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return idDeck;
    }

    private void guardarListaFotosSeccion(SQLiteDatabase db, int idDeck, String seccion, List<byte[]> listaFotos) {
        if (listaFotos == null) return;
        for (byte[] foto : listaFotos) {
            ContentValues cv = new ContentValues();
            cv.put(COL_FOTO_DECK_ID, idDeck);
            cv.put(COL_FOTO_SECCION, seccion);
            cv.put(COL_FOTO_BLOB, foto);
            db.insert(TABLE_DECK_FOTOS, null, cv);
        }
    }

    public List<Deck> obtenerTodosLosDecks() {
        List<Deck> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DECKS + " ORDER BY " + COL_DECK_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                lista.add(new Deck(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_DECK_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DECK_NOMBRE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TIPO_PROT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CANT_PROT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_REGULACION)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ES_PUBLICO)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_COUNT_MAIN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_COUNT_EXTRA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_COUNT_SIDE))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    public void cargarFotosDeUnDeck(Deck deck) {
        SQLiteDatabase db = this.getReadableDatabase();
        deck.setFotosMain(obtenerFotosPorSeccion(db, deck.getIdDeck(), "MAIN"));
        deck.setFotosExtra(obtenerFotosPorSeccion(db, deck.getIdDeck(), "EXTRA"));
        deck.setFotosSide(obtenerFotosPorSeccion(db, deck.getIdDeck(), "SIDE"));
        db.close();
    }

    private List<byte[]> obtenerFotosPorSeccion(SQLiteDatabase db, int idDeck, String seccion) {
        List<byte[]> fotos = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_FOTO_BLOB + " FROM " + TABLE_DECK_FOTOS +
                        " WHERE " + COL_FOTO_DECK_ID + " = ? AND " + COL_FOTO_SECCION + " = ? ORDER BY " + COL_FOTO_ID + " ASC",
                new String[]{String.valueOf(idDeck), seccion}
        );
        if (cursor.moveToFirst()) {
            do {
                fotos.add(cursor.getBlob(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return fotos;
    }

    public int actualizarDeck(Deck deck) {
        SQLiteDatabase db = this.getWritableDatabase();
        int filas = 0;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COL_DECK_NOMBRE, deck.getNombreDeck());
            values.put(COL_TIPO_PROT, deck.getTipoProtectores());
            values.put(COL_CANT_PROT, deck.getCantidadProtectores());
            values.put(COL_REGULACION, deck.getRegulacion());
            values.put(COL_ES_PUBLICO, deck.isEsPublico() ? 1 : 0);
            values.put(COL_COUNT_MAIN, deck.getFotosMain().size());
            values.put(COL_COUNT_EXTRA, deck.getFotosExtra().size());
            values.put(COL_COUNT_SIDE, deck.getFotosSide().size());

            filas = db.update(TABLE_DECKS, values, COL_DECK_ID + " = ?", new String[]{String.valueOf(deck.getIdDeck())});

            db.delete(TABLE_DECK_FOTOS, COL_FOTO_DECK_ID + " = ?", new String[]{String.valueOf(deck.getIdDeck())});
            guardarListaFotosSeccion(db, deck.getIdDeck(), "MAIN", deck.getFotosMain());
            guardarListaFotosSeccion(db, deck.getIdDeck(), "EXTRA", deck.getFotosExtra());
            guardarListaFotosSeccion(db, deck.getIdDeck(), "SIDE", deck.getFotosSide());

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return filas;
    }

    public int eliminarDeck(int idDeck) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DECK_FOTOS, COL_FOTO_DECK_ID + " = ?", new String[]{String.valueOf(idDeck)});
        int filas = db.delete(TABLE_DECKS, COL_DECK_ID + " = ?", new String[]{String.valueOf(idDeck)});
        db.close();
        return filas;
    }

    // ==================== CRUD CARTONPASS ====================
    public void registrarEncuentroCartonPass(EncuentroCartonPass nuevo) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + COL_CP_ID + ", " + COL_CP_VECES +
                        " FROM " + TABLE_CARTONPASS + " WHERE UPPER(" + COL_CP_NICK + ") = UPPER(?)",
                new String[]{nuevo.getNickname()});

        ContentValues values = new ContentValues();
        values.put(COL_CP_NICK, nuevo.getNickname());
        values.put(COL_CP_SALUDO, nuevo.getSaludo());
        values.put(COL_CP_DECK, nuevo.getDeckCompartido());
        values.put(COL_CP_INVOCACION, nuevo.getInvocacionPreferida());
        values.put(COL_CP_PAIS, nuevo.getPais());
        values.put(COL_CP_REGULACION, nuevo.getRegulacion());
        values.put(COL_CP_FECHA, nuevo.getFechaHora());
        if (nuevo.getAvatarBytes() != null) {
            values.put(COL_CP_AVATAR, nuevo.getAvatarBytes());
        }
        if (nuevo.tieneUbicacionValida()) {
            values.put(COL_CP_LAT, nuevo.getLatitud());
            values.put(COL_CP_LON, nuevo.getLongitud());
            values.put(COL_CP_LUGAR, nuevo.getNombreLugar());
        }

        if (cursor.moveToFirst()) {
            int idExistente = cursor.getInt(0);
            int vecesActuales = cursor.getInt(1);
            values.put(COL_CP_VECES, vecesActuales + 1);
            db.update(TABLE_CARTONPASS, values, COL_CP_ID + " = ?", new String[]{String.valueOf(idExistente)});
        } else {
            values.put(COL_CP_VECES, 1);
            db.insert(TABLE_CARTONPASS, null, values);
        }

        cursor.close();
        db.close();
    }

    public List<EncuentroCartonPass> obtenerTodosLosEncuentrosCartonPass() {
        List<EncuentroCartonPass> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CARTONPASS + " ORDER BY " + COL_CP_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                lista.add(new EncuentroCartonPass(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CP_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_NICK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_SALUDO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_DECK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_INVOCACION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_PAIS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_REGULACION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_FECHA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CP_VECES)),
                        cursor.getBlob(cursor.getColumnIndexOrThrow(COL_CP_AVATAR)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CP_LAT)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CP_LON)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_LUGAR))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    public List<EncuentroCartonPass> obtenerEncuentrosConUbicacion() {
        List<EncuentroCartonPass> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CARTONPASS +
                " WHERE " + COL_CP_LAT + " != 0.0 OR " + COL_CP_LON + " != 0.0 ORDER BY " + COL_CP_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                lista.add(new EncuentroCartonPass(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CP_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_NICK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_SALUDO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_DECK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_INVOCACION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_PAIS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_REGULACION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_FECHA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CP_VECES)),
                        cursor.getBlob(cursor.getColumnIndexOrThrow(COL_CP_AVATAR)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CP_LAT)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CP_LON)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CP_LUGAR))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    public int eliminarEncuentroCartonPass(int idEncuentro) {
        SQLiteDatabase db = this.getWritableDatabase();
        int filas = db.delete(TABLE_CARTONPASS, COL_CP_ID + " = ?", new String[]{String.valueOf(idEncuentro)});
        db.close();
        return filas;
    }

    // ==================== CRUD PUBLICACIONES DEL MAPA (DUELO, VENTA, BÚSQUEDA) ====================
    public long insertarPublicacionMapa(String autor, String tipo, String titulo, String detalle, String paisReg, double lat, double lon, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MP_AUTOR, autor);
        cv.put(COL_MP_TIPO, tipo);
        cv.put(COL_MP_TITULO, titulo);
        cv.put(COL_MP_DETALLE, detalle);
        cv.put(COL_MP_PAIS_REG, paisReg);
        cv.put(COL_MP_LAT, lat);
        cv.put(COL_MP_LON, lon);
        cv.put(COL_MP_FECHA, fecha);
        long id = db.insert(TABLE_MAPA_PUBS, null, cv);
        db.close();
        return id;
    }

    public List<String[]> obtenerPublicacionesMapa() {
        List<String[]> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_MAPA_PUBS + " ORDER BY " + COL_MP_ID + " DESC", null);
        if (c.moveToFirst()) {
            do {
                lista.add(new String[]{
                        String.valueOf(c.getInt(c.getColumnIndexOrThrow(COL_MP_ID))),
                        c.getString(c.getColumnIndexOrThrow(COL_MP_AUTOR)),
                        c.getString(c.getColumnIndexOrThrow(COL_MP_TIPO)),
                        c.getString(c.getColumnIndexOrThrow(COL_MP_TITULO)),
                        c.getString(c.getColumnIndexOrThrow(COL_MP_DETALLE)),
                        c.getString(c.getColumnIndexOrThrow(COL_MP_PAIS_REG)),
                        String.valueOf(c.getDouble(c.getColumnIndexOrThrow(COL_MP_LAT))),
                        String.valueOf(c.getDouble(c.getColumnIndexOrThrow(COL_MP_LON))),
                        c.getString(c.getColumnIndexOrThrow(COL_MP_FECHA))
                });
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return lista;
    }

    public void eliminarPublicacionMapa(int idPub) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MAPA_PUBS, COL_MP_ID + " = ?", new String[]{String.valueOf(idPub)});
        db.close();
    }
}