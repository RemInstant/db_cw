package org.example.model;

public record UserExamStat(long id,
                           long userId,
                           String subjectTitle,
                           int tasksCount,
                           int correctTasksCount) {
}
