package com.example.theatre.service;

import com.example.theatre.dto.AuthResponse;
import com.example.theatre.dto.LoginRequest;
import com.example.theatre.dto.RegisterRequest;
import com.example.theatre.dto.UserResponse;
import com.example.theatre.entity.Role;
import com.example.theatre.entity.User;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.InvalidCredentialsException;
import com.example.theatre.exception.InvalidRequestException;
import com.example.theatre.mapper.UserMapper;
import com.example.theatre.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expiresInSeconds;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       @Value("${app.jwt.issuer}") String issuer,
                       @Value("${app.jwt.expires-in-seconds}") long expiresInSeconds) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expiresInSeconds = expiresInSeconds;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Password must be at most 72 UTF-8 bytes");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.CUSTOMER);
        try {
            return UserMapper.toResponse(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException("Email is already registered");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .claim("roles", List.of(user.getRole().name()))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return new AuthResponse(token, "Bearer", expiresInSeconds, UserMapper.toResponse(user));
    }
}
