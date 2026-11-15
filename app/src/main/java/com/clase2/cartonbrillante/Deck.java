package com.clase2.cartonbrillante;

import java.util.ArrayList;
import java.util.List;

public class Deck {
    private int idDeck;
    private String nombreDeck;
    private String tipoProtectores;
    private int cantidadProtectores;
    private String regulacion; // TCG / OCG
    private boolean esPublico; // true = Público (se sincronizará a Firestore), false = Privado

    // Listas de imágenes comprimidas (BLOBs)
    private List<byte[]> fotosMain = new ArrayList<>();
    private List<byte[]> fotosExtra = new ArrayList<>();
    private List<byte[]> fotosSide = new ArrayList<>();

    // Contadores rápidos para mostrar en la cuadrícula principal
    private int conteoMain;
    private int conteoExtra;
    private int conteoSide;

    public Deck() {}

    // Constructor para insertar un nuevo Deck
    public Deck(String nombreDeck, String tipoProtectores, int cantidadProtectores, String regulacion, boolean esPublico,
                List<byte[]> fotosMain, List<byte[]> fotosExtra, List<byte[]> fotosSide) {
        this.nombreDeck = nombreDeck;
        this.tipoProtectores = tipoProtectores;
        this.cantidadProtectores = cantidadProtectores;
        this.regulacion = regulacion;
        this.esPublico = esPublico;
        this.fotosMain = fotosMain;
        this.fotosExtra = fotosExtra;
        this.fotosSide = fotosSide;
        this.conteoMain = (fotosMain != null) ? fotosMain.size() : 0;
        this.conteoExtra = (fotosExtra != null) ? fotosExtra.size() : 0;
        this.conteoSide = (fotosSide != null) ? fotosSide.size() : 0;
    }

    // Constructor para listar desde SQLite en la cuadrícula
    public Deck(int idDeck, String nombreDeck, String tipoProtectores, int cantidadProtectores,
                String regulacion, boolean esPublico, int conteoMain, int conteoExtra, int conteoSide) {
        this.idDeck = idDeck;
        this.nombreDeck = nombreDeck;
        this.tipoProtectores = tipoProtectores;
        this.cantidadProtectores = cantidadProtectores;
        this.regulacion = regulacion;
        this.esPublico = esPublico;
        this.conteoMain = conteoMain;
        this.conteoExtra = conteoExtra;
        this.conteoSide = conteoSide;
    }

    public int getIdDeck() { return idDeck; }
    public void setIdDeck(int idDeck) { this.idDeck = idDeck; }

    public String getNombreDeck() { return nombreDeck; }
    public void setNombreDeck(String nombreDeck) { this.nombreDeck = nombreDeck; }

    public String getTipoProtectores() { return tipoProtectores; }
    public void setTipoProtectores(String tipoProtectores) { this.tipoProtectores = tipoProtectores; }

    public int getCantidadProtectores() { return cantidadProtectores; }
    public void setCantidadProtectores(int cantidadProtectores) { this.cantidadProtectores = cantidadProtectores; }

    public String getRegulacion() { return regulacion; }
    public void setRegulacion(String regulacion) { this.regulacion = regulacion; }

    public boolean isEsPublico() { return esPublico; }
    public void setEsPublico(boolean esPublico) { this.esPublico = esPublico; }

    public List<byte[]> getFotosMain() { return fotosMain; }
    public void setFotosMain(List<byte[]> fotosMain) {
        this.fotosMain = fotosMain;
        this.conteoMain = (fotosMain != null) ? fotosMain.size() : 0;
    }

    public List<byte[]> getFotosExtra() { return fotosExtra; }
    public void setFotosExtra(List<byte[]> fotosExtra) {
        this.fotosExtra = fotosExtra;
        this.conteoExtra = (fotosExtra != null) ? fotosExtra.size() : 0;
    }

    public List<byte[]> getFotosSide() { return fotosSide; }
    public void setFotosSide(List<byte[]> fotosSide) {
        this.fotosSide = fotosSide;
        this.conteoSide = (fotosSide != null) ? fotosSide.size() : 0;
    }

    public int getConteoMain() { return conteoMain; }
    public int getConteoExtra() { return conteoExtra; }
    public int getConteoSide() { return conteoSide; }
}