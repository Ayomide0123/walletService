package com.hng.walletService.service;

import com.hng.walletService.model.dto.response.JwtResponse;
import com.hng.walletService.model.entity.UserEntity;
import com.hng.walletService.repository.UserRepository;
import com.hng.walletService.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final JwtUtil jwtUtil;

    @Transactional
    public JwtResponse handleGoogleCallback(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String picture = oAuth2User.getAttribute("picture");

        // Find or create user
        UserEntity user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> createNewUser(email, name, googleId, picture));

        // Create wallet if doesn't exist
        try {
            walletService.getWalletByUser(user);
        } catch (RuntimeException e) {
            walletService.createWallet(user);
        }

        // Generate JWT
        String token = jwtUtil.generateToken(user.getEmail());

        log.info("User authenticated: {}", email);

        return JwtResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    private UserEntity createNewUser(String email, String name, String googleId, String picture) {
        UserEntity user = UserEntity.builder()
                .email(email)
                .name(name)
                .googleId(googleId)
                .picture(picture)
                .isActive(true)
                .build();

        UserEntity savedUser = userRepository.save(user);
        log.info("New user created: {}", email);

        return savedUser;
    }
}
