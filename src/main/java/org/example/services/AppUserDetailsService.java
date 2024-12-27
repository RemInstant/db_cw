package org.example.services;

import org.example.model.AppUser;
import org.example.repositories.CredentialsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppUserDetailsService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(AppUserDetailsService.class);

  private final CredentialsRepository credentialsRepository;

  public AppUserDetailsService(CredentialsRepository credentialsRepository) {
    this.credentialsRepository = credentialsRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<AppUser> user = credentialsRepository.getUserByUsername(username);

    if (user.isEmpty()) {
      throw new UsernameNotFoundException("username '%s' not found".formatted(username));
    }

    log.info("i am alive!!!!!");

    return User.builder()
        .username(user.get().username())
        .password(user.get().password())
        .authorities(user.get().authorities())
        .build();
  }
}
