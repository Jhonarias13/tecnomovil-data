package datos;

import modelo.Accion;
import modelo.Registro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class GeneradorDatos {

    private record RutaSimulada(String nombre, List<String> estaciones, int peso) { }

    private static final List<RutaSimulada> RUTAS = List.of(
            new RutaSimulada("T1 Troncal Norte-Sur",
                    List.of("Terminal Norte", "Plaza Central", "Universidad", "Hospital Regional",
                            "Parque Industrial", "Terminal Sur"), 5),
            new RutaSimulada("T2 Troncal Oriente-Occidente",
                    List.of("Portal Oriente", "Mercado Mayor", "Plaza Central", "Estadio", "Portal Occidente"), 4),
            new RutaSimulada("A3 Alimentadora Colinas",
                    List.of("Colinas Altas", "Barrio Jardín", "Mercado Mayor"), 2),
            new RutaSimulada("A4 Alimentadora Ribera",
                    List.of("Ribera del Río", "Zona Franca", "Parque Industrial"), 2),
            new RutaSimulada("E5 Expreso Aeropuerto",
                    List.of("Plaza Central", "Centro de Convenciones", "Aeropuerto"), 1),
            new RutaSimulada("C6 Circular Centro",
                    List.of("Plaza Central", "Biblioteca Pública", "Universidad", "Estadio"), 3));

    // Cada valor se repite según su peso; así elegir un índice al azar ya respeta la distribución.
    private static final List<RutaSimulada> BOLSA_RUTAS =
            expandir(RUTAS.stream().map(ruta -> Map.entry(ruta, ruta.peso())).toList());
    private static final List<Integer> HORAS_MANANA =
            expandir(List.of(Map.entry(5, 1), Map.entry(6, 3), Map.entry(7, 4), Map.entry(8, 2), Map.entry(9, 1)));
    private static final List<Integer> HORAS_MEDIODIA =
            expandir(List.of(Map.entry(11, 1), Map.entry(12, 2), Map.entry(13, 2), Map.entry(14, 1)));
    private static final List<Integer> HORAS_TARDE =
            expandir(List.of(Map.entry(16, 1), Map.entry(17, 3), Map.entry(18, 4), Map.entry(19, 2), Map.entry(20, 1)));

    private static final double PROBABILIDAD_VIAJE_MEDIODIA = 0.30;
    private static final int MINUTOS_POR_TRAMO = 4;

    private GeneradorDatos() { }

    public static List<String> rutas() {
        return RUTAS.stream().map(RutaSimulada::nombre).toList();
    }

    public static List<Registro> generar(int numeroUsuarios, LocalDate fecha, long semilla) {
        return IntStream.range(0, numeroUsuarios)
                .parallel()
                .mapToObj(indice -> simularJornada(indice, fecha, semilla))
                .flatMap(List::stream)
                .sorted(Registro.ORDEN_CRONOLOGICO)
                .toList();
    }

    // Un Random por usuario (derivado de la semilla) mantiene el resultado igual aunque se genere en paralelo.
    static List<Registro> simularJornada(int indiceUsuario, LocalDate fecha, long semilla) {
        SplittableRandom azar = new SplittableRandom(semilla ^ (indiceUsuario * 0x9E3779B97F4A7C15L));
        String idUsuario = "U%06d".formatted(indiceUsuario);
        RutaSimulada ruta = elegir(BOLSA_RUTAS, azar);
        int paradas = ruta.estaciones().size();

        int origen = azar.nextInt(paradas);
        int destino = elegirDistinta(paradas, List.of(origen), azar);
        int intermedia = elegirDistinta(paradas, List.of(origen, destino), azar);
        boolean viajaAlMediodia = azar.nextDouble() < PROBABILIDAD_VIAJE_MEDIODIA;

        // Todos los sorteos se hacen siempre, viaje o no al mediodía, para no desfasar la secuencia aleatoria.
        LocalDateTime inicioManana = fecha.atTime(elegir(HORAS_MANANA, azar), azar.nextInt(60));
        LocalDateTime inicioMediodia = fecha.atTime(elegir(HORAS_MEDIODIA, azar), azar.nextInt(60));
        LocalDateTime inicioTarde = fecha.atTime(elegir(HORAS_TARDE, azar), azar.nextInt(60));
        int extraManana = 2 + azar.nextInt(7);
        int extraMediodia = 2 + azar.nextInt(7);
        int extraTarde = 2 + azar.nextInt(7);

        Stream<List<Registro>> viajes = viajaAlMediodia
                ? Stream.of(
                        viaje(idUsuario, ruta, origen, destino, inicioManana, extraManana),
                        viaje(idUsuario, ruta, destino, intermedia, inicioMediodia, extraMediodia),
                        viaje(idUsuario, ruta, intermedia, origen, inicioTarde, extraTarde))
                : Stream.of(
                        viaje(idUsuario, ruta, origen, destino, inicioManana, extraManana),
                        viaje(idUsuario, ruta, destino, origen, inicioTarde, extraTarde));

        return viajes.flatMap(List::stream).toList();
    }

    private static List<Registro> viaje(String idUsuario, RutaSimulada ruta, int origen, int destino,
                                        LocalDateTime inicio, int minutosExtra) {
        long duracion = (long) Math.abs(destino - origen) * MINUTOS_POR_TRAMO + minutosExtra;
        return List.of(
                new Registro(idUsuario, ruta.nombre(), ruta.estaciones().get(origen), Accion.ENTRADA, inicio),
                new Registro(idUsuario, ruta.nombre(), ruta.estaciones().get(destino), Accion.SALIDA,
                        inicio.plusMinutes(duracion)));
    }

    private static int elegirDistinta(int paradas, List<Integer> excluidas, SplittableRandom azar) {
        List<Integer> candidatas = IntStream.range(0, paradas)
                .filter(indice -> !excluidas.contains(indice))
                .boxed()
                .toList();
        return elegir(candidatas, azar);
    }

    private static <T> T elegir(List<T> opciones, SplittableRandom azar) {
        return opciones.get(azar.nextInt(opciones.size()));
    }

    private static <T> List<T> expandir(List<Map.Entry<T, Integer>> pesos) {
        return pesos.stream()
                .flatMap(entrada -> Collections.nCopies(entrada.getValue(), entrada.getKey()).stream())
                .toList();
    }
}
