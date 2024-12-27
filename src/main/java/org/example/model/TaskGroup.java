package org.example.model;

public record TaskGroup(int id,
                        int subjectId,
                        int serialNumber,
                        String title,
                        String answerFormat) {

}
