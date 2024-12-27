package org.example.model;

import java.time.LocalDateTime;

public record AppUser(int id,
                      String username,
                      String password,
                      String[] authorities,
                      LocalDateTime registrationDateTime) {

}
