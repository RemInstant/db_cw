package org.example.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.model.AppUser;
import org.example.repositories.CredentialsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.ConnectException;
import java.security.Principal;

@Controller
public class CredentialsController {

  private static final Logger log = LoggerFactory.getLogger(CredentialsController.class);

  private final CredentialsRepository credentialsRepository;
  private final AuthenticationManager authenticationManager;

  public CredentialsController(CredentialsRepository credentialsRepository,
                               AuthenticationManager authenticationManager) {
    this.credentialsRepository = credentialsRepository;
    this.authenticationManager = authenticationManager;
  }

  @GetMapping("/login")
  public String getLoginPage(Principal principal) {
    if (principal != null) {
      return "redirect:/home";
    }

    return "login.html";
  }

  @GetMapping("/register")
  public String getRegisterPage(Principal principal) {
    if (principal != null) {
      return "redirect:/home";
    }

    return "register.html";
  }

  @PostMapping("/register")
  public String postRegisterPage(HttpServletRequest request, AppUser appUser, Model model, Principal principal) {
    if (principal != null) {
      return "redirect:/home";
    }

    try {
      if (credentialsRepository.isUserPresentByUsername(appUser.username())) {
        request.getSession()
            .setAttribute("CREDENTIALS_EXCEPTION_MESSAGE", "Пользователь с таким именем уже существует");
        return "redirect:/register?error";
      }

      String encryptedPassword = new BCryptPasswordEncoder().encode(appUser.password());

      credentialsRepository.saveUser(appUser.username(), encryptedPassword);

      UsernamePasswordAuthenticationToken authRequest =
          new UsernamePasswordAuthenticationToken(appUser.username(), appUser.password());

      Authentication authentication = authenticationManager.authenticate(authRequest);
      SecurityContextHolder.getContext().setAuthentication(authentication);
      HttpSession session = request.getSession(true);
      session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
    } catch (CannotGetJdbcConnectionException ex) {
      request.getSession()
          .setAttribute("CREDENTIALS_EXCEPTION_MESSAGE", "Не удалось подключиться к базе данных");
      return "redirect:/register?error";
    }

    return "redirect:/home";
  }
}
