package com.bp.jaringochi.domain.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;
import com.bp.jaringochi.domain.budget.service.BudgetService;
import com.bp.jaringochi.domain.report.dao.ReportDao;
import com.bp.jaringochi.domain.report.dto.CategoryDiffItem;
import com.bp.jaringochi.domain.report.dto.MonthlyReport;
import com.bp.jaringochi.domain.report.dto.ReportNarrative;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatItem;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatistics;
import com.bp.jaringochi.domain.statistics.service.StatisticsService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportDao reportDao;
    private final StatisticsService statisticsService;
    private final BudgetService budgetService;
    private final ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int EXPENSE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final String PERSONA =
            "너는 '자린고비 가계부' 앱의 마스코트 '굴비'야. 절약왕 자린고비처럼 짠돌이지만 사용자를 진심으로 "
          + "아끼는 다정하고 구수한 말투로 말해. 잔소리는 짧고 따뜻하게, 칭찬은 아낌없이. 반말로 친근하게. "
          + "금액 숫자는 주어진 데이터에 있는 값만 쓰고 절대 지어내지 마. 한국어로만 답해.";

    // ==================================================================
    // 1. 레포트 조회 (없으면 생성·저장)
    // ==================================================================
    @Override
    @Transactional
    public MonthlyReport getMonthly(Long userId, int year, int month) {
        validateMonth(year, month);

        MonthlyReport existing = reportDao.selectByUserAndMonth(userId, year, month);
        if (existing != null) {
            existing.setCategories(parseCategories(existing.getCategoryJson()));
            return existing;                          // 캐싱: 재생성 안 함
        }

        MonthlyReport report = buildReport(userId, year, month);
        reportDao.insert(report);                     // id 채워짐
        return report;
    }

    // ==================================================================
    // 2. 굴비에게 한 마디 (월 1회)
    // ==================================================================
    @Override
    @Transactional
    public MonthlyReport talk(Long userId, int year, int month, String message) {
        if (!StringUtils.hasText(message)) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_INPUT);
        }
        String trimmed = message.strip();
        if (trimmed.length() > 200) {
            trimmed = trimmed.substring(0, 200);
        }

        MonthlyReport report = reportDao.selectByUserAndMonth(userId, year, month);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        if (StringUtils.hasText(report.getGulbiReply())) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_REPLIED);
        }

        String reply = generateReply(userId, report, trimmed);
        if (!StringUtils.hasText(reply)) {
            // AI 실패 시 1회 기회를 소모하지 않도록 저장하지 않고 에러 — 재시도 가능
            throw new BusinessException(ErrorCode.REPORT_AI_FAILED);
        }

        LocalDateTime now = LocalDateTime.now();
        reportDao.updateReply(report.getId(), trimmed, reply, now);

        report.setUserMessage(trimmed);
        report.setGulbiReply(reply);
        report.setRepliedAt(now);
        report.setCategories(parseCategories(report.getCategoryJson()));
        return report;
    }

    // ==================================================================
    // 레포트 생성 — 통계/예산 재사용해 숫자 채우고 AI 텍스트 입힘
    // ==================================================================
    private MonthlyReport buildReport(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        YearMonth pym = ym.minusMonths(1);

        CategoryStatistics cur = statisticsService.getByCategory(userId, start, end, EXPENSE);
        CategoryStatistics prev = statisticsService.getByCategory(
                userId, pym.atDay(1), pym.atEndOfMonth(), EXPENSE);

        BigDecimal totalExpense = nz(cur.getTotal());
        BigDecimal prevExpense = nz(prev.getTotal());

        List<CategoryStatItem> curItems = cur.getItems() == null ? List.of() : cur.getItems();
        List<CategoryStatItem> prevItems = prev.getItems() == null ? List.of() : prev.getItems();

        // 카테고리별 전월 대비
        List<CategoryDiffItem> diffs = new ArrayList<>();
        for (CategoryStatItem c : curItems) {
            CategoryDiffItem d = new CategoryDiffItem();
            d.setCategoryId(c.getCategoryId());
            d.setCategoryName(c.getCategoryName());
            d.setAmount(c.getAmount());
            d.setRatio(c.getRatio());
            BigDecimal prevAmt = findPrevAmount(prevItems, c.getCategoryId());
            d.setDiffAmount(nz(c.getAmount()).subtract(prevAmt));
            diffs.add(d);
        }

        MonthlyReport report = new MonthlyReport();
        report.setUserId(userId);
        report.setReportYear(year);
        report.setReportMonth(month);
        report.setTotalExpense(totalExpense);
        report.setPrevExpense(prevExpense);
        report.setDiffRatio(calcDiffRatio(totalExpense, prevExpense));
        report.setTopCategory(curItems.isEmpty() ? null : curItems.get(0).getCategoryName());
        report.setCategoryJson(writeCategories(diffs));
        report.setCategories(diffs);
        applyWeekStats(report, userId);
        report.setGeneratedAt(LocalDateTime.now());

        // 지난달 레포트(연속성) — 있으면 AI 프롬프트에 컨텍스트로 주입
        MonthlyReport last = reportDao.selectByUserAndMonth(userId, pym.getYear(), pym.getMonthValue());
        applyAiNarrative(report, diffs, last);

        return report;
    }

    // 주간 예산 성공 주 수 — 오늘이 속한 진행 중 주는 제외하고, 완료된 최근 3주 중 ratio<=100 (월 무관)
    private void applyWeekStats(MonthlyReport report, Long userId) {
        LocalDate today = LocalDate.now();
        List<WeeklyBudget> completed = new ArrayList<>();
        for (WeeklyBudget w : budgetService.getRecentWeeks(userId)) {   // 과거→현재, 최대 4주(현재 주 포함 가능)
            if (w.getEndDate() != null && w.getEndDate().isBefore(today)) {
                completed.add(w);                                       // 끝난 주만
            }
        }
        int from = Math.max(0, completed.size() - 3);                  // 가장 최근 3주만
        int total = 0, success = 0;
        for (WeeklyBudget w : completed.subList(from, completed.size())) {
            total++;
            if (w.getRatio() != null && w.getRatio().compareTo(HUNDRED) <= 0) {
                success++;
            }
        }
        report.setSuccessWeeks(success);
        report.setTotalWeeks(total);
    }

    // ==================================================================
    // AI 호출 ① 레포트 내러티브 (구조화 출력)
    // ==================================================================
    private void applyAiNarrative(MonthlyReport r, List<CategoryDiffItem> diffs, MonthlyReport last) {
        String system = PERSONA
                + " 사용자의 한 달 지출 데이터를 보고 짧고 따뜻하게 정리해줘. "
                + "mood 는 hello, warn, happy, sad, hungry, sulk, angry 중 하나만 골라"
                + "(지출을 잘 관리했으면 happy, 과소비면 warn/sulk/angry). "
                + "oneLiner 는 40자 이내 한 줄 총평, categoryComment 는 전월보다 늘어난 카테고리 짚고 절약 포인트(80자 이내), "
                + "advice 는 다음 달 구체적 절약 조언(100자 이내).";

        try {
            ReportNarrative n = chatClient.prompt()
                    .system(system)
                    .user(buildReportPrompt(r, diffs, last))
                    .call()
                    .entity(ReportNarrative.class);

            if (n != null) {
                r.setOneLiner(StringUtils.hasText(n.getOneLiner()) ? n.getOneLiner() : fallbackOneLiner(r));
                r.setMood(normalizeMood(n.getMood(), r));
                r.setCategoryComment(n.getCategoryComment());
                r.setAdvice(n.getAdvice());
                return;
            }
        } catch (Exception e) {
            // 키 오류·네트워크·파싱 등 모든 실패는 폴백으로 흡수 (앱이 죽지 않게)
            log.warn("[report] AI 내러티브 생성 실패 → 폴백: {}", e.toString(), e);
        }
        applyFallbackNarrative(r);
    }

    // ==================================================================
    // AI 호출 ② 굴비 한 마디 (평문)
    // ==================================================================
    private String generateReply(Long userId, MonthlyReport r, String userMessage) {
        String system = PERSONA
                + " 사용자가 이번 달 가계부 레포트를 보고 너에게 한 마디 건넸어. "
                + "굴비답게 짧고 다정하게 80자 이내로 한 번만 답해줘.";

        StringBuilder user = new StringBuilder();
        user.append("[이번 달 요약] ").append(reportSummaryLine(r));

        // 지난달 대화(연속성) — 있으면 기억하듯 이어서
        YearMonth pym = YearMonth.of(r.getReportYear(), r.getReportMonth()).minusMonths(1);
        MonthlyReport last = reportDao.selectByUserAndMonth(userId, pym.getYear(), pym.getMonthValue());
        if (last != null && StringUtils.hasText(last.getUserMessage())) {
            user.append("\n[지난달 사용자] ").append(last.getUserMessage());
            if (StringUtils.hasText(last.getGulbiReply())) {
                user.append("\n[지난달 굴비] ").append(last.getGulbiReply());
            }
        }
        user.append("\n\n[이번 달 사용자의 한 마디] ").append(userMessage);

        try {
            return chatClient.prompt()
                    .system(system)
                    .user(user.toString())
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[report] 굴비 한마디 생성 실패: {}", e.toString(), e);
            return null;   // talk() 에서 R503 으로 분기
        }
    }

    // ==================================================================
    // 프롬프트 구성
    // ==================================================================
    private String buildReportPrompt(MonthlyReport r, List<CategoryDiffItem> diffs, MonthlyReport last) {
        StringBuilder sb = new StringBuilder();
        sb.append("[이번 달 ").append(r.getReportYear()).append("-").append(r.getReportMonth()).append(" 지출 요약]\n");
        sb.append("- 총 지출: ").append(won(r.getTotalExpense())).append("원\n");
        sb.append("- 전월 지출: ").append(won(r.getPrevExpense())).append("원");
        sb.append(r.getDiffRatio() != null ? " (전월대비 " + r.getDiffRatio() + "%)\n" : " (전월 데이터 없음)\n");
        sb.append("- 가장 많이 쓴 카테고리: ").append(r.getTopCategory() == null ? "없음" : r.getTopCategory()).append("\n");
        sb.append("- 주간 예산 성공: ").append(nz0(r.getSuccessWeeks())).append("주 / ")
          .append(nz0(r.getTotalWeeks())).append("주\n");
        sb.append("- 카테고리별(전월대비):\n");
        for (CategoryDiffItem d : diffs) {
            sb.append("  · ").append(d.getCategoryName())
              .append(" ").append(won(d.getAmount())).append("원");
            if (d.getRatio() != null) sb.append("(").append(d.getRatio()).append("%)");
            if (d.getDiffAmount() != null) {
                sb.append(" 전월대비 ").append(d.getDiffAmount().signum() >= 0 ? "+" : "")
                  .append(won(d.getDiffAmount())).append("원");
            }
            sb.append("\n");
        }
        if (last != null) {
            sb.append("[지난달 참고] 총 지출 ").append(won(last.getTotalExpense())).append("원");
            if (StringUtils.hasText(last.getOneLiner())) {
                sb.append(", 굴비 한줄평 \"").append(last.getOneLiner()).append("\"");
            }
            sb.append(" — 이번 달과 비교해 한마디 해줘.\n");
        }
        return sb.toString();
    }

    private String reportSummaryLine(MonthlyReport r) {
        String diff = r.getDiffRatio() != null ? "전월대비 " + r.getDiffRatio() + "%" : "전월 데이터 없음";
        return "총 지출 " + won(r.getTotalExpense()) + "원, " + diff
             + ", 가장 많이 쓴 카테고리 " + (r.getTopCategory() == null ? "없음" : r.getTopCategory());
    }

    // ==================================================================
    // 계산·폴백·유틸
    // ==================================================================
    private BigDecimal calcDiffRatio(BigDecimal total, BigDecimal prev) {
        if (prev == null || prev.signum() == 0) {
            return null;                                  // 전월 0 → 증감률 산정 불가
        }
        return total.subtract(prev)
                    .multiply(HUNDRED)
                    .divide(prev, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal findPrevAmount(List<CategoryStatItem> prevItems, Long categoryId) {
        if (categoryId == null) return BigDecimal.ZERO;
        for (CategoryStatItem p : prevItems) {
            if (categoryId.equals(p.getCategoryId())) {
                return nz(p.getAmount());
            }
        }
        return BigDecimal.ZERO;
    }

    private void applyFallbackNarrative(MonthlyReport r) {
        r.setOneLiner(fallbackOneLiner(r));
        r.setMood(fallbackMood(r));
        r.setCategoryComment(r.getTopCategory() == null
                ? "이번 달은 지출 기록이 적네. 다음 달도 같이 살펴보자."
                : "이번 달은 '" + r.getTopCategory() + "'에 가장 많이 썼어. 거기부터 조금씩 줄여보자.");
        r.setAdvice("다음 달엔 주간 예산을 미리 정해두고 큰 지출은 하루 미뤄 생각해보자.");
    }

    private String fallbackOneLiner(MonthlyReport r) {
        if (r.getDiffRatio() == null) return "이번 달도 굴비랑 같이 차근차근 살펴보자.";
        return r.getDiffRatio().signum() <= 0
                ? "지난달보다 아꼈네, 잘했어!"
                : "지난달보다 조금 더 썼어. 다음 달엔 같이 줄여보자.";
    }

    // mood 가 7종 밖이거나 비었으면 숫자 기반 기본값으로 교정
    private String normalizeMood(String mood, MonthlyReport r) {
        if (mood != null) {
            String m = mood.trim().toLowerCase();
            switch (m) {
                case "hello": case "warn": case "happy":
                case "sad": case "hungry": case "sulk": case "angry":
                    return m;
            }
        }
        return fallbackMood(r);
    }

    private String fallbackMood(MonthlyReport r) {
        if (r.getDiffRatio() == null) return "hello";
        return r.getDiffRatio().signum() <= 0 ? "happy" : "warn";
    }

    private List<CategoryDiffItem> parseCategories(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<CategoryDiffItem>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeCategories(List<CategoryDiffItem> diffs) {
        try {
            return objectMapper.writeValueAsString(diffs);
        } catch (Exception e) {
            return "[]";
        }
    }

    // 생성 가능 월: 완료된 달(지난달)까지만. 이번 달·미래 달은 차단
    private void validateMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_INPUT);
        }
        YearMonth target = YearMonth.of(year, month);
        if (!target.isBefore(YearMonth.now())) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_INPUT);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int nz0(Integer v) {
        return v == null ? 0 : v;
    }

    private String won(BigDecimal v) {
        return new DecimalFormat("#,##0").format(nz(v));
    }
}
