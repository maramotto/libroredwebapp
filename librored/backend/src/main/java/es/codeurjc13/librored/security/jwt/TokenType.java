package es.codeurjc13.librored.security.jwt;

import java.time.Duration;

public enum TokenType {

    ACCESS(Duration.ofHours(2), "AuthToken"),
    REFRESH(Duration.ofDays(7), "RefreshToken");

    /**
     * Token lifetime in seconds
     */
    public final Duration duration;
    public final String cookieName;

    TokenType(Duration duration, String cookieName) {
        this.duration = duration;
        this.cookieName = cookieName;
    }
}