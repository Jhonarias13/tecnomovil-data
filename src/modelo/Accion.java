package modelo;

import java.util.Arrays;

public enum Accion {
    ENTRADA("entrada"),
    SALIDA("salida");

    private final String etiqueta;

    Accion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    // Los validadores envían el texto en minúscula y a veces con espacios.
    public static Accion desde(String texto) {
        return Arrays.stream(values())
                .filter(accion -> accion.etiqueta.equalsIgnoreCase(texto.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Acción desconocida: " + texto));
    }
}
