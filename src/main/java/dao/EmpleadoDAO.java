package dao;

import Util.HibernateUtil;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import model.Empleado;
import model.TurnoEmpleados;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class EmpleadoDAO {

    public List<Empleado> buscarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Empleado", Empleado.class).list();
        }
    }

    public void guardar(Empleado empleado) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            session.save(empleado);

            tx.commit();
        } catch (Exception e) {

            if (tx != null) {
                tx.rollback();
            }

            e.printStackTrace();
            throw new RuntimeException("No se pudo guardar el empleado.", e);
        }
    }

    public void actualizar(Empleado empleado) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            session.update(empleado);

            tx.commit();

        } catch (Exception e) {

            if (tx != null) {
                tx.rollback();
            }

            e.printStackTrace();
            throw new RuntimeException("No se pudo actualizar el empleado.", e);
        }
    }

    public void eliminar(Long id) {

        Transaction tx = null;

        try (Session session =
                    HibernateUtil
                            .getSessionFactory()
                            .openSession()) {

            tx = session.beginTransaction();

            // 1) Eliminar todos los turnos del empleado
            eliminarTurnosEmpleado(id, session);

            // 2) Buscar empleado
            Empleado empleado =
                    session.get(Empleado.class, id);

            // 3) Eliminar empleado
            if (empleado != null) {
                session.remove(empleado);
            }

            tx.commit();

        } catch (Exception e) {

            if (tx != null) {
                tx.rollback();
            }

            e.printStackTrace();
            throw new RuntimeException("No se pudo eliminar el empleado.", e);
        }
    }

    // Registrar turno a un empleado
    public void registrarTurno(TurnoEmpleados turno) {

        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            tx = session.beginTransaction();

            session.save(turno);

            tx.commit();

        } catch (Exception e) {

            if (tx != null) {
                tx.rollback();
            }

            e.printStackTrace();
            throw new RuntimeException("No se pudo registrar el turno.", e);
        }

    }

    public boolean yaRegistroTurno(
            Long empleadoId,
            LocalDate fecha,
            String turno) {

        try (Session session = HibernateUtil
                .getSessionFactory()
                .openSession()) {

            String hql = "SELECT COUNT(t) " +
                    "FROM TurnoEmpleados t " +
                    "WHERE t.empleado.id = :id " +
                    "AND t.fecha = :fecha " +
                    "AND t.turno = :turno";

            Long cantidad = session.createQuery(hql, Long.class)
                    .setParameter("id", empleadoId)
                    .setParameter("fecha", fecha)
                    .setParameter("turno", turno)
                    .uniqueResult();

            return cantidad != null && cantidad > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }

    // Obtener todos los turnos de un empleado
    public List<TurnoEmpleados> obtenerTurnosDeEmpleado(Long empleadoId) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            return session.createQuery(
                    "FROM TurnoEmpleados t WHERE t.empleado.id = :empleadoId",
                    TurnoEmpleados.class)
                    .setParameter("empleadoId", empleadoId)
                    .list();

        } catch (Exception e) {

            e.printStackTrace();

            return List.of();
        }

    }

    // Obtener turnos por fecha
    public List<TurnoEmpleados> obtenerTurnosPorFecha(LocalDate fecha) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            return session.createQuery(
                    "FROM TurnoEmpleados t WHERE t.fecha = :fecha",
                    TurnoEmpleados.class)
                    .setParameter("fecha", fecha)
                    .list();

        } catch (Exception e) {

            e.printStackTrace();

            return List.of();
        }

    }

    public Double totalPagadoSemana(LocalDate inicio, LocalDate fin) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double total = session.createQuery(
                    "SELECT SUM(t.montoHistorial) FROM TurnoEmpleados t WHERE t.fecha BETWEEN :inicio AND :fin",
                    Double.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .uniqueResult();

            return total != null ? total : 0.0;

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }

    }

    public void eliminarTurno(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            TurnoEmpleados turno = session.get(TurnoEmpleados.class, id);
            if (turno != null)
                session.delete(turno);
            tx.commit();
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            e.printStackTrace();
        }
    }

    public void eliminarTurnosEmpleado(Long empleadoId, Session session) {

        session.createMutationQuery(
                "DELETE FROM TurnoEmpleados t WHERE t.empleado.id = :id"
        )
        .setParameter("id", empleadoId)
        .executeUpdate();
    }

    public List<TurnoEmpleados> obtenerTodosLosTurnos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Usamos HQL para traer todos los registros de la tabla TurnoEmpleados
            // 'JOIN FETCH' asegura que el objeto Empleado se cargue junto con el turno
            return session.createQuery(
                    "SELECT t FROM TurnoEmpleados t LEFT JOIN FETCH t.empleado ORDER BY t.fecha DESC",
                    TurnoEmpleados.class).list();
        } catch (Exception e) {
            System.err.println("Error al obtener turnos: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<TurnoEmpleados> obtenerTurnosPorRango(LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT t FROM TurnoEmpleados t LEFT JOIN FETCH t.empleado WHERE t.fecha BETWEEN :inicio AND :fin ORDER BY t.fecha DESC",
                    TurnoEmpleados.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .list();
        } catch (Exception e) {
            System.err.println("Error al obtener turnos por rango: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Todos los turnos de un empleado dentro de un rango de fechas
    public List<TurnoEmpleados> obtenerTurnosDeEmpleadoEnRango(Long empleadoId, LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM TurnoEmpleados t WHERE t.empleado.id = :empleadoId " +
                    "AND t.fecha BETWEEN :inicio AND :fin ORDER BY t.fecha",
                    TurnoEmpleados.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Los empleados cobran únicamente los sábados: todos los turnos trabajados
     * de domingo a sábado se pagan (y por lo tanto se descuentan de la caja) el
     * sábado de esa semana. Dado un rango [inicio, fin] devuelve el rango de
     * fechas de turno cuyo sábado de pago cae dentro del rango, o null si el
     * rango no incluye ningún sábado. El domingo no impacta los sueldos.
     */
    private LocalDate[] rangoTurnosPagadosEnRango(LocalDate inicio, LocalDate fin) {
        LocalDate primerSabado = inicio.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        LocalDate ultimoSabado = fin.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        if (primerSabado.isAfter(ultimoSabado)) {
            return null;
        }
        return new LocalDate[] { primerSabado.minusDays(6), ultimoSabado };
    }

    // Indica si el rango incluye al menos un sábado (día en que impactan los sueldos)
    public boolean rangoIncluyeDiaDePago(LocalDate inicio, LocalDate fin) {
        return rangoTurnosPagadosEnRango(inicio, fin) != null;
    }

    private static final String FILTRO_EFECTIVO =
            "AND (t.empleado.medioPago IS NULL OR t.empleado.medioPago = 'Efectivo')";
    private static final String FILTRO_TRANSFERENCIA = "AND t.empleado.medioPago = 'Transferencia'";

    // Sueldos pagados en el período (sábados incluidos) a empleados que cobran en efectivo
    public Double totalSueldosEfectivoEnRango(LocalDate inicio, LocalDate fin) {
        return sumarSueldosPagados(inicio, fin, FILTRO_EFECTIVO);
    }

    // Sueldos pagados en el período (sábados incluidos) a empleados que cobran por transferencia
    public Double totalSueldosTransferenciaEnRango(LocalDate inicio, LocalDate fin) {
        return sumarSueldosPagados(inicio, fin, FILTRO_TRANSFERENCIA);
    }

    // Sueldos devengados (turnos trabajados) por fecha de turno, cobren o no en el rango: efectivo
    public Double totalSueldosTrabajadosEfectivoEnRango(LocalDate inicio, LocalDate fin) {
        return sumarTurnos(inicio, fin, FILTRO_EFECTIVO);
    }

    // Sueldos devengados (turnos trabajados) por fecha de turno, cobren o no en el rango: transferencia
    public Double totalSueldosTrabajadosTransferenciaEnRango(LocalDate inicio, LocalDate fin) {
        return sumarTurnos(inicio, fin, FILTRO_TRANSFERENCIA);
    }

    private Double sumarSueldosPagados(LocalDate inicio, LocalDate fin, String filtroMedioPago) {
        LocalDate[] rango = rangoTurnosPagadosEnRango(inicio, fin);
        if (rango == null) {
            return 0.0;
        }
        return sumarTurnos(rango[0], rango[1], filtroMedioPago);
    }

    private Double sumarTurnos(LocalDate inicio, LocalDate fin, String filtroMedioPago) {
        LocalDate[] rango = { inicio, fin };
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Double total = session.createQuery(
                    "SELECT SUM(t.montoHistorial) FROM TurnoEmpleados t " +
                    "WHERE t.fecha BETWEEN :desde AND :hasta " + filtroMedioPago,
                    Double.class)
                    .setParameter("desde", rango[0])
                    .setParameter("hasta", rango[1])
                    .uniqueResult();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public String desglosarToken(int token) {

        // Convertir a String
        String tokenStr = String.format("%08d", token);

        // Obtener partes
        String idParte = tokenStr.substring(0, 4);
        String turnoParte = tokenStr.substring(4, 5);

        // Convertir ID
        Long empleadoId = Long.parseLong(idParte);

        // Determinar turno
        String turno = turnoParte.equals("1") ? "Mañana" : "Tarde";

        return "Empleado ID: " + empleadoId + " | Turno: " + turno;
    }
}