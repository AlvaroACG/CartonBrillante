package com.clase2.cartonbrillante;

public class EncuentroCartonPass {
    private int idEncuentro;
    private String nickname;
    private String saludo;
    private String deckCompartido;
    private String invocacionPreferida;
    private String pais;
    private String regulacion; // "TCG" u "OCG"
    private String fechaHora;
    private int vecesEncontrado;
    private byte[] avatarBytes;

    // Datos de Geolocalización del Encuentro
    private double latitud;
    private double longitud;
    private String nombreLugar;

    public EncuentroCartonPass() {}

    public EncuentroCartonPass(String nickname, String saludo, String deckCompartido, String invocacionPreferida,
                               String pais, String regulacion, String fechaHora, byte[] avatarBytes,
                               double latitud, double longitud, String nombreLugar) {
        this.nickname = nickname;
        this.saludo = saludo;
        this.deckCompartido = deckCompartido;
        this.invocacionPreferida = invocacionPreferida;
        this.pais = pais;
        this.regulacion = regulacion;
        this.fechaHora = fechaHora;
        this.vecesEncontrado = 1;
        this.avatarBytes = avatarBytes;
        this.latitud = latitud;
        this.longitud = longitud;
        this.nombreLugar = nombreLugar;
    }

    public EncuentroCartonPass(int idEncuentro, String nickname, String saludo, String deckCompartido,
                               String invocacionPreferida, String pais, String regulacion, String fechaHora,
                               int vecesEncontrado, byte[] avatarBytes, double latitud, double longitud, String nombreLugar) {
        this.idEncuentro = idEncuentro;
        this.nickname = nickname;
        this.saludo = saludo;
        this.deckCompartido = deckCompartido;
        this.invocacionPreferida = invocacionPreferida;
        this.pais = pais;
        this.regulacion = regulacion;
        this.fechaHora = fechaHora;
        this.vecesEncontrado = vecesEncontrado;
        this.avatarBytes = avatarBytes;
        this.latitud = latitud;
        this.longitud = longitud;
        this.nombreLugar = nombreLugar;
    }

    public boolean tieneUbicacionValida() {
        return latitud != 0.0 || longitud != 0.0;
    }

    public int getIdEncuentro() { return idEncuentro; }
    public void setIdEncuentro(int idEncuentro) { this.idEncuentro = idEncuentro; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getSaludo() { return saludo; }
    public void setSaludo(String saludo) { this.saludo = saludo; }

    public String getDeckCompartido() { return deckCompartido; }
    public void setDeckCompartido(String deckCompartido) { this.deckCompartido = deckCompartido; }

    public String getInvocacionPreferida() { return invocacionPreferida; }
    public void setInvocacionPreferida(String invocacionPreferida) { this.invocacionPreferida = invocacionPreferida; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getRegulacion() { return regulacion; }
    public void setRegulacion(String regulacion) { this.regulacion = regulacion; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public int getVecesEncontrado() { return vecesEncontrado; }
    public void setVecesEncontrado(int vecesEncontrado) { this.vecesEncontrado = vecesEncontrado; }

    public byte[] getAvatarBytes() { return avatarBytes; }
    public void setAvatarBytes(byte[] avatarBytes) { this.avatarBytes = avatarBytes; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public String getNombreLugar() { return nombreLugar; }
    public void setNombreLugar(String nombreLugar) { this.nombreLugar = nombreLugar; }
}