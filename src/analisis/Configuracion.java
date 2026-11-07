package analisis;

import java.util.Map;

public record Configuracion(Map<String, Integer> capacidadPorRuta,
                            double umbralSobrecarga,
                            double factorHoraPico,
                            int topRutas) {

    public Configuracion {
        // Copia defensiva: si quien la crea modifica su mapa después, aquí no cambia nada.
        capacidadPorRuta = Map.copyOf(capacidadPorRuta);
    }
}
