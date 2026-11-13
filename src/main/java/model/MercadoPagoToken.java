package model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mercadoPagoToken")
public class MercadoPagoToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;
    private String userId;
    // External POS ID de cada caja (el QR impreso asociado). La columna
    // original "externalPosId" se conserva como la de la Caja 1.
    private String externalPosId;
    private String externalPosIdCaja2;
    private String externalPosIdCaja3;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getExternalPosId() {
        return externalPosId;
    }

    public void setExternalPosId(String externalPosId) {
        this.externalPosId = externalPosId;
    }

    public String getExternalPosIdCaja2() {
        return externalPosIdCaja2;
    }

    public void setExternalPosIdCaja2(String externalPosIdCaja2) {
        this.externalPosIdCaja2 = externalPosIdCaja2;
    }

    public String getExternalPosIdCaja3() {
        return externalPosIdCaja3;
    }

    public void setExternalPosIdCaja3(String externalPosIdCaja3) {
        this.externalPosIdCaja3 = externalPosIdCaja3;
    }

    /** External POS ID de la caja indicada (1, 2 o 3). */
    public String getExternalPosIdCaja(int numeroCaja) {
        switch (numeroCaja) {
            case 1: return externalPosId;
            case 2: return externalPosIdCaja2;
            case 3: return externalPosIdCaja3;
            default: throw new IllegalArgumentException("Caja inexistente: " + numeroCaja);
        }
    }

    public void setExternalPosIdCaja(int numeroCaja, String valor) {
        switch (numeroCaja) {
            case 1: externalPosId = valor; break;
            case 2: externalPosIdCaja2 = valor; break;
            case 3: externalPosIdCaja3 = valor; break;
            default: throw new IllegalArgumentException("Caja inexistente: " + numeroCaja);
        }
    }

}
