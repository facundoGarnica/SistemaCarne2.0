package dao;

import Util.HibernateUtil;
import java.time.LocalDate;
import java.util.List;
import model.Adelanto;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class AdelantoDAO {

    public List<Adelanto> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT a FROM Adelanto a LEFT JOIN FETCH a.empleado ORDER BY a.id DESC",
                    Adelanto.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Adelanto> buscarPorRango(LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT a FROM Adelanto a LEFT JOIN FETCH a.empleado WHERE a.fecha BETWEEN :inicio AND :fin ORDER BY a.id DESC",
                    Adelanto.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // Todos los adelantos de un empleado dentro de un rango de fechas
    public List<Adelanto> buscarPorEmpleadoYRango(Long empleadoId, LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Adelanto a WHERE a.empleado.id = :empleadoId " +
                    "AND a.fecha BETWEEN :inicio AND :fin ORDER BY a.fecha",
                    Adelanto.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void guardar(Adelanto adelanto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(adelanto);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw new RuntimeException("No se pudo guardar el adelanto.", e);
        }
    }

    public void actualizar(Adelanto adelanto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.update(adelanto);
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
            Adelanto adelanto = session.get(Adelanto.class, id);
            if (adelanto != null) {
                session.delete(adelanto);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    public Double sumarPorRangoYMedioPago(LocalDate inicio, LocalDate fin, String medioPago) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double total = session.createQuery(
                    "SELECT SUM(a.monto) FROM Adelanto a WHERE a.fecha BETWEEN :inicio AND :fin AND a.medioPago = :medioPago",
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
