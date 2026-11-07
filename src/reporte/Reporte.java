package reporte;

import analisis.AnalizadorTransporte;
import analisis.ResumenDiario;
import modelo.EstadoRuta;
import modelo.Tramo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Devuelve el texto en vez de imprimirlo; así el formateo también queda sin efectos secundarios.
public final class Reporte {

    private static final String LINEA = "-".repeat(78);
    private static final String SALTO = System.lineSeparator();

    private Reporte() { }

    public static String formatear(ResumenDiario resumen) {
        return String.join(SALTO,
                titulo("a) AFLUENCIA POR ESTACIÓN (ingresos y salidas del día)"),
                afluencia(resumen),
                titulo("b) FLUJO POR HORA Y HORAS PICO"),
                flujoPorHora(resumen),
                titulo("c) RUTAS MÁS UTILIZADAS"),
                rutas(resumen),
                titulo("d) PATRONES DE VIAJE POR USUARIO (muestra de 8 usuarios)"),
                patrones(resumen),
                titulo("e) TIEMPO PROMEDIO ENTRE ESTACIONES CONSECUTIVAS"),
                tiempos(resumen),
                titulo("f) DETECCIÓN DE SOBRECARGA EN RUTAS"),
                sobrecarga(resumen));
    }

    private static String titulo(String texto) {
        return String.join(SALTO, "", LINEA, " " + texto, LINEA);
    }

    private static String afluencia(ResumenDiario resumen) {
        return AnalizadorTransporte.<String>topN(Integer.MAX_VALUE)
                .apply(resumen.afluenciaPorEstacion())
                .stream()
                .map(e -> " %-26s %,10d ingresos   %,10d salidas".formatted(
                        e.getKey(), e.getValue(), resumen.salidasPorEstacion().getOrDefault(e.getKey(), 0L)))
                .collect(Collectors.joining(SALTO));
    }

    private static String flujoPorHora(ResumenDiario resumen) {
        long maximo = resumen.flujoPorHora().values().stream().mapToLong(Long::longValue).max().orElse(1L);
        List<Integer> horasPico = resumen.horasPico().stream().map(Map.Entry::getKey).toList();
        String tabla = resumen.flujoPorHora().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> " %02d:00  %,9d  %-40s%s".formatted(
                        e.getKey(), e.getValue(),
                        "█".repeat((int) Math.round(40.0 * e.getValue() / maximo)),
                        horasPico.contains(e.getKey()) ? " ◄ PICO" : ""))
                .collect(Collectors.joining(SALTO));
        String pico = resumen.horasPico().stream()
                .map(e -> "%02d:00 (%,d)".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
        return tabla + SALTO + " Horas pico: " + pico;
    }

    private static String rutas(ResumenDiario resumen) {
        List<Map.Entry<String, Long>> ranking = resumen.rutasMasUtilizadas();
        return IntStream.range(0, ranking.size())
                .mapToObj(i -> " %d. %-30s %,10d ingresos".formatted(
                        i + 1, ranking.get(i).getKey(), ranking.get(i).getValue()))
                .collect(Collectors.joining(SALTO));
    }

    private static String patrones(ResumenDiario resumen) {
        return resumen.patronesPorUsuario().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(8)
                .map(e -> " %s: %s".formatted(e.getKey(), String.join(" → ", e.getValue())))
                .collect(Collectors.joining(SALTO))
                + SALTO
                + " Usuarios con patrón calculado: %,d".formatted(resumen.patronesPorUsuario().size());
    }

    private static String tiempos(ResumenDiario resumen) {
        String encabezado = " Promedio general: %.2f minutos por desplazamiento".formatted(resumen.tiempoPromedioMinutos());
        String tramos = resumen.tiempoPromedioPorTramo().entrySet().stream()
                .sorted(Map.Entry.<Tramo, Double>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().toString()))
                .limit(6)
                .map(e -> "   %-50s %6.2f min".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(SALTO));
        return encabezado + SALTO + " Tramos más lentos:" + SALTO + tramos;
    }

    private static String sobrecarga(ResumenDiario resumen) {
        return resumen.estadoRutas().stream()
                .sorted(Comparator.comparingDouble(EstadoRuta::ocupacion).reversed())
                .map(r -> " %-30s hora %02d:00  %,7d / %,7d  %6.1f%%  %s".formatted(
                        r.ruta(), r.horaMasCargada(), r.entradasEnHora(), r.capacidadPorHora(),
                        r.ocupacion() * 100, r.estado()))
                .collect(Collectors.joining(SALTO));
    }
}
