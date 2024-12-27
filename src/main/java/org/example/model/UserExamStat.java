package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record UserExamStat(long id,
                           long userId,
                           String examTitle,
                           LocalDateTime submitDateTime,
                           int tasksCount,
                           int correctTasksCount) {

  public String formattedSubmitDateTime() {
    return submitDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
  }

}
