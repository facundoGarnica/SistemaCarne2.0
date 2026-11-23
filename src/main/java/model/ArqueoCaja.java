package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Conteo del efectivo de un turno (Gastos y reportes > Reportes y balance):
 * lo que decía el sistema al contar y los billetes que había en la caja.
 */
@Entity
@Table(name = "arqueo_caja", uniqueConstraints = @UniqueConstraint(columnNames = {"fecha", "turno"}))
public class ArqueoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    /** "Mañana" o "Tarde" (HorarioLocal). */
    @Column(nullable = false, length = 20)
    private String turno;

    /** Efectivo que mostraba el sistema para ese turno al contar. */
    @Column(name = "monto_sistema")
    private Double montoSistema;

    /** Billetes que había en la caja. */
    @Column(name = "monto_contado")
    private Double montoContado;

    private LocalDateTime registrado;

    /** Contado − sistema: positivo si sobra, negativo si falta. */
    public double diferencia() {
        return (montoContado == null ? 0 : montoContado) - (montoSistema == null ? 0 : montoSistema);
    }

    public boolean coincide() {
        return Math.abs(diferencia()) < 0.5;
    }

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

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public Double getMontoSistema() {
        return montoSistema;
    }

    public void setMontoSistema(Double montoSistema) {
        this.montoSistema = montoSistema;
    }

    public Double getMontoContado() {
        return montoContado;
    }

    public void setMontoContado(Double montoContado) {
        this.montoContado = montoContado;
    }

    public LocalDateTime getRegistrado() {
        return registrado;
    }

    public void setRegistrado(LocalDateTime registrado) {
        this.registrado = registrado;
    }
}
