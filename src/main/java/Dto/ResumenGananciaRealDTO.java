package Dto;

import java.util.List;

public class ResumenGananciaRealDTO {
    private final boolean tieneDatos;
    private final double totalVentas;
    private final double totalGastos;
    private final double totalSueldos;
    private final double cmv;
    private final double gananciaReal;
    private final double margenReal;
    private final double gananciaSobreCosto;
    private final List<AlertaDTO> alertas;

    public ResumenGananciaRealDTO(boolean tieneDatos, double totalVentas, double totalGastos, double totalSueldos,
            double cmv, double gananciaReal, double margenReal, double gananciaSobreCosto, List<AlertaDTO> alertas) {
        this.tieneDatos = tieneDatos;
        this.totalVentas = totalVentas;
        this.totalGastos = totalGastos;
        this.totalSueldos = totalSueldos;
        this.cmv = cmv;
        this.gananciaReal = gananciaReal;
        this.margenReal = margenReal;
        this.gananciaSobreCosto = gananciaSobreCosto;
        this.alertas = alertas;
    }

    public boolean isTieneDatos() {
        return tieneDatos;
    }

    public double getTotalVentas() {
        return totalVentas;
    }

    public double getTotalGastos() {
        return totalGastos;
    }

    public double getTotalSueldos() {
        return totalSueldos;
    }

    public double getCmv() {
        return cmv;
    }

    public double getGananciaReal() {
        return gananciaReal;
    }

    public double getMargenReal() {
        return margenReal;
    }

    public double getGananciaSobreCosto() {
        return gananciaSobreCosto;
    }

    public List<AlertaDTO> getAlertas() {
        return alertas;
    }
}
