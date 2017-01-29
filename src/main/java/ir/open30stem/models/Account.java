package ir.open30stem.models;

import org.springframework.data.annotation.Id;

import java.io.Serializable;

/**
 * Created by Erfan Sharafzadeh on 11/17/16.
 */

public class Account implements Serializable {
    @Id
    private String id;

    public String username;
    public String token;

    public Account(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public Account(){

    }
}
