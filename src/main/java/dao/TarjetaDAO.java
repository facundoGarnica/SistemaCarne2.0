package dao;

import Util.HibernateUtil;
import java.util.List;
import model.Tarjeta;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class TarjetaDAO {

    public List<Tarjeta> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Tarjeta", Tarjeta.class).list();
        }
    }

    public Tarjeta buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Tarjeta.class, id);
        }
    }

    public Tarjeta buscarPorCodigoBarra(String codigoBarra) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Tarjeta t WHERE t.codigoBarra = :codigoBarra",
                    Tarjeta.class)
                    .setParameter("codigoBarra", codigoBarra)
                    .uniqueResult();
        }
    }

    public void guardar(Tarjeta tarjeta) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(tarjeta);
            tx.commit();

        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }

    public void actualizar(Tarjeta tarjeta) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.update(tarjeta);
            tx.commit();

        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }

    public void eliminar(Long id) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Tarjeta tarjeta = session.get(Tarjeta.class, id);
            if (tarjeta != null) {
                session.delete(tarjeta);
            }

            tx.commit();

        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }

    public List<Tarjeta> buscarActivas() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Tarjeta t WHERE t.estado = true",
                    Tarjeta.class)
                    .list();
        }
    }

    public List<Tarjeta> buscarInactivas() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Tarjeta t WHERE t.estado = false",
                    Tarjeta.class)
                    .list();
        }
    }
}