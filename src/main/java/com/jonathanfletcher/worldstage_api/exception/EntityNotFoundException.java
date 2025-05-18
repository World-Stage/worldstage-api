package com.jonathanfletcher.worldstage_api.exception;

public class EntityNotFoundException extends RuntimeException{

    public EntityNotFoundException(String message, Throwable e){
        super(message, e);
    }

    public EntityNotFoundException(String message){
        super(message);
    }
}
