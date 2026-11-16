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
import java.util.List;
import model.Producto;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ProductoDAO {

    public List<Producto> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Producto", Producto.class).list();
        }
    }

    public void eliminar(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Producto producto = session.get(Producto.class, id);
            if (producto != null) {
                session.delete(producto);
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }

    public void guardar(Producto producto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(producto);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }

   public void actualizar(Producto producto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(producto);   // 🔥 CLAVE
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
   
    /**
     * Cambia el precio de varios productos (id → precio nuevo) en una sola
     * transacción: si falla uno, no se cambia ninguno. Solo toca el precio.
     * Devuelve cuántos productos se actualizaron.
     */
    public int actualizarPrecios(java.util.Map<Long, Double> preciosPorId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            int actualizados = 0;
            for (java.util.Map.Entry<Long, Double> e : preciosPorId.entrySet()) {
                actualizados += session.createMutationQuery(
                        "UPDATE Producto p SET p.precio = :precio WHERE p.id = :id")
                        .setParameter("precio", e.getValue())
                        .setParameter("id", e.getKey())
                        .executeUpdate();
            }
            tx.commit();
            return actualizados;
        } catch (RuntimeException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
    }

    public List<Producto> obtenerProductoPorTipo(String tipo) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Producto p WHERE p.tipo = :tipo", Producto.class)
                    .setParameter("tipo", tipo)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Producto obtenerProductoPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Producto.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Producto buscarPorCodigo(int codigo) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Producto p WHERE p.codigo = :codigo", Producto.class)
                    .setParameter("codigo", codigo)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
