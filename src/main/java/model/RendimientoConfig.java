package model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rendimiento_config")
public class RendimientoConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String categoria;
    private Double porcentajeAprovechable;
    private Double umbralAlertaPorcentaje;
    private Double costoReferenciaKg;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getCategoria() {
        return categoria;
    }
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    public Double getPorcentajeAprovechable() {
        return porcentajeAprovechable;
    }
    public void setPorcentajeAprovechable(Double porcentajeAprovechable) {
        this.porcentajeAprovechable = porcentajeAprovechable;
    }
    public Double getUmbralAlertaPorcentaje() {
        return umbralAlertaPorcentaje;
    }
    public void setUmbralAlertaPorcentaje(Double umbralAlertaPorcentaje) {
        this.umbralAlertaPorcentaje = umbralAlertaPorcentaje;
    }
    public Double getCostoReferenciaKg() {
        return costoReferenciaKg;
    }
    public void setCostoReferenciaKg(Double costoReferenciaKg) {
        this.costoReferenciaKg = costoReferenciaKg;
    }
}
