package com.bp.jaringochi.domain.report.dao;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.report.dto.MonthlyReport;

@Mapper
public interface ReportDao {

    // 유저 + 연/월 단건 조회 (캐싱: 있으면 재사용)
    MonthlyReport selectByUserAndMonth(@Param("userId") Long userId,
                                       @Param("year") Integer year,
                                       @Param("month") Integer month);

    // 생성 후 저장 (id 자동 채움)
    int insert(MonthlyReport report);

    // "굴비에게 한 마디" 응답 저장 (월 1회)
    int updateReply(@Param("id") Long id,
                    @Param("userMessage") String userMessage,
                    @Param("gulbiReply") String gulbiReply,
                    @Param("repliedAt") LocalDateTime repliedAt);
}
