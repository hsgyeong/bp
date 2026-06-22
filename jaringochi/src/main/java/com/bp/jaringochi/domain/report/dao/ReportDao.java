package com.bp.jaringochi.domain.report.dao;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.report.dto.MonthlyReport;

@Mapper
public interface ReportDao {

    // 해당 유저의 해당 월 레포트 1건 (없으면 null)
    MonthlyReport selectByUserAndMonth(@Param("userId") Long userId,
                                       @Param("year") int year,
                                       @Param("month") int month);

    // 레포트 생성 (id 자동 채움)
    int insert(MonthlyReport report);

    // 굴비 한 마디 저장 (월 1회)
    int updateReply(@Param("id") Long id,
                    @Param("userMessage") String userMessage,
                    @Param("gulbiReply") String gulbiReply,
                    @Param("repliedAt") LocalDateTime repliedAt);
}
