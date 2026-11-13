package dao;

import Util.HibernateUtil;
import model.MercadoPagoToken;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class MercadoPagoTokenDAO {

    public void guardar(MercadoPagoToken token) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            tx = session.beginTransaction();

            session.save(token);

            tx.commit();

        } catch (Exception e) {

            if (tx != null) {
                tx.rollback();
            }

            e.printStackTrace();
        }
    }

    public void actualizar(MercadoPagoToken token) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            tx = session.beginTransaction();

            session.merge(token);

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

            MercadoPagoToken token =
                    session.get(MercadoPagoToken.class, id);

            if (token != null) {
                session.delete(token);
            }

            tx.commit();

        } catch (Exception e) {

            if (tx != null) {
                tx.rollback();
            }

            e.printStackTrace();
        }
    }

    public MercadoPagoToken obtenerToken() {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            return session.createQuery(
                    "FROM MercadoPagoToken",
                    MercadoPagoToken.class
            )
            .setMaxResults(1)
            .uniqueResult();

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

}