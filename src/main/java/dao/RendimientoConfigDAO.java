package dao;

import Util.HibernateUtil;
import java.util.List;
import model.RendimientoConfig;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class RendimientoConfigDAO {

    public List<RendimientoConfig> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM RendimientoConfig", RendimientoConfig.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public RendimientoConfig buscarPorCategoria(String categoria) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM RendimientoConfig r WHERE r.categoria = :categoria",
                    RendimientoConfig.class)
                    .setParameter("categoria", categoria)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Devuelve la config de la categoría, creándola con los defaults del negocio
     * si todavía no fue cargada (evita romper el cálculo cuando no se configuró nada).
     */
    public RendimientoConfig obtenerOCrearDefault(String categoria) {
        RendimientoConfig existente = buscarPorCategoria(categoria);
        if (existente != null) {
            return existente;
        }

        RendimientoConfig nuevo = new RendimientoConfig();
        nuevo.setCategoria(categoria);
        nuevo.setUmbralAlertaPorcentaje(0.90);
        if ("Carniceria".equals(categoria)) {
            nuevo.setPorcentajeAprovechable(0.851);
            nuevo.setCostoReferenciaKg(10350.0);
        } else {
            // Sin dato histórico propio todavía: se deja en 0 (alerta deshabilitada
            // hasta que el usuario cargue el % real desde la pantalla de configuración).
            nuevo.setPorcentajeAprovechable(0.0);
            nuevo.setCostoReferenciaKg(null);
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(nuevo);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
        return nuevo;
    }

    public void actualizar(RendimientoConfig config) {
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
