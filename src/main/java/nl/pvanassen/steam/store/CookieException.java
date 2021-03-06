package nl.pvanassen.steam.store;

/**
 * Exception thrown when steam is bitching about cookies
 * 
 * @author Paul van Assen
 */
public class CookieException extends RuntimeException {
    /**
     * Default constructor with build in message
     */
    public CookieException() {
        super("Error posting data, got cookie error back from Steam");
    }

    CookieException(String message) {
        super(message);
    }
}
