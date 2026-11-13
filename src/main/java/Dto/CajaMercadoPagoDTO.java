package Dto;

/**
 * Caja (POS) registrada en la cuenta de Mercado Pago, con su QR asociado.
 * Se arma a partir de la respuesta de GET /pos (ver MercadoPagoPosService).
 */
public class CajaMercadoPagoDTO {

    private String id;
    private String nombre;
    private String externalId;
    private String storeId;
    private String externalStoreId;
    private String categoria;
    private String modoOperacion;
    private String estado;
    private String uuid;
    private String fechaCreacion;
    private String fechaActualizacion;
    private String qrImagen;
    private String qrPdf;
    private String qrPlantillaImagen;

    // Caja del sistema (Caja 1/2/3) que usa este QR; se calcula al mostrarla
    private String cajaAsignada = "—";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getExternalStoreId() { return externalStoreId; }
    public void setExternalStoreId(String externalStoreId) { this.externalStoreId = externalStoreId; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getModoOperacion() { return modoOperacion; }
    public void setModoOperacion(String modoOperacion) { this.modoOperacion = modoOperacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getQrImagen() { return qrImagen; }
    public void setQrImagen(String qrImagen) { this.qrImagen = qrImagen; }

    public String getQrPdf() { return qrPdf; }
    public void setQrPdf(String qrPdf) { this.qrPdf = qrPdf; }

    public String getQrPlantillaImagen() { return qrPlantillaImagen; }
    public void setQrPlantillaImagen(String qrPlantillaImagen) { this.qrPlantillaImagen = qrPlantillaImagen; }

    public String getCajaAsignada() { return cajaAsignada; }
    public void setCajaAsignada(String cajaAsignada) { this.cajaAsignada = cajaAsignada; }
}
