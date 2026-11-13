package dao;

import Util.HibernateUtil;
import java.time.LocalDate;
import java.util.List;
import model.Compra;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class CompraDAO {

    public List<Compra> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Compra c ORDER BY c.fecha DESC", Compra.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Compra> buscarPorRango(LocalDate desde, LocalDate hasta) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Compra c WHERE c.fecha BETWEEN :desde AND :hasta ORDER BY c.fecha DESC",
                    Compra.class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Compra> buscarPorRangoYCategoria(LocalDate desde, LocalDate hasta, String categoria) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Compra c WHERE c.fecha BETWEEN :desde AND :hasta AND c.categoria = :categoria",
                    Compra.class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .setParameter("categoria", categoria)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void guardar(Compra compra) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(compra);
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
            Compra compra = session.get(Compra.class, id);
            if (compra != null) {
                session.delete(compra);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
