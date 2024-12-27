package org.example.model;

public record NumberedTask(int id,
                           int serialNumber,
                           String statement,
                           byte[] image,
                           String answerFormat,
                           String answer) {

}
