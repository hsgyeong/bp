package com.bp.jaringochi.domain.user.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.user.dto.RefreshToken;

@Mapper
public interface RefreshTokenDao {

    int insertRefreshToken(RefreshToken refreshToken);

    RefreshToken findValidByTokenHash(String tokenHash);

    int revokeByTokenHash(String tokenHash);

    int revokeAllByUserId(@Param("userId") Long userId);
}