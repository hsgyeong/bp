package com.bp.jaringochi.domain.report.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.report.dto.MonthlyReport;

@Mapper
public interface ReportDao {

    // 해당 유저의 해당 월 레포트 1건 (없으면 null)
    MonthlyReport selectByUserAndMonth(@Param("userId") Long userId,
                                       @Param("year") int year,
                                       @Param("month") int month);

    // 메모리(연속성): 대상월 이전 최근 limit개 레포트 (최신순). 과거 다짐/추세 참고용
    List<MonthlyReport> selectRecentReports(@Param("userId") Long userId,
                                            @Param("year") int year,
                                            @Param("month") int month,
                                            @Param("limit") int limit);

    // 레포트 생성 (id 자동 채움)
    int insert(MonthlyReport report);

    // 굴비 한 마디 저장 (월 1회)
    int updateReply(@Param("id") Long id,
                    @Param("userMessage") String userMessage,
                    @Param("gulbiReply") String gulbiReply,
                    @Param("repliedAt") LocalDateTime repliedAt);
}
