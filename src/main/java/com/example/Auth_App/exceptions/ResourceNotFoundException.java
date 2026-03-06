package com.example.Auth_App.exceptions;


public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message){
        super(message);
    }
    public ResourceNotFoundException(){
        super("Resource Not Found");
    }
}
