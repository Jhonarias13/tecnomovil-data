package pruebas;

import analisis.AnalizadorTransporte;
import analisis.Configuracion;
import datos.GeneradorDatos;
import modelo.Accion;
import modelo.EstadoRuta;
import modelo.Registro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// Sin JUnit para cumplir "sin frameworks externos"; los valores esperados se calcularon a mano.
public final class PruebasAnalizador {

    private record Resultado(String nombre, boolean exito, String detalle) { }

    private static final LocalDate DIA = LocalDate.of(2026, 11, 6);

    // U1 hace A → C → A por R1 y U2 hace B → C → B por R2.
    private static final List<Registro> DATOS = List.of(
            registro("U1", "R1", "A", Accion.ENTRADA, 7, 0),
            registro("U2", "R2", "B", Accion.ENTRADA, 7, 5),
            registro("U2", "R2", "C", Accion.SALIDA, 7, 15),
            registro("U1", "R1", "C", Accion.SALIDA, 7, 20),
            registro("U2", "R2", "C", Accion.ENTRADA, 12, 0),
            registro("U2", "R2", "B", Accion.SALIDA, 12, 30),
            registro("U1", "R1", "C", Accion.ENTRADA, 17, 10),
            registro("U1", "R1", "A", Accion.SALIDA, 17, 40));

    private PruebasAnalizador() { }

    public static void main(String[] args) {
        List<Resultado> resultados = List.of(
                prueba("a) Afluencia por estación", () ->
                        AnalizadorTransporte.afluenciaPorEstacion(DATOS, false)
                                .equals(Map.of("A", 1L, "B", 1L, "C", 2L))),
                prueba("b) Flujo por hora", () ->
                        AnalizadorTransporte.flujoPorHora(DATOS, false)
                                .equals(Map.of(7, 4L, 12, 2L, 17, 2L))),
                prueba("b) Horas pico (80 % del máximo)", () ->
                        AnalizadorTransporte.horasPico(AnalizadorTransporte.flujoPorHora(DATOS, false), 0.8)
                                .equals(List.of(Map.entry(7, 4L)))),
                prueba("c) Rutas más utilizadas", () ->
                        AnalizadorTransporte.rutasMasUtilizadas(DATOS, 5, false)
                                .equals(List.of(Map.entry("R1", 2L), Map.entry("R2", 2L)))),
                prueba("d) Patrones de viaje por usuario", () ->
                        AnalizadorTransporte.patronesPorUsuario(DATOS, false)
                                .equals(Map.of("U1", List.of("A", "C", "A"), "U2", List.of("B", "C", "B")))),
                prueba("e) Tiempo promedio entre estaciones", () ->
                        AnalizadorTransporte.tiempoPromedioMinutos(
                                AnalizadorTransporte.desplazamientos(DATOS, false)) == 22.5),
                prueba("f) Sobrecarga: R1 crítica y R2 normal", () -> {
                    List<EstadoRuta> estado = AnalizadorTransporte.detectarSobrecarga(
                            DATOS, Map.of("R1", 1, "R2", 2), 0.9, false);
                    return estado.size() == 2
                            && estado.get(0).ruta().equals("R1") && estado.get(0).critica()
                            && estado.get(1).ruta().equals("R2") && !estado.get(1).critica();
                }),
                prueba("Inmutabilidad: la lista original no cambia", () -> {
                    List<Registro> copia = new ArrayList<>(DATOS);
                    AnalizadorTransporte.analizar(DATOS, configuracion(), true);
                    return copia.equals(DATOS);
                }),
                prueba("Inmutabilidad: los resultados no se pueden modificar", () -> {
                    try {
                        AnalizadorTransporte.afluenciaPorEstacion(DATOS, false).put("X", 99L);
                        return false;
                    } catch (UnsupportedOperationException esperado) {
                        return true;
                    }
                }),
                prueba("Funciones puras: misma entrada, mismo resultado", () ->
                        AnalizadorTransporte.analizar(DATOS, configuracion(), false)
                                .equals(AnalizadorTransporte.analizar(DATOS, configuracion(), false))),
                prueba("Paralelismo: stream() y parallelStream() coinciden", () -> {
                    List<Registro> datos = GeneradorDatos.generar(5_000, DIA, 7L);
                    return AnalizadorTransporte.analizar(datos, configuracion(), false)
                            .equals(AnalizadorTransporte.analizar(datos, configuracion(), true));
                }),
                prueba("Generador determinista (misma semilla)", () ->
                        GeneradorDatos.generar(1_000, DIA, 7L).equals(GeneradorDatos.generar(1_000, DIA, 7L))));

        resultados.forEach(r -> System.out.printf(" [%s] %s%s%n",
                r.exito() ? "OK  " : "FALLA", r.nombre(), r.detalle().isEmpty() ? "" : " → " + r.detalle()));

        long fallidas = resultados.stream().filter(r -> !r.exito()).count();
        System.out.printf("%n %d pruebas, %d exitosas, %d fallidas%n",
                resultados.size(), resultados.size() - fallidas, fallidas);
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static Configuracion configuracion() {
        return new Configuracion(Map.of("R1", 1, "R2", 2), 0.9, 0.8, 5);
    }

    private static Registro registro(String usuario, String ruta, String estacion, Accion accion, int hora, int minuto) {
        return new Registro(usuario, ruta, estacion, accion, LocalDateTime.of(DIA.getYear(), DIA.getMonth(), DIA.getDayOfMonth(), hora, minuto));
    }

    private static Resultado prueba(String nombre, Supplier<Boolean> condicion) {
        try {
            return new Resultado(nombre, condicion.get(), "");
        } catch (RuntimeException error) {
            return new Resultado(nombre, false, error.toString());
        }
    }
}
