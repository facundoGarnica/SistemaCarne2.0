package Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Controla la "Zona con cobertura inalámbrica" (Mobile Hotspot) moderna de
 * Windows para que los clientes puedan conectarse a la red WiFi ya
 * configurada en el equipo (Configuración > Red e Internet > Zona con
 * cobertura inalámbrica).
 *
 * A diferencia de la vieja "Red hospedada" (netsh wlan hostednetwork), esta
 * API (NetworkOperatorTetheringManager, vía PowerShell) no requiere
 * privilegios de administrador ni tareas programadas: el mismo usuario que
 * corre la app de ventas puede prender y apagar la zona WiFi directamente,
 * igual que lo haría desde la Configuración de Windows o el Centro de
 * actividades. El SSID y la contraseña son los que el usuario ya definió
 * en Windows; esta clase no los toca.
 */
public class WifiHotspotManager {

    private static final String SCRIPT = """
            $ProgressPreference = 'SilentlyContinue'
            Add-Type -AssemblyName System.Runtime.WindowsRuntime
            [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager,Windows.Networking.NetworkOperators,ContentType=WindowsRuntime] | Out-Null

            $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
                $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
            })[0]

            function Await($WinRtTask, $ResultType) {
                $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
                $netTask = $asTask.Invoke($null, @($WinRtTask))
                $netTask.Wait(-1) | Out-Null
                $netTask.Result
            }

            try {
                $perfil = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile()
                if ($null -eq $perfil) {
                    Write-Output "ERROR:SIN_CONEXION"
                    exit 1
                }

                $manager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager]::CreateFromConnectionProfile($perfil)
                $resultType = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringOperationResult]

                switch ("%s") {
                    "start" {
                        $resultado = Await ($manager.StartTetheringAsync()) $resultType
                        Write-Output ("RESULTADO:" + $resultado.Status)
                    }
                    "stop" {
                        $resultado = Await ($manager.StopTetheringAsync()) $resultType
                        Write-Output ("RESULTADO:" + $resultado.Status)
                    }
                }

                Write-Output ("ESTADO:" + $manager.TetheringOperationalState)
            } catch {
                Write-Output ("ERROR:" + $_.Exception.Message)
                exit 1
            }
            """;

    public static boolean estaActivo() {
        String salida = ejecutar("status");
        return salida != null && salida.contains("ESTADO:On");
    }

    public static boolean activar() {
        String salida = ejecutar("start");
        return salida != null && salida.contains("ESTADO:On");
    }

    public static boolean desactivar() {
        String salida = ejecutar("stop");
        return salida != null && salida.contains("ESTADO:Off");
    }

    private static String ejecutar(String accion) {
        try {
            String script = String.format(SCRIPT, accion);
            String base64 = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));

            Process proceso = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-EncodedCommand", base64
            ).redirectErrorStream(true).start();

            StringBuilder salida = new StringBuilder();
            try (BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    salida.append(linea).append(System.lineSeparator());
                }
            }
            proceso.waitFor();
            return salida.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
