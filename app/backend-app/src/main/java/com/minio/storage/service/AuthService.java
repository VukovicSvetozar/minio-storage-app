package com.minio.storage.service;

import com.minio.storage.dto.*;
import com.minio.storage.entity.PasswordResetToken;
import com.minio.storage.entity.RefreshToken;
import com.minio.storage.entity.User;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.PasswordResetTokenRepository;
import com.minio.storage.repository.UserRepository;
import com.minio.storage.security.JwtUtil;
import com.minio.storage.util.ActivityLogHelper;
import com.minio.storage.util.TokenHasher;
import com.minio.storage.util.VerificationKeyGenerator;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserSecurityService userSecurityService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistTokenService blacklistTokenService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ActivityLogHelper activityLogHelper;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String username = normalize(request.getUsername());
        String email = normalize(request.getEmail());

        if (userRepository.existsByUsername(username)) {
            throw AppException.badRequest("Korisničko ime je već zauzeto");
        }

        if (userRepository.existsByEmail(email)) {
            throw AppException.badRequest("Email adresa se već koristi");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        String rawKey = VerificationKeyGenerator.generateKey();

        user.setVerificationKey(TokenHasher.sha256Hex(rawKey));
        LocalDateTime now = LocalDateTime.now();
        user.setVerificationKeySentAt(now);
        user.setVerificationKeyExpiresAt(now.plusHours(24));
        user.setEmailVerified(false);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition pri registraciji za korisničko ime: {} / email: {}", username, email);
            throw AppException.badRequest("Korisničko ime ili email već postoje");
        }

        boolean emailSent = emailService.sendVerificationEmail(
                savedUser.getEmail(),
                savedUser.getUsername(),
                rawKey
        );

        if (!emailSent) {
            log.error("Neuspješno slanje verifikacionog emaila za: {}", savedUser.getEmail());
            throw AppException.internalError("Registracija nije uspjela zbog greške u slanju emaila. Pokušajte ponovo.");
        }

        log.info("Novi korisnik je registrovan: {}", savedUser.getUsername());
        activityLogHelper.logRegistration(savedUser.getUsername(), httpRequest);

        return AuthResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .emailVerified(false)
                .build();
    }

    @Transactional
    public AuthResponse verifyEmail(VerificationRequest request, HttpServletRequest httpRequest) {
        String genericError = "Verifikacija nije uspjela. Ključ je nevažeći ili je nalog već verifikovan.";

        String username = normalize(request.getUsername());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.badRequest(genericError));

        if (user.isEmailVerified()) {
            log.info("Korisnik {} je već verifikovan, vraća se token.", user.getUsername());
            return generateAuthResponse(user);
        }

        if (user.getVerificationKey() == null) {
            throw AppException.badRequest(genericError);
        }

        if (user.getVerificationKeyExpiresAt() != null &&
                user.getVerificationKeyExpiresAt().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest(genericError);
        }

        String incomingHash = TokenHasher.sha256Hex(request.getKey());

        if (!incomingHash.equals(user.getVerificationKey())) {
            log.warn("Pokušaj verifikacije sa pogrešnim ključem za korisnika: {}", user.getUsername());
            throw AppException.badRequest(genericError);
        }

        user.setEmailVerified(true);
        user.setVerificationKey(null);
        User verifiedUser = userRepository.save(user);

        log.info("Email uspješno verifikovan za korisnika: {}", verifiedUser.getUsername());
        activityLogHelper.logEmailVerification(verifiedUser.getUsername(), httpRequest);

        emailService.sendWelcomeEmail(verifiedUser.getEmail(), verifiedUser.getUsername());

        return generateAuthResponse(verifiedUser);
    }

    @Transactional
    public void resendVerificationEmail(String username, HttpServletRequest httpRequest) {
        String normalized = normalize(username);
        User user = userRepository.findByUsername(normalized)
                .orElseThrow(() -> AppException.notFound("Korisnik nije pronađen"));

        if (user.isEmailVerified()) {
            throw AppException.badRequest("Email je već verifikovan");
        }

        LocalDateTime now = LocalDateTime.now();

        if (user.getVerificationKeySentAt() != null &&
                user.getVerificationKeySentAt().isAfter(now.minusMinutes(2))) {
            throw AppException.badRequest("Novi ključ možete zatražiti svaka 2 minuta");
        }

        String newRawKey = VerificationKeyGenerator.generateKey();
        user.setVerificationKey(TokenHasher.sha256Hex(newRawKey));
        user.setVerificationKeySentAt(now);
        user.setVerificationKeyExpiresAt(now.plusHours(24));

        userRepository.save(user);

        boolean emailSent = emailService.sendVerificationEmail(
                user.getEmail(),
                user.getUsername(),
                newRawKey
        );

        if (!emailSent) {
            log.error("Slanje verifikacionog emaila neuspješno za korisnika: {}", user.getEmail());
            throw AppException.internalError("Slanje email-a nije uspjelo. Pokušajte ponovo.");
        }

        log.info("Uspješno ponovljeno slanje za: {}", normalized);
        activityLogHelper.logResendVerification(user.getUsername(), httpRequest);

    }

    public boolean isUsernameTaken(String username) {
        String normalized = normalize(username);
        return userRepository.existsByUsername(normalized);
    }

    public boolean isEmailTaken(String email) {
        String normalized = normalize(email);
        return userRepository.existsByEmail(normalized);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        System.out.println(passwordEncoder.encode("system123*"));
        try {
            System.out.println("111");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalize(request.getUsernameOrEmail()),
                            request.getPassword()
                    )
            );
            System.out.println("222");
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();

            System.out.println("333");
            if (!user.isEmailVerified()) {
                log.warn("Pokušaj prijave sa neverifikovanim emailom: {}", user.getUsername());
                throw AppException.forbidden("Nalog nije verifikovan. Molimo potvrdite vašu email adresu.");
            }
            System.out.println("444");
            if (!user.isActive()) {
                log.warn("Pokušaj prijave na deaktiviran nalog: {}", user.getUsername());
                throw AppException.forbidden("Vaš nalog je suspendovan. Kontaktirajte podršku.");
            }
            System.out.println("555");
            String jwt = jwtUtil.generateToken(user);
            RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user);
            String refreshToken = refreshTokenEntity.getToken();
            Long expiresIn = jwtUtil.getExpirationInSeconds();

            log.info("Korisnik se uspješno prijavio: {}", user.getUsername());
            activityLogHelper.logLoginSuccess(user.getUsername(), httpRequest);

            return AuthResponse.builder()
                    .token(jwt)
                    .refreshToken(refreshToken)
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .expiresIn(expiresIn)
                    .emailVerified(true)
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Neuspješan pokušaj prijave (pogrešni podaci) za: {}", request.getUsernameOrEmail());
            throw AppException.unauthorized("Pogrešno korisničko ime ili lozinka", request.getUsernameOrEmail());

        } catch (DisabledException | LockedException e) {
            log.warn("Pokušaj prijave na onemogućen nalog (Spring nivo): {}", request.getUsernameOrEmail());
            throw AppException.unauthorized("Nalog je deaktiviran ili neverifikovan.", request.getUsernameOrEmail());
        }

    }

    public UserResponse getCurrentUser() {
        User user = userSecurityService.getCurrentUser();

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .build();
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());

        String newAccessToken = jwtUtil.generateToken(newRefreshToken.getUser());
        Long expiresIn = jwtUtil.getExpirationInSeconds();

        log.info("Token je uspješno osvježen za korisnika: {}", newRefreshToken.getUser().getUsername());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(expiresIn)
                .build();
    }

    @Transactional
    public void logout(String accessToken) {
        long expiry = jwtUtil.getExpirationTimeMs(accessToken);
        blacklistTokenService.blacklistToken(accessToken, expiry);

        String username = jwtUtil.extractUsername(accessToken);
        User user = findUserByUsername(username);

        refreshTokenService.revokeAllUserTokens(user);

        log.info("Korisnik {} uspješno odjavljen. Sve sesije poništene.", username);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String email = normalize(request.getEmail());
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (!user.isEmailVerified()) {
                log.warn("Zahtjev za reset lozinke za neverifikovan mail: {}", user.getEmail());
                return;
            }

            Optional<PasswordResetToken> existingToken = resetTokenRepository.findByUserAndUsedFalse(user);
            if (existingToken.isPresent() && existingToken.get().getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                log.info("Rate limit aktivan za korisnika: {}. Preskače se slanje.", user.getEmail());
                return;
            }

            resetTokenRepository.deleteByUser(user);

            String rawToken = java.util.UUID.randomUUID().toString();

            String tokenHash = TokenHasher.sha256Hex(rawToken);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setTokenHash(tokenHash);
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
            resetToken.setUsed(false);
            resetTokenRepository.save(resetToken);

            boolean emailSent = emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getUsername(),
                    rawToken
            );

            if (emailSent) {
                log.info("Reset email uspješno poslat korisniku: {}", user.getUsername());
                activityLogHelper.logPasswordResetRequest(user.getEmail(), httpRequest);
            } else {
                log.error("Greška pri slanju email-a za: {}", user.getEmail());
                throw AppException.internalError("Slanje email-a za reset lozinke nije uspjelo");
            }
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String incomingHash = TokenHasher.sha256Hex(request.getToken());

        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(() -> AppException.badRequest("Token je nevažeći ili je istekao."));

        if (!resetToken.isValid()) {
            if (resetToken.isExpired()) {
                log.warn("Istekao token reset za user: {}", resetToken.getUser().getUsername());
            }
            if (resetToken.isUsed()) {
                log.warn("Pokušaj ponovnog korišćenja tokena za user: {}", resetToken.getUser().getUsername());
            }
            throw AppException.badRequest("Token je nevažeći ili je istekao.");
        }

        User user = resetToken.getUser();
        try {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            resetToken.setUsed(true);
            resetTokenRepository.save(resetToken);

            refreshTokenService.revokeAllUserTokens(user);

            activityLogHelper.logPasswordResetSuccess(user.getUsername(), httpRequest);
            log.info("Lozinka je promijenjena za korisnika: {}", user.getUsername());
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            log.warn("Race condition pri resetovanju lozinke (token): {}", incomingHash);
            throw AppException.badRequest("Token je nevažeći ili je istekao.");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    public boolean validateResetToken(String rawToken) {
        String hash = TokenHasher.sha256Hex(rawToken);
        Optional<PasswordResetToken> resetToken = resetTokenRepository.findByTokenHash(hash);
        return resetToken.isPresent() && resetToken.get().isValid();
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(normalize(username))
                .orElseThrow(() -> AppException.notFound("Korisnik nije pronađen"));
    }

    private AuthResponse generateAuthResponse(User user) {
        String jwt = jwtUtil.generateToken(user);
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user);
        String refreshToken = refreshTokenEntity.getToken();
        Long expiresIn = jwtUtil.getExpirationInSeconds();

        return AuthResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(expiresIn)
                .emailVerified(true)
                .build();
    }

}