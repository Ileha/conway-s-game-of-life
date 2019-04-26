package app.figure.Exceptions;

public abstract class FigureException extends Exception {
    private String message;

    public FigureException(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
