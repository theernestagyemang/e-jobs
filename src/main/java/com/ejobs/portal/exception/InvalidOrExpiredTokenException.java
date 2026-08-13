package com.ejobs.portal.exception;

/**
 * Password reset token that is unknown, already redeemed, or past its expiry. All three
 * cases share one exception and one message so the response cannot be used to probe
 * which tokens exist. Maps to 400.
 */
public class InvalidOrExpiredTokenException extends RuntimeException {

    public InvalidOrExpiredTokenException(String message) {
        super(message);
    }
}
