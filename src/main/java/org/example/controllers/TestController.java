package org.example.controllers;

import org.example.model.Test;
import org.example.repositories.TestRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@RestController
public class TestController {

  private static final Logger logger =
          Logger.getLogger(TestController.class.getName());

  private final TestRepo testRepo;

  public TestController(TestRepo testRepo) {
    this.testRepo = testRepo;
  }

  @GetMapping("/test")
  public Test getTest(
          @RequestParam(required = false) Integer testAbobus,
          @RequestParam(required = false) String testName) {
    Test test = new Test(Objects.requireNonNullElse(testAbobus, 5),
            Objects.requireNonNullElse(testName, "upi gloopy"));
    logger.info("Abobus: " + test);
    return test;
  }

  @GetMapping("/all")
  public List<Test> getAll() {
    return testRepo.getAll();
  }

  @GetMapping("/user")
  public UserDetails getUsers(@RequestParam(defaultValue = "aboba") String username) {
    return testRepo.getUserDetailsByUsername(username);
  }

  @PostMapping("/test")
  public ResponseEntity<Test> makeTest(@RequestBody Test test) {
    logger.info("Abobus: " + test.getAbobus());
    return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(test);
  }

//  @GetMapping("/login")
//  public String login() {
//    return "login.html";
//  }
}