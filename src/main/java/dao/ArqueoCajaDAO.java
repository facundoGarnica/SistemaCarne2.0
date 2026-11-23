package dao;

import Util.HibernateUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import model.ArqueoCaja;
import org.hibernate.Session;
import org.hibernate.Transaction;

/** Conteos de efectivo por día y turno (control de caja del balance). */
public class ArqueoCajaDAO {

    public List<ArqueoCaja> buscarPorRango(LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM ArqueoCaja a WHERE a.fecha BETWEEN :inicio AND :fin ORDER BY a.fecha, a.turno DESC",
                    ArqueoCaja.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public ArqueoCaja buscar(LocalDate fecha, String turno) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return buscar(session, fecha, turno);
        }
    }

    private ArqueoCaja buscar(Session session, LocalDate fecha, String turno) {
        return session.createQuery("FROM ArqueoCaja a WHERE a.fecha = :fecha AND a.turno = :turno", ArqueoCaja.class)
                .setParameter("fecha", fecha)
                .setParameter("turno", turno)
                .uniqueResult();
    }

    /** Guarda (o reemplaza) el conteo de ese día y turno. */
    public void guardar(LocalDate fecha, String turno, double montoSistema, double montoContado) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            ArqueoCaja a = buscar(session, fecha, turno);
            if (a == null) {
                a = new ArqueoCaja();
                a.setFecha(fecha);
                a.setTurno(turno);
            }
            a.setMontoSistema(montoSistema);
            a.setMontoContado(montoContado);
            a.setRegistrado(LocalDateTime.now());
            session.merge(a);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /** Quita el conteo de ese día y turno (se desmarcaron las dos casillas). */
    public void borrar(LocalDate fecha, String turno) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("DELETE FROM ArqueoCaja a WHERE a.fecha = :fecha AND a.turno = :turno")
                    .setParameter("fecha", fecha)
                    .setParameter("turno", turno)
                    .executeUpdate();
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
