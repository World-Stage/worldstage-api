package com.jonathanfletcher.worldstage_api.exception;

public class InvalidStreamKeyException extends RuntimeException{

    public InvalidStreamKeyException(String message, Throwable e) {super(message, e);}

    public InvalidStreamKeyException(String message) {super(message);}

}
