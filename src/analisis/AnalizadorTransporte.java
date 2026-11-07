package analisis;

import modelo.Desplazamiento;
import modelo.EstadoRuta;
import modelo.Registro;
import modelo.Tramo;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class AnalizadorTransporte {

    public static final int CAPACIDAD_POR_DEFECTO = 10_000;

    public static final Predicate<Registro> ES_ENTRADA = Registro::esEntrada;
    public static final Predicate<Registro> ES_SALIDA = ES_ENTRADA.negate();
    public static final Predicate<Registro> TODOS = registro -> true;

    private AnalizadorTransporte() { }

    static <T> Stream<T> flujo(Collection<T> datos, boolean paralelo) {
        return paralelo ? datos.parallelStream() : datos.stream();
    }

    public static <K> Map<K, Long> contarPor(List<Registro> registros,
                                             Predicate<Registro> filtro,
                                             Function<Registro, K> clasificador,
                                             boolean paralelo) {
        return flujo(registros, paralelo)
                .filter(filtro)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(clasificador, Collectors.counting()),
                        Map::copyOf));
    }

    public static <K> Function<Map<K, Long>, List<Map.Entry<K, Long>>> topN(int n) {
        // El desempate por nombre evita que dos ejecuciones muestren rankings distintos con valores iguales.
        return conteos -> conteos.entrySet().stream()
                .sorted(Map.Entry.<K, Long>comparingByValue().reversed()
                        .thenComparing(entrada -> String.valueOf(entrada.getKey())))
                .limit(n)
                .map(entrada -> Map.entry(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    // a) Afluencia por estación

    public static Map<String, Long> afluenciaPorEstacion(List<Registro> registros, boolean paralelo) {
        return contarPor(registros, ES_ENTRADA, Registro::estacion, paralelo);
    }

    public static Map<String, Long> salidasPorEstacion(List<Registro> registros, boolean paralelo) {
        return contarPor(registros, ES_SALIDA, Registro::estacion, paralelo);
    }

    // b) Horas pico

    public static Map<Integer, Long> flujoPorHora(List<Registro> registros, boolean paralelo) {
        return contarPor(registros, TODOS, Registro::hora, paralelo);
    }

    // Umbral relativo al máximo: con un número fijo, el criterio dejaría de servir al cambiar el volumen.
    public static List<Map.Entry<Integer, Long>> horasPico(Map<Integer, Long> flujoPorHora, double factor) {
        long maximo = flujoPorHora.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        Predicate<Map.Entry<Integer, Long>> esHoraPico = alcanzaUmbral(factor * maximo);
        return flujoPorHora.entrySet().stream()
                .filter(esHoraPico)
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entrada -> Map.entry(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    public static <K> Predicate<Map.Entry<K, Long>> alcanzaUmbral(double umbral) {
        return entrada -> entrada.getValue() >= umbral;
    }

    // c) Rutas más utilizadas

    public static List<Map.Entry<String, Long>> rutasMasUtilizadas(List<Registro> registros, int n, boolean paralelo) {
        Function<List<Registro>, Map<String, Long>> contarIngresosPorRuta =
                datos -> contarPor(datos, ES_ENTRADA, Registro::ruta, paralelo);
        return contarIngresosPorRuta
                .andThen(AnalizadorTransporte.<String>topN(n))
                .apply(registros);
    }
}
