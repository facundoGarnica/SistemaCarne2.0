package dao;

import Util.HibernateUtil;
import java.time.LocalDate;
import java.util.List;
import model.CierreInventario;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class CierreInventarioDAO {

    public List<CierreInventario> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM CierreInventario c ORDER BY c.fechaCierre DESC", CierreInventario.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<CierreInventario> buscarPorRango(LocalDate desde, LocalDate hasta) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM CierreInventario c WHERE c.fechaCierre BETWEEN :desde AND :hasta ORDER BY c.fechaCierre DESC",
                    CierreInventario.class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public CierreInventario buscarPorFechaYCategoria(LocalDate fechaCierre, String categoria) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM CierreInventario c WHERE c.fechaCierre = :fecha AND c.categoria = :categoria",
                    CierreInventario.class)
                    .setParameter("fecha", fechaCierre)
                    .setParameter("categoria", categoria)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void guardar(CierreInventario cierre) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(cierre);
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
            CierreInventario cierre = session.get(CierreInventario.class, id);
            if (cierre != null) {
                session.delete(cierre);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
