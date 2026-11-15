package com.clase2.cartonbrillante;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CloudSyncService {

    private static FirebaseFirestore getDb() {
        return FirebaseFirestore.getInstance();
    }

    // 1. Guardar o actualizar usuario en la nube
    public static void guardarUsuarioCloud(String uid, String nick, String nombre, String correo, String pais, String rol) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid", uid);
        usuario.put("nickname", nick);
        usuario.put("nombre_completo", nombre);
        usuario.put("correo", correo);
        usuario.put("pais", pais);
        usuario.put("tipo_cuenta", rol);
        usuario.put("actualizado_en", System.currentTimeMillis());

        getDb().collection("usuarios").document(correo)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {})
                .addOnFailureListener(e -> {});
    }

    // 2. Publicar en el Foro (Público)
    public static void publicarEnForoCloud(String autor, String tipo, String titulo, String detalle, String precio, String formato, float condicion, boolean negociable, String imagenUri) {
        Map<String, Object> pub = new HashMap<>();
        pub.put("autor_nick", autor);
        pub.put("tipo_actividad", tipo);
        pub.put("titulo", titulo);
        pub.put("detalle", detalle);
        pub.put("precio", precio);
        pub.put("formato", formato);
        pub.put("condicion", condicion);
        pub.put("negociable", negociable);
        pub.put("imagen_uri", imagenUri);
        pub.put("timestamp", System.currentTimeMillis());

        getDb().collection("publicaciones").add(pub);
    }

    // 3. Publicar Marca en el Mapa Global (Público)
    public static void publicarMarcaMapaCloud(String autor, String tipo, String titulo, String detalle, String paisReg, double lat, double lon, String fecha) {
        Map<String, Object> marca = new HashMap<>();
        marca.put("autor_nick", autor);
        marca.put("tipo_actividad", tipo); // DUELO, VENTA, BUSQUEDA
        marca.put("titulo", titulo);
        marca.put("detalle", detalle);
        marca.put("pais_regulacion", paisReg);
        marca.put("latitud", lat);
        marca.put("longitud", lon);
        marca.put("fecha_hora", fecha);
        marca.put("timestamp", System.currentTimeMillis());

        getDb().collection("mapa_publicaciones").add(marca);
    }
}
