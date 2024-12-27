package org.example.model;

public record Task(int id,
                   int groupId,
                   String statement,
                   byte[] image,
                   String answer) {

}
