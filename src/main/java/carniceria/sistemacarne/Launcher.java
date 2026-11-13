package carniceria.sistemacarne;

/**
 * Punto de entrada del .jar / .exe.
 *
 * JavaFX no deja arrancar un .jar cuya clase principal hereda de
 * Application si JavaFX no está en el module-path ("JavaFX runtime
 * components are missing"). Esta clase no hereda de Application, así que
 * el sistema arranca igual desde el .jar con todo adentro.
 */
public class Launcher {

    public static void main(String[] args) {
        App.main(args);
    }
}
