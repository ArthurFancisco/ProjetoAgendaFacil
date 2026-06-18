package br.com.agendafacil.security;

import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {
    private final UserRepository userRepository;
    private final int maxAttempts;
    private final int lockoutMinutes;

    public LoginFailureHandler(UserRepository userRepository,
                               @Value("${app.security.max-login-attempts}") int maxAttempts,
                               @Value("${app.security.lockout-minutes}") int lockoutMinutes) {
        this.userRepository = userRepository;
        this.maxAttempts = maxAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    @Override
    @Transactional
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        if (email != null) {
            userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
                user.setFailedAttempts(user.getFailedAttempts() + 1);
                if (user.getFailedAttempts() >= maxAttempts) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
                }
                userRepository.save(user);
            });
        }
        response.sendRedirect("/login?error");
    }
}
