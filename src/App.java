import analisis.AnalizadorTransporte;
import analisis.Configuracion;
import analisis.ResumenDiario;
import datos.GeneradorDatos;
import modelo.Registro;
import reporte.Reporte;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// Único lugar con efectos secundarios (consola y reloj); el resto del proyecto son funciones puras.
public final class App {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 6);
    private static final long SEMILLA = 2026L;

    private static final Configuracion CONFIGURACION = new Configuracion(
            Map.of(
                    "T1 Troncal Norte-Sur", 20_000,
                    "T2 Troncal Oriente-Occidente", 18_000,
                    "A3 Alimentadora Colinas", 12_000,
                    "A4 Alimentadora Ribera", 11_000,
                    "E5 Expreso Aeropuerto", 6_000,
                    "C6 Circular Centro", 16_000),
            0.85,
            0.80,
            6);

    private record Medicion<T>(T valor, long milisegundos) { }

    private App() { }

    public static void main(String[] args) {
        int usuarios = args.length > 0 ? Integer.parseInt(args[0]) : 200_000;

        System.out.println("=".repeat(78));
        System.out.println(" TecnoMovil Data · Procesamiento funcional de datos de transporte · TecnoValle");
        System.out.println("=".repeat(78));

        Medicion<List<Registro>> generacion = medir(() -> GeneradorDatos.generar(usuarios, FECHA, SEMILLA));
        List<Registro> registros = generacion.valor();
        int huellaOriginal = registros.hashCode();
        System.out.printf(" Registros simulados: %,d (%,d usuarios) generados en %,d ms%n",
                registros.size(), usuarios, generacion.milisegundos());
        System.out.printf(" Núcleos disponibles: %d%n", Runtime.getRuntime().availableProcessors());

        // La primera pasada calienta el JIT; sin ella la medición secuencial sale inflada.
        AnalizadorTransporte.analizar(registros, CONFIGURACION, false);
        Medicion<ResumenDiario> secuencial = medir(() -> AnalizadorTransporte.analizar(registros, CONFIGURACION, false));
        Medicion<ResumenDiario> paralelo = medir(() -> AnalizadorTransporte.analizar(registros, CONFIGURACION, true));

        System.out.println(Reporte.formatear(paralelo.valor()));

        System.out.println();
        System.out.println("=".repeat(78));
        System.out.println(" VERIFICACIONES DEL PARADIGMA FUNCIONAL");
        System.out.println("=".repeat(78));
        System.out.printf(" Tiempo stream() secuencial : %,6d ms%n", secuencial.milisegundos());
        System.out.printf(" Tiempo parallelStream()    : %,6d ms%n", paralelo.milisegundos());
        System.out.printf(" Resultados secuencial == paralelo : %b%n", secuencial.valor().equals(paralelo.valor()));
        System.out.printf(" Lista original sin modificar      : %b%n", huellaOriginal == registros.hashCode());
        System.out.printf(" Lista original es inmutable       : %b%n", esInmutable(registros));
    }

    private static <T> Medicion<T> medir(Supplier<T> calculo) {
        long inicio = System.nanoTime();
        T valor = calculo.get();
        return new Medicion<>(valor, (System.nanoTime() - inicio) / 1_000_000);
    }

    private static boolean esInmutable(List<Registro> registros) {
        try {
            registros.add(registros.get(0));
            return false;
        } catch (UnsupportedOperationException esperado) {
            return true;
        }
    }
}
