package modelo;

public record Tramo(String origen, String destino) {

    @Override
    public String toString() {
        return origen + " → " + destino;
    }
}
