package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double total;

    private LocalDateTime fecha;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<DetalleVenta> detalleVentas;

    private String medioPago;

    @ManyToOne
    @JoinColumn(name = "tarjeta_id")
    private Tarjeta tarjeta;

    @Column(name = "sello_antes")
    private Integer selloAntes;

    @Column(name = "sello_despues")
    private Integer selloDespues;

    /**
     * Solo en pagos mixtos (medioPago = "Mixto"): la parte pagada en efectivo.
     * El resto (total − montoEfectivo) se cobró por QR / virtual.
     */
    @Column(name = "monto_efectivo")
    private Double montoEfectivo;

    public static final String MIXTO = "Mixto";

    public Venta() {
    }

    /** Parte de la venta pagada en efectivo (todo, nada o la parte del pago mixto). */
    public double montoEnEfectivo() {
        double t = total != null ? total : 0.0;
        if (MIXTO.equalsIgnoreCase(medioPago)) {
            return montoEfectivo != null ? Math.min(montoEfectivo, t) : 0.0;
        }
        return "Efectivo".equalsIgnoreCase(medioPago) ? t : 0.0;
    }

    /** Parte de la venta pagada en virtual / QR. */
    public double montoEnVirtual() {
        double t = total != null ? total : 0.0;
        return t - montoEnEfectivo();
    }

    public boolean esMixto() {
        return MIXTO.equalsIgnoreCase(medioPago);
    }

    public Double getMontoEfectivo() {
        return montoEfectivo;
    }

    public void setMontoEfectivo(Double montoEfectivo) {
        this.montoEfectivo = montoEfectivo;
    }

    public Long getId() {
        return id;
    }

    public Double getTotal() {
        return total;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public List<DetalleVenta> getDetalleVentas() {
        return detalleVentas;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public Tarjeta getTarjeta() {
        return tarjeta;
    }

    public Integer getSelloAntes() {
        return selloAntes;
    }

    public Integer getSelloDespues() {
        return selloDespues;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public void setDetalleVentas(List<DetalleVenta> detalleVentas) {
        this.detalleVentas = detalleVentas;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public void setTarjeta(Tarjeta tarjeta) {
        this.tarjeta = tarjeta;
    }

    public void setSelloAntes(Integer selloAntes) {
        this.selloAntes = selloAntes;
    }

    public void setSelloDespues(Integer selloDespues) {
        this.selloDespues = selloDespues;
    }
}