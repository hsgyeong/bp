package com.bp.jaringochi.domain.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.user.dao.RefreshTokenDao;
import com.bp.jaringochi.domain.user.dto.RefreshToken;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenDao refreshTokenDao;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public String createRefreshToken(Long userId) {
    	// 클라이언트에게 줄 원본 refresh token
        String rawToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        
        // DB에는 원본이 아니라 해시값만 저장
        refreshToken.setTokenHash(hash(rawToken));
        
        // 현재 시각 + 설정된 만료 시간
        refreshToken.setExpiresAt(LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS));

        refreshTokenDao.insertRefreshToken(refreshToken);

        return rawToken;
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 클라이언트가 보낸 원본 refresh token을 같은 방식으로 해시한 뒤 폐기
        refreshTokenDao.revokeByTokenHash(hash(rawToken));
    }

    public RefreshToken findValidRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
        }

        RefreshToken refreshToken = refreshTokenDao.findValidByTokenHash(hash(rawToken));

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
        }

        return refreshToken;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            
            // byte 배열은 DB 저장이 불편하므로 Base64 문자열로 바꿔 저장
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("refresh token hash failed", e);
        }
    }
}