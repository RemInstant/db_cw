package org.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Service
public class BackupService {

  private static final Logger log = LoggerFactory.getLogger(BackupService.class);

  private final String containerName;
  private final String dbUser;
  private final String dbName;
  private final String backupPath;
  private final String backupLogPath;
  private final String remoteBackupPath;

  public BackupService(Environment environment) {
    String dataSourceUrl = environment.getProperty("spring.datasource.url");

    if (dataSourceUrl == null) {
      log.error("Property 'spring.datasource.url' not found. Please set the property and rerun application.");
      dataSourceUrl = "/ ";
    }

    containerName = environment.getProperty("container.name");
    dbUser = environment.getProperty("spring.datasource.username");
    dbName = dataSourceUrl.substring(dataSourceUrl.lastIndexOf("/") + 1);
    backupPath = environment.getProperty("backup.path");
    backupLogPath = environment.getProperty("backup.logPath");
    remoteBackupPath = environment.getProperty("backup.remoteBackupPath");
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void createBackup() {

    // TODO: append date to log

    try {
      ProcessBuilder processBuilder = new ProcessBuilder("docker", "exec", containerName,
          "pg_dump", "-U", dbUser, "-Fc", dbName);
      processBuilder
          .redirectError(ProcessBuilder.Redirect.appendTo(new File(backupLogPath)))
          .redirectOutput(ProcessBuilder.Redirect.to(new File(backupPath)))
          .start()
          .waitFor();

//      throw new IOException("123");

    } catch (IOException e) {
      // TODO: something
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void loadBackup(OutputStream outputStream) {
    createBackup();
    try {
      Path file = Paths.get(backupPath);
      Files.copy(file, outputStream);
      outputStream.flush();
    } catch (IOException e) {
//      LOG.info("Error writing file to output stream. Filename was '{}'" + fileName, e);
//      throw new RuntimeException("IOError writing file to output stream");
    }
  }

  public void restoreFromBackupFile(MultipartFile backupFile) {
    Objects.requireNonNull(backupFile);

    try {
      Path file = Paths.get(backupPath);
      Files.deleteIfExists(file);
      Files.copy(backupFile.getInputStream(), file);
    } catch (IOException e) {
      log.info("!!!", e);
//      LOG.info("Error writing file to output stream. Filename was '{}'" + fileName, e);
//      throw new RuntimeException("IOError writing file to output stream");
    }

    // TODO: append date to log file

    try {
      ProcessBuilder processBuilder = new ProcessBuilder("docker", "cp", backupPath,
          containerName + ":" + remoteBackupPath);
      processBuilder
          .redirectErrorStream(true)
          .redirectError(ProcessBuilder.Redirect.appendTo(new File(backupLogPath)))
          .redirectOutput(ProcessBuilder.Redirect.appendTo(new File(backupLogPath)))
          .start()
          .waitFor();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    try {
      ProcessBuilder processBuilder = new ProcessBuilder("docker", "exec", containerName,
          "pg_restore", "-U", dbUser, "--clean", "-d", dbName, remoteBackupPath);
      processBuilder
          .redirectErrorStream(true)
          .redirectError(ProcessBuilder.Redirect.appendTo(new File(backupLogPath)))
          .redirectOutput(ProcessBuilder.Redirect.appendTo(new File(backupLogPath)))
          .start()
          .waitFor();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
