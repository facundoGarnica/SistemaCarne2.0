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

    public Venta() {
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