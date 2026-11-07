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
}
