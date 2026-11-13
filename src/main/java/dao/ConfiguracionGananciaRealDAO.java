package dao;

import Util.HibernateUtil;
import model.ConfiguracionGananciaReal;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ConfiguracionGananciaRealDAO {

    /** Fila única de configuración global; se crea con el default (3%) si todavía no existe. */
    public ConfiguracionGananciaReal obtenerOCrear() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            ConfiguracionGananciaReal existente = session
                    .createQuery("FROM ConfiguracionGananciaReal", ConfiguracionGananciaReal.class)
                    .setMaxResults(1)
                    .uniqueResult();
            if (existente != null) {
                return existente;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ConfiguracionGananciaReal nueva = new ConfiguracionGananciaReal();
        nueva.setUmbralDesviacionPrecio(0.03);

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(nueva);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
        return nueva;
    }

    public void actualizar(ConfiguracionGananciaReal config) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(config);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
