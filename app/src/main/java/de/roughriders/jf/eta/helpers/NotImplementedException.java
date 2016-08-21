package de.roughriders.jf.eta.helpers;

/**
 * Exception to throw when a piece of code or feature has not yet been finished.
 */
public class NotImplementedException extends Exception {
    public NotImplementedException(String message) {
        super(message);
    }
}
