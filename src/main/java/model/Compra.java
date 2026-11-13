package model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "compra")
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate fecha;
    private String categoria;
    private Integer unidades;
    private Double pesoTotalKg;
    private Double costoTotal;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public LocalDate getFecha() {
        return fecha;
    }
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
    public String getCategoria() {
        return categoria;
    }
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    public Integer getUnidades() {
        return unidades;
    }
    public void setUnidades(Integer unidades) {
        this.unidades = unidades;
    }
    public Double getPesoTotalKg() {
        return pesoTotalKg;
    }
    public void setPesoTotalKg(Double pesoTotalKg) {
        this.pesoTotalKg = pesoTotalKg;
    }
    public Double getCostoTotal() {
        return costoTotal;
    }
    public void setCostoTotal(Double costoTotal) {
        this.costoTotal = costoTotal;
    }
}
