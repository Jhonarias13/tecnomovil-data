package modelo;

public record EstadoRuta(String ruta,
                         int horaMasCargada,
                         long entradasEnHora,
                         int capacidadPorHora,
                         double ocupacion,
                         boolean critica) {

    public String estado() {
        return critica ? "CRÍTICA" : "NORMAL";
    }
}
