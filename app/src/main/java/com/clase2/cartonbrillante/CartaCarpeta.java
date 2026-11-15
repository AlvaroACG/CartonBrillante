package com.clase2.cartonbrillante;

public class CartaCarpeta {
    private int idCarta;
    private String nombre;
    private String codigoSet;
    private String rareza;
    private String condicion;
    private int cantidad;
    private int precioReferencia;
    private String regulacion; // TCG u OCG
    private byte[] imagen; // BLOB en SQLite

    public CartaCarpeta() {
    }

    // Constructor sin ID (para insertar nuevas cartas)
    public CartaCarpeta(String nombre, String codigoSet, String rareza, String condicion, int cantidad, int precioReferencia, String regulacion, byte[] imagen) {
        this.nombre = nombre;
        this.codigoSet = codigoSet;
        this.rareza = rareza;
        this.condicion = condicion;
        this.cantidad = cantidad;
        this.precioReferencia = precioReferencia;
        this.regulacion = regulacion;
        this.imagen = imagen;
    }

    // Constructor completo (para leer desde SQLite)
    public CartaCarpeta(int idCarta, String nombre, String codigoSet, String rareza, String condicion, int cantidad, int precioReferencia, String regulacion, byte[] imagen) {
        this.idCarta = idCarta;
        this.nombre = nombre;
        this.codigoSet = codigoSet;
        this.rareza = rareza;
        this.condicion = condicion;
        this.cantidad = cantidad;
        this.precioReferencia = precioReferencia;
        this.regulacion = regulacion;
        this.imagen = imagen;
    }

    public int getIdCarta() { return idCarta; }
    public void setIdCarta(int idCarta) { this.idCarta = idCarta; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigoSet() { return codigoSet; }
    public void setCodigoSet(String codigoSet) { this.codigoSet = codigoSet; }

    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }

    public String getCondicion() { return condicion; }
    public void setCondicion(String condicion) { this.condicion = condicion; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getPrecioReferencia() { return precioReferencia; }
    public void setPrecioReferencia(int precioReferencia) { this.precioReferencia = precioReferencia; }

    public String getRegulacion() { return regulacion; }
    public void setRegulacion(String regulacion) { this.regulacion = regulacion; }

    public byte[] getImagen() { return imagen; }
    public void setImagen(byte[] imagen) { this.imagen = imagen; }
}