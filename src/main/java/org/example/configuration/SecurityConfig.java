package org.example.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  @Bean
  public OncePerRequestFilter oncePerRequestFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
          String username = authentication.getName();
          String roles = authentication.getAuthorities().toString();
          System.out.println(username + ' ' + roles);
        } else {
          System.out.println("mregwrg");
        }

        filterChain.doFilter(request, response);
      }
    };
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity)
      throws Exception {
    return httpSecurity
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(form -> form
            .loginPage("/login").permitAll()
            .defaultSuccessUrl("/home", true)
            .failureHandler(authenticationFailureHandler()))
        .logout(logout -> logout
            .logoutSuccessUrl("/home")
            .deleteCookies("JSESSIONID"))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/css/**").permitAll()
            .requestMatchers("/home").permitAll()
            .requestMatchers("/login").permitAll()
            .requestMatchers("/register").permitAll()
            .requestMatchers("/admin").hasRole("ADMIN")
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .build();
  }

  @Bean
  public AuthenticationFailureHandler authenticationFailureHandler() {
    return (request, response, exception) -> {
      String message = exception.getMessage();

      if (exception.getCause() != null) {
        if (exception.getCause() instanceof CannotGetJdbcConnectionException) {
          message = "Не удалось подключиться к базе данных";
        }
      } else if (exception instanceof BadCredentialsException) {
        message = "Неверные учётные данные пользователя";
      }

      request.getSession().setAttribute("CREDENTIALS_EXCEPTION_MESSAGE", message);
      response.sendRedirect("/login?error");
    };
  }

//  @Bean
//  public UserDetailsService users() {
//    UserDetails user = User.builder()
//        .username("user")
//        .password("{noop}user")
//        .roles("USER")
//        .build();
//    UserDetails admin = User.builder()
//        .username("admin")
//        .password("{noop}admin")
//        .roles("USER", "ADMIN")
//        .build();
//    return new InMemoryUserDetailsManager(user, admin);
//  }

}
