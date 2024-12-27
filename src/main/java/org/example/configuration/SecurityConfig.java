package org.example.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.services.AppUserDetailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thymeleaf.DialectConfiguration;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.expression.Lists;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.standard.StandardDialect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.IntStream;

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
            .failureHandler((request, response, exception) -> {
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
            })
        )
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
//        .sessionManagement(session -> session
//            .sessionCreationPolicy(SessionCreationPolicy.NEVER))
        .build();
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
