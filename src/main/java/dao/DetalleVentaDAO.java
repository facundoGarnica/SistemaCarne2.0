/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

/**
 *
 * @author garca
 */
import Util.HibernateUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.DetalleVenta;
import model.Venta;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class DetalleVentaDAO {

    public List<DetalleVenta> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM DetalleVenta", DetalleVenta.class).list();
        }
    }

    public void eliminar(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            DetalleVenta detalle = session.get(DetalleVenta.class, id);
            if (detalle != null) {
                session.delete(detalle);
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }

    public void guardar(DetalleVenta detalle) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(detalle);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }

    public void actualizar(DetalleVenta detalle) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.update(detalle);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }

    public List<DetalleVenta> buscarPorVenta(Venta venta) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Usar JOIN FETCH para cargar la relación con Producto de forma eager
            String hql = "FROM DetalleVenta dv JOIN FETCH dv.producto WHERE dv.venta = :venta";

            List<DetalleVenta> detalles = session.createQuery(hql, DetalleVenta.class)
                    .setParameter("venta", venta)
                    .list();

            // Debug: verificar que los productos se cargaron
            System.out.println("Consultando detalles para venta ID: " + venta.getId());
            for (DetalleVenta detalle : detalles) {
                System.out.println("Detalle encontrado - Producto: "
                        + (detalle.getProducto() != null ? detalle.getProducto().getNombre() : "null")
                        + ", Peso: " + detalle.getPeso() + ", Precio: " + detalle.getPrecio());
            }

            return detalles;

        } catch (Exception e) {
            System.err.println("Error en buscarPorVenta: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // Retornar lista vacía en caso de error
        }
    }

    /**
     * Busca los detalles de venta (con su producto) de todas las ventas
     * realizadas dentro del rango de fechas indicado, para poder armar
     * reportes agregados por producto (kilos vendidos, total facturado, etc).
     */
    public List<DetalleVenta> buscarPorRangoFechas(LocalDate desde, LocalDate hasta) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDateTime inicio = desde.atStartOfDay();
            LocalDateTime fin = hasta.atTime(23, 59, 59, 999999999);

            String hql = "FROM DetalleVenta dv JOIN FETCH dv.producto "
                    + "WHERE dv.venta.fecha >= :desde AND dv.venta.fecha <= :hasta";

            return session.createQuery(hql, DetalleVenta.class)
                    .setParameter("desde", inicio)
                    .setParameter("hasta", fin)
                    .list();

        } catch (Exception e) {
            System.err.println("Error en buscarPorRangoFechas: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
