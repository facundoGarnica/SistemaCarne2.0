package dao;

import Util.HibernateUtil;
import java.time.LocalDate;
import java.util.List;
import model.Gastos;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class GastoDAO {

    public List<Gastos> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Gastos g ORDER BY g.id DESC", Gastos.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Gastos> buscarPorRango(LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Gastos g WHERE g.fecha BETWEEN :inicio AND :fin ORDER BY g.id DESC",
                    Gastos.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void guardar(Gastos gasto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(gasto);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    public void actualizar(Gastos gasto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.update(gasto);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    public void eliminar(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Gastos gasto = session.get(Gastos.class, id);
            if (gasto != null) {
                session.delete(gasto);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    // Método útil para el cálculo de ganancias restando estos gastos
    public Double sumarGastosPorRango(LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double total = session.createQuery(
                    "SELECT SUM(g.monto) FROM Gastos g WHERE g.fecha BETWEEN :inicio AND :fin",
                    Double.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .uniqueResult();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public Double sumarGastosPorRangoYMedioPago(LocalDate inicio, LocalDate fin, String medioPago) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double total = session.createQuery(
                    "SELECT SUM(g.monto) FROM Gastos g WHERE g.fecha BETWEEN :inicio AND :fin AND g.medioPago = :medioPago",
                    Double.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .setParameter("medioPago", medioPago)
                    .uniqueResult();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }
}