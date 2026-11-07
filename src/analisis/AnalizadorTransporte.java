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

    // d) Patrones de viaje por usuario

    // groupingBy (no el concurrente) respeta el orden de encuentro también en paralelo.
    public static Map<String, List<String>> patronesPorUsuario(List<Registro> registros, boolean paralelo) {
        return flujo(registros, paralelo)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::idUsuario,
                                Collectors.collectingAndThen(
                                        Collectors.mapping(Registro::estacion, Collectors.toList()),
                                        AnalizadorTransporte::sinRepetidosConsecutivos)),
                        Map::copyOf));
    }

    // Al salir y volver a entrar en la misma estación aparece dos veces seguidas; se deja una.
    public static List<String> sinRepetidosConsecutivos(List<String> estaciones) {
        return IntStream.range(0, estaciones.size())
                .filter(i -> i == 0 || !estaciones.get(i).equals(estaciones.get(i - 1)))
                .mapToObj(estaciones::get)
                .toList();
    }

    // e) Tiempo promedio entre estaciones

    // Salida → entrada es tiempo de espera entre viajes, no de recorrido; por eso solo cuenta entrada → salida.
    private static final BiPredicate<Registro, Registro> ES_VIAJE = (actual, siguiente) ->
            ES_ENTRADA.test(actual)
                    && ES_SALIDA.test(siguiente)
                    && !actual.estacion().equals(siguiente.estacion());

    public static List<Desplazamiento> desplazamientos(List<Registro> registros, boolean paralelo) {
        Map<String, List<Registro>> porUsuario = flujo(registros, paralelo)
                .collect(Collectors.groupingBy(Registro::idUsuario));
        return flujo(porUsuario.values(), paralelo)
                .flatMap(registrosUsuario -> desplazamientosDeUsuario(registrosUsuario).stream())
                .toList();
    }

    public static List<Desplazamiento> desplazamientosDeUsuario(List<Registro> registrosUsuario) {
        List<Registro> ordenados = registrosUsuario.stream()
                .sorted(Registro.ORDEN_CRONOLOGICO)
                .toList();
        return IntStream.range(0, Math.max(ordenados.size() - 1, 0))
                .filter(i -> ES_VIAJE.test(ordenados.get(i), ordenados.get(i + 1)))
                .mapToObj(i -> crearDesplazamiento(ordenados.get(i), ordenados.get(i + 1)))
                .toList();
    }

    private static Desplazamiento crearDesplazamiento(Registro origen, Registro destino) {
        return new Desplazamiento(
                origen.idUsuario(),
                new Tramo(origen.estacion(), destino.estacion()),
                Duration.between(origen.timestamp(), destino.timestamp()).toMinutes());
    }

    // Minutos en long: la suma es exacta y el promedio no varía entre secuencial y paralelo.
    public static double tiempoPromedioMinutos(List<Desplazamiento> desplazamientos) {
        return desplazamientos.stream()
                .mapToLong(Desplazamiento::minutos)
                .average()
                .orElse(0.0);
    }

    public static Map<Tramo, Double> tiempoPromedioPorTramo(List<Desplazamiento> desplazamientos, boolean paralelo) {
        return flujo(desplazamientos, paralelo)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Desplazamiento::tramo,
                                Collectors.averagingLong(Desplazamiento::minutos)),
                        Map::copyOf));
    }

    // f) Detección de sobrecarga en rutas

    // Se evalúa la hora más cargada y no el total del día: la sobrecarga ocurre en franjas, no en promedio.
    public static List<EstadoRuta> detectarSobrecarga(List<Registro> registros,
                                                      Map<String, Integer> capacidadPorRuta,
                                                      double umbral,
                                                      boolean paralelo) {
        Map<String, Map<Integer, Long>> ingresosPorRutaYHora = flujo(registros, paralelo)
                .filter(ES_ENTRADA)
                .collect(Collectors.groupingBy(Registro::ruta,
                        Collectors.groupingBy(Registro::hora, Collectors.counting())));

        return ingresosPorRutaYHora.entrySet().stream()
                .map(evaluadorDeRuta(capacidadPorRuta, umbral))
                .sorted(Comparator.comparingDouble(EstadoRuta::ocupacion).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }

    static Function<Map.Entry<String, Map<Integer, Long>>, EstadoRuta> evaluadorDeRuta(
            Map<String, Integer> capacidadPorRuta, double umbral) {
        return entrada -> {
            Map.Entry<Integer, Long> horaMasCargada = entrada.getValue().entrySet().stream()
                    .max(Map.Entry.<Integer, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                    .orElseThrow();
            int capacidad = capacidadPorRuta.getOrDefault(entrada.getKey(), CAPACIDAD_POR_DEFECTO);
            double ocupacion = (double) horaMasCargada.getValue() / capacidad;
            return new EstadoRuta(entrada.getKey(), horaMasCargada.getKey(), horaMasCargada.getValue(),
                    capacidad, ocupacion, ocupacion > umbral);
        };
    }
}
