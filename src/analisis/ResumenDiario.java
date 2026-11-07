package analisis;

import modelo.EstadoRuta;
import modelo.Tramo;

import java.util.List;
import java.util.Map;

// Al ser record, equals compara todo el contenido: sirve para verificar secuencial vs paralelo.
public record ResumenDiario(long totalRegistros,
                            Map<String, Long> afluenciaPorEstacion,
                            Map<String, Long> salidasPorEstacion,
                            Map<Integer, Long> flujoPorHora,
                            List<Map.Entry<Integer, Long>> horasPico,
                            List<Map.Entry<String, Long>> rutasMasUtilizadas,
                            Map<String, List<String>> patronesPorUsuario,
                            double tiempoPromedioMinutos,
                            Map<Tramo, Double> tiempoPromedioPorTramo,
                            List<EstadoRuta> estadoRutas) {
}
