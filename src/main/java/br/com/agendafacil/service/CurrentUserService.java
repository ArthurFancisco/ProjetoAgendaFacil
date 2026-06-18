package br.com.agendafacil.service;

import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.exception.BusinessException;
import br.com.agendafacil.repository.UserRepository;
import br.com.agendafacil.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser current)) {
            throw new BusinessException("Usuário não autenticado.");
        }
        return userRepository.findWithEstablishmentById(current.id())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public UUID currentEstablishmentId() {
        AppUser user = currentUser();
        if (user.getEstablishment() == null) {
            throw new BusinessException("Usuário sem estabelecimento vinculado.");
        }
        return user.getEstablishment().getId();
    }
}
