package com.finsight.finsight_api.user;

import com.finsight.finsight_api.user.UpdateEmailRequest;
import com.finsight.finsight_api.user.UpdatePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updateEmail(UpdateEmailRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 1. Validar la contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("The current password is incorrect.");
        }

        // 2. Validar que el nuevo correo no esté en uso por otra cuenta
        if (userRepository.findByEmail(request.getNewEmail()).isPresent()) {
            throw new IllegalArgumentException("The entered email is already registered.");
        }

        // 3. Actualizar correo y guardar
        currentUser.setEmail(request.getNewEmail());
        userRepository.save(currentUser);
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 1. Validar la contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("The current password is incorrect.");
        }

        // 2. Encriptar y actualizar la nueva contraseña
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }
}