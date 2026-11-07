package modelo;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

public record Registro(String idUsuario, String ruta, String estacion, Accion accion, LocalDateTime timestamp) {

    // El usuario desempata registros con el mismo timestamp para que el orden sea estable.
    public static final Comparator<Registro> ORDEN_CRONOLOGICO =
            Comparator.comparing(Registro::timestamp).thenComparing(Registro::idUsuario);

    public Registro {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(estacion, "estacion");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public boolean esEntrada() {
        return accion == Accion.ENTRADA;
    }

    public int hora() {
        return timestamp.getHour();
    }
}
