package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarjetas")
public class Tarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_barra", unique = true, nullable = false)
    private String codigoBarra;

    @Column(nullable = false)
    private Boolean estado;

    @Column(name = "fecha_activacion")
    private LocalDateTime fechaActivacion;

    @Column(name = "sellos_actuales", nullable = false)
    private Integer sellosActuales;

    @Column(name = "descuento", nullable = false)
    private Double descuento;

    @Column(name = "tope", nullable = false)
    private Double tope;

    // Constructores

    public Tarjeta() {
    }

    public Tarjeta(String codigoBarra, Double descuento, Double tope) {
        this.codigoBarra = codigoBarra;
        this.estado = false;
        this.sellosActuales = 0;
        this.descuento = descuento;
        this.tope = tope;
    }


    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaActivacion() {
        return fechaActivacion;
    }

    public void setFechaActivacion(LocalDateTime fechaActivacion) {
        this.fechaActivacion = fechaActivacion;
    }

    public Integer getSellosActuales() {
        return sellosActuales;
    }

    public void setSellosActuales(Integer sellosActuales) {
        this.sellosActuales = sellosActuales;
    }

    public void sumarSello(Integer numero){
        this.sellosActuales++;
    }

    public Double getDescuento() {
        return descuento;
    }

    public void setDescuento(Double descuento) {
        this.descuento = descuento;
    }
    
    public String getDescuentoString() {
        return descuento != null ? descuento.toString() : "";
    }

     public String getTopeString() {
        return tope != null ? tope.toString() : "";
    }

    public Double getTope() {
        return tope;
    }

    public void setTope(Double tope) {
        this.tope = tope;
    }
    
}