package com.clase2.cartonbrillante;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;

public class RegulacionHelper {

    private static final String PREFS_NAME = "CartonBrillantePrefs";
    private static final String KEY_PAIS = "pais_usuario";

    // Lista estricta de territorios OCG
    private static final List<String> PAISES_OCG = Arrays.asList(
            "Japón",
            "Corea del Sur",
            "China",
            "Hong Kong",
            "Taiwán",
            "Filipinas",
            "Malasia",
            "Singapur",
            "Tailandia",
            "Indonesia"
    );

    // Lista cerrada de países del mundo para el Spinner (evita escritura manual)
    public static final String[] LISTA_PAISES_MUNDO = {
            "Chile", "Argentina", "Perú", "Brasil", "México", "Colombia", "Estados Unidos",
            "Canadá", "España", "Reino Unido", "Francia", "Alemania", "Italia", "Australia",
            "Japón", "Corea del Sur", "China", "Hong Kong", "Taiwán", "Filipinas",
            "Malasia", "Singapur", "Tailandia", "Indonesia",
            "Uruguay", "Paraguay", "Bolivia", "Ecuador", "Venezuela", "Costa Rica", "Panamá",
            "Puerto Rico", "República Dominicana", "Guatemala", "El Salvador", "Honduras",
            "Nicaragua", "Portugal", "Países Bajos", "Bélgica", "Suiza", "Suecia", "Noruega",
            "Polonia", "Austria", "Grecia", "Nueva Zelanda", "Sudáfrica", "Otro (Resto del Mundo)"
    };

    // Determina automáticamente si un país es OCG o TCG
    public static String obtenerRegulacionPorPais(String pais) {
        if (pais == null) return "TCG";
        for (String paisOcg : PAISES_OCG) {
            if (paisOcg.equalsIgnoreCase(pais.trim())) {
                return "OCG";
            }
        }
        return "TCG";
    }

    // Guarda el país del usuario localmente al registrarse o editar perfil
    public static void guardarPaisUsuario(Context context, String pais) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PAIS, pais).apply();
    }

    // Obtiene el país actual del usuario (Por defecto "Chile" si aún no se registra)
    public static String obtenerPaisUsuario(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PAIS, "Chile");
    }

    // Devuelve directamente "TCG" u "OCG" según el usuario activo
    public static String obtenerRegulacionActual(Context context) {
        return obtenerRegulacionPorPais(obtenerPaisUsuario(context));
    }
}
