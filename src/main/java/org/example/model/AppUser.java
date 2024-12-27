package org.example.model;

public record AppUser(String username,
                      String password,
                      String[] authorities) {

}
