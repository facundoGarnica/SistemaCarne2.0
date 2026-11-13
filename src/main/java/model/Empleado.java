package model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "empleado")
public class Empleado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private Double pagoManiana;
    private Double pagoTarde;
    private String  tokenM;
    private String  tokenT;
    private String medioPago;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public Double getPagoManiana() {
        return pagoManiana;
    }
    public void setPagoManiana(Double pagoManiana) {
        this.pagoManiana = pagoManiana;
    }
    public Double getPagoTarde() {
        return pagoTarde;
    }
    public void setPagoTarde(Double pagoTarde) {
        this.pagoTarde = pagoTarde;
    }

    @Override
    public String toString() {
        return this.nombre; 
    }
    public String getTokenM() {
        return tokenM;
    }
    public void setTokenM(String tokenM) {
        this.tokenM = tokenM;
    }
    public String getTokenT() {
        return tokenT;
    }
    public void setTokenT(String tokenT) {
        this.tokenT = tokenT;
    }

    public String getMedioPago() {
        return medioPago == null || medioPago.isBlank() ? "Efectivo" : medioPago;
    }
    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

}
