package Util;

public class DatosPagos {

    private long id;

    private String Cliente;

    private String Fecha;

    private Double Monto;

    private String Estado;

    public DatosPagos(
            long id,
            String Cliente,
            String Fecha,
            Double Monto,
            String Estado
    ) {
        this.id = id;
        this.Cliente = Cliente;
        this.Fecha = Fecha;
        this.Monto = Monto;
        this.Estado = Estado;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCliente() {
        return Cliente;
    }

    public void setCliente(String Cliente) {
        this.Cliente = Cliente;
    }

    public String getFecha() {
        return Fecha;
    }

    public void setFecha(String Fecha) {
        this.Fecha = Fecha;
    }

    public Double getMonto() {
        return Monto;
    }

    public void setMonto(Double Monto) {
        this.Monto = Monto;
    }

    public String getEstado() {
        return Estado;
    }

    public void setEstado(String Estado) {
        this.Estado = Estado;
    }

}