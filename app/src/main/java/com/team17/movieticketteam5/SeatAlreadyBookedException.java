
package com.team17.movieticketteam5;

public class SeatAlreadyBookedException
        extends RuntimeException {

    public SeatAlreadyBookedException(String message) {
        super(message);
    }
}
