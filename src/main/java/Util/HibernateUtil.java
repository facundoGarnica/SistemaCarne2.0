package Util;

import java.io.File;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Crear la carpeta de la base de datos si no existe
            File dbDir = new File("C:/SistemaCarne/data");
            if (!dbDir.exists()) {
                dbDir.mkdirs(); // crea SistemaCarne y data
                System.out.println("Carpeta de base de datos creada en: " + dbDir.getAbsolutePath());
            }

            // Construir SessionFactory desde hibernate.cfg.xml. Los datos de
            // conexión se pueden pisar con config.properties (uno por PC), así
            // no hace falta recompilar si otra PC tiene otra contraseña.
            Configuration cfg = new Configuration().configure("hibernate.cfg.xml");
            pisarSiExiste(cfg, "hibernate.connection.url", "db.url");
            pisarSiExiste(cfg, "hibernate.connection.username", "db.usuario");
            pisarSiExiste(cfg, "hibernate.connection.password", "db.contrasena");
            return cfg.buildSessionFactory();

        } catch (Throwable ex) {
            System.err.println("Error creando SessionFactory: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static void pisarSiExiste(Configuration cfg, String propiedadHibernate, String claveConfig) {
        String valor = ConfiguracionApp.valor(claveConfig);
        if (valor != null) {
            cfg.setProperty(propiedadHibernate, valor);
        }
    }

    /** URL, usuario y contraseña con los que se conecta (para el backup). */
    public static String[] datosConexion() {
        Configuration cfg = new Configuration().configure("hibernate.cfg.xml");
        String url = ConfiguracionApp.valor("db.url");
        String usuario = ConfiguracionApp.valor("db.usuario");
        String clave = ConfiguracionApp.valor("db.contrasena");
        return new String[]{
            url != null ? url : cfg.getProperty("hibernate.connection.url"),
            usuario != null ? usuario : cfg.getProperty("hibernate.connection.username"),
            clave != null ? clave : cfg.getProperty("hibernate.connection.password")
        };
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
