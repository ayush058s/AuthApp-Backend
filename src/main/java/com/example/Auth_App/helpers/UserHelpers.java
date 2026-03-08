package com.example.Auth_App.helpers;

import java.util.UUID;

public class UserHelpers {

    public static UUID parseUUID(String uuidString){
        return UUID.fromString(uuidString);
    }
}
