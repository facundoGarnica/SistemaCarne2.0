package model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracion_ganancia_real")
public class ConfiguracionGananciaReal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double umbralDesviacionPrecio;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Double getUmbralDesviacionPrecio() {
        return umbralDesviacionPrecio;
    }
    public void setUmbralDesviacionPrecio(Double umbralDesviacionPrecio) {
        this.umbralDesviacionPrecio = umbralDesviacionPrecio;
    }
}
