package com.bp.jaringochi.domain.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;
import com.bp.jaringochi.domain.budget.service.BudgetService;
import com.bp.jaringochi.domain.report.client.OpenAiClient;
import com.bp.jaringochi.domain.report.dao.ReportDao;
import com.bp.jaringochi.domain.report.dto.CategoryDiffItem;
import com.bp.jaringochi.domain.report.dto.MonthlyReport;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatItem;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatistics;
import com.bp.jaringochi.domain.statistics.service.StatisticsService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportDao reportDao;
    private final StatisticsService statisticsService;
    private final BudgetService budgetService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int EXPENSE = 2;
    private static final Set<String> MOODS =
            Set.of("hello", "warn", "happy", "sad", "hungry", "sulk", "angry");

    // 굴비 페르소나 — 짠돌이지만 사용자를 아끼는 다정한 말투
    private static final String PERSONA =
            "너는 '자린고비 가계부' 앱의 마스코트 '굴비'야. 굴비는 절약왕 자린고비의 상징(천장에 매달아 두고 보며 절약하던 굴비)이고, "
            + "짠돌이지만 사용자를 진심으로 아끼는 다정하고 구수한 말투로 말해. 잔소리는 짧고 따뜻하게, 칭찬은 아낌없이. "
            + "반말체로 친근하게. 금액 숫자는 주어진 데이터에 있는 값만 쓰고 절대 지어내지 마. 모든 답변은 한국어로.";

    // ==================================================================
    // 1) 월간 레포트 조회 / 생성
    // ==================================================================
    @Override
    @Transactional
    public MonthlyReport getOrCreate(Long userId, Integer year, Integer month) {
        validateMonth(year, month);

        MonthlyReport existing = reportDao.selectByUserAndMonth(userId, year, month);
        if (existing != null) {
            existing.setCategories(parseCategories(existing.getCategoryJson()));
            return existing;
        }

        MonthlyReport report = buildReport(userId, year, month);
        reportDao.insert(report);                 // id 채워짐
        report.setCategories(parseCategories(report.getCategoryJson()));
        return report;
    }

    // 통계 수집 + AI 생성 → 저장 직전 MonthlyReport 구성
    private MonthlyReport buildReport(Long userId, Integer year, Integer month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        YearMonth prevYm = ym.minusMonths(1);

        // 이번 달 / 전월 카테고리별 지출 (기존 통계 서비스 재사용)
        CategoryStatistics curr = statisticsService.getByCategory(userId, start, end, EXPENSE);
        CategoryStatistics prev = statisticsService.getByCategory(
                userId, prevYm.atDay(1), prevYm.atEndOfMonth(), EXPENSE);

        BigDecimal totalExpense = nz(curr.getTotal());
        BigDecimal prevExpense = nz(prev.getTotal());

        // 전월 같은 카테고리 금액 매핑 (이름 기준 — '기타'는 id가 null이라 이름 매칭)
        Map<String, BigDecimal> prevByName = new HashMap<>();
        for (CategoryStatItem it : safe(prev.getItems())) {
            prevByName.put(it.getCategoryName(), nz(it.getAmount()));
        }

        List<CategoryDiffItem> categories = new ArrayList<>();
        for (CategoryStatItem it : safe(curr.getItems())) {
            BigDecimal amt = nz(it.getAmount());
            BigDecimal prevAmt = prevByName.getOrDefault(it.getCategoryName(), BigDecimal.ZERO);
            CategoryDiffItem d = new CategoryDiffItem();
            d.setCategoryName(it.getCategoryName());
            d.setAmount(amt);
            d.setRatio(nz(it.getRatio()));
            d.setPrevAmount(prevAmt);
            d.setDiffAmount(amt.subtract(prevAmt));
            categories.add(d);
        }

        // 주간 예산 성공 주 수 (해당 월에 걸친 주만 — MVP: 최근 주 재사용)
        int[] weekStats = countBudgetWeeks(userId, start, end);

        MonthlyReport r = new MonthlyReport();
        r.setUserId(userId);
        r.setReportYear(year);
        r.setReportMonth(month);
        r.setTotalExpense(totalExpense);
        r.setPrevExpense(prevExpense);
        r.setDiffRatio(calcDiffRatio(totalExpense, prevExpense));
        r.setTotalWeeks(weekStats[0]);
        r.setSuccessWeeks(weekStats[1]);
        r.setTopCategory(categories.isEmpty() ? null : categories.get(0).getCategoryName());
        r.setCategoryJson(writeCategories(categories));

        applyAiNarrative(r, categories);
        return r;
    }

    // 해당 월에 걸친 주간예산 → [총 주 수, 성공 주 수(ratio<=100)]
    private int[] countBudgetWeeks(Long userId, LocalDate start, LocalDate end) {
        int total = 0;
        int success = 0;
        for (WeeklyBudget wb : safe(budgetService.getRecentWeeks(userId))) {
            // 주가 해당 월 범위와 겹치면 카운트
            if (wb.getStartDate() == null || wb.getEndDate() == null) continue;
            boolean overlaps = !wb.getStartDate().isAfter(end) && !wb.getEndDate().isBefore(start);
            if (!overlaps) continue;
            total++;
            BigDecimal ratio = wb.getRatio();
            if (ratio != null && ratio.compareTo(BigDecimal.valueOf(100)) <= 0) {
                success++;
            }
        }
        return new int[]{total, success};
    }

    // ==================================================================
    // 2) 굴비에게 한 마디 (월 1회)
    // ==================================================================
    @Override
    @Transactional
    public MonthlyReport talk(Long userId, Integer year, Integer month, String message) {
        validateMonth(year, month);
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

        String reply = generateReply(report, trimmed);
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

    private String generateReply(MonthlyReport r, String userMessage) {
        String system = PERSONA
                + " 사용자가 이번 달 가계부 레포트를 보고 너에게 한 마디 건넸어. "
                + "굴비답게 짧고 다정하게(80자 이내) 한 번만 답해줘.";
        String context = "[이번 달 요약] " + reportSummaryLine(r);
        String user = context + "\n\n[사용자의 한 마디] " + userMessage;
        return openAiClient.chat(system, user, false);
    }

    // ==================================================================
    // AI 내러티브 생성 (one_liner / mood / category_comment / advice)
    // ==================================================================
    private void applyAiNarrative(MonthlyReport r, List<CategoryDiffItem> categories) {
        String system = PERSONA + " 사용자의 한 달 지출 데이터를 보고 짧고 따뜻하게 정리해줘. "
                + "반드시 아래 JSON 형식으로만 답해(다른 텍스트 금지):\n"
                + "{\n"
                + "  \"one_liner\": \"한 줄 총평 (40자 이내, 굴비 말투)\",\n"
                + "  \"mood\": \"hello|warn|happy|sad|hungry|sulk|angry 중 하나 — 지출을 잘 관리했으면 happy, 과소비면 sulk/angry/warn\",\n"
                + "  \"category_comment\": \"가장 눈에 띄는(전월보다 늘어난) 카테고리 코멘트 + 절약 포인트 (80자 이내)\",\n"
                + "  \"advice\": \"다음 달을 위한 구체적 절약 조언 한두 개 (100자 이내)\"\n"
                + "}";
        String user = buildReportPrompt(r, categories);

        String raw = openAiClient.chat(system, user, true);
        JsonNode node = tryParse(raw);

        if (node != null) {
            r.setOneLiner(text(node, "one_liner", null));
            r.setMood(normalizeMood(text(node, "mood", null), r));
            r.setCategoryComment(text(node, "category_comment", null));
            r.setAdvice(text(node, "advice", null));
        }

        // 폴백: 키 미설정·파싱 실패 등 — 숫자만으로도 레포트가 보이게
        if (!StringUtils.hasText(r.getOneLiner())) {
            r.setOneLiner(fallbackOneLiner(r));
        }
        if (!StringUtils.hasText(r.getMood())) {
            r.setMood(fallbackMood(r));
        }
        if (!StringUtils.hasText(r.getCategoryComment())) {
            r.setCategoryComment(fallbackCategoryComment(r, categories));
        }
        if (!StringUtils.hasText(r.getAdvice())) {
            r.setAdvice("다음 달엔 가장 많이 쓴 항목부터 조금씩 줄여보자. 굴비가 응원할게!");
        }
    }

    private String buildReportPrompt(MonthlyReport r, List<CategoryDiffItem> categories) {
        StringBuilder sb = new StringBuilder();
        sb.append("아래는 사용자의 ").append(r.getReportYear()).append("년 ")
          .append(r.getReportMonth()).append("월 지출 요약이야.\n");
        sb.append("- 총 지출: ").append(won(r.getTotalExpense())).append("원\n");
        sb.append("- 전월 총 지출: ").append(won(r.getPrevExpense())).append("원\n");
        if (r.getDiffRatio() != null) {
            sb.append("- 전월 대비: ").append(r.getDiffRatio()).append("% (")
              .append(r.getDiffRatio().signum() >= 0 ? "증가" : "감소").append(")\n");
        } else {
            sb.append("- 전월 대비: 비교 불가(전월 데이터 없음)\n");
        }
        sb.append("- 주간 예산: 총 ").append(nz0(r.getTotalWeeks())).append("주 중 ")
          .append(nz0(r.getSuccessWeeks())).append("주 성공(예산 안 넘김)\n");
        sb.append("- 카테고리별(많이 쓴 순):\n");
        if (categories.isEmpty()) {
            sb.append("  (지출 내역 없음)\n");
        } else {
            for (CategoryDiffItem c : categories) {
                BigDecimal diff = nz(c.getDiffAmount());
                String sign = diff.signum() >= 0 ? "+" : "-";
                sb.append("  · ").append(c.getCategoryName()).append(": ")
                  .append(won(c.getAmount())).append("원 (")
                  .append(c.getRatio() == null ? "0" : c.getRatio().stripTrailingZeros().toPlainString())
                  .append("%), 전월대비 ").append(sign).append(won(diff.abs())).append("원\n");
            }
        }
        return sb.toString();
    }

    private String reportSummaryLine(MonthlyReport r) {
        return r.getReportMonth() + "월 총 지출 " + won(r.getTotalExpense()) + "원, "
                + "전월 대비 " + (r.getDiffRatio() == null ? "비교 불가" : r.getDiffRatio() + "%") + ", "
                + "가장 많이 쓴 곳: " + (r.getTopCategory() == null ? "없음" : r.getTopCategory()) + ".";
    }

    // ==================================================================
    // 폴백 텍스트
    // ==================================================================
    private String fallbackOneLiner(MonthlyReport r) {
        BigDecimal d = r.getDiffRatio();
        if (d == null) return r.getReportMonth() + "월 살림살이 정리해봤다. 이번 달도 고생했어!";
        if (d.signum() < 0) return "지난달보다 아꼈구나. 굴비도 흐뭇하다!";
        if (d.compareTo(BigDecimal.valueOf(10)) > 0) return "지난달보다 좀 더 썼네. 다음 달엔 같이 조여보자.";
        return r.getReportMonth() + "월도 무난했어. 이 페이스 유지하자!";
    }

    private String fallbackMood(MonthlyReport r) {
        BigDecimal d = r.getDiffRatio();
        if (d == null) return "hello";
        if (d.signum() < 0) return "happy";
        if (d.compareTo(BigDecimal.valueOf(20)) > 0) return "sulk";
        if (d.compareTo(BigDecimal.valueOf(5)) > 0) return "warn";
        return "happy";
    }

    private String fallbackCategoryComment(MonthlyReport r, List<CategoryDiffItem> categories) {
        if (categories.isEmpty()) return "이번 달은 지출 기록이 거의 없네. 기록부터 차근차근 해보자!";
        // 전월 대비 가장 많이 늘어난 카테고리
        CategoryDiffItem top = categories.get(0);
        for (CategoryDiffItem c : categories) {
            if (nz(c.getDiffAmount()).compareTo(nz(top.getDiffAmount())) > 0) top = c;
        }
        BigDecimal diff = nz(top.getDiffAmount());
        if (diff.signum() > 0) {
            return "'" + top.getCategoryName() + "'가 전월보다 " + won(diff) + "원 늘었어. 여기부터 줄여보면 어떨까?";
        }
        return "가장 많이 쓴 곳은 '" + r.getTopCategory() + "'야. 잘 들여다보자.";
    }

    // ==================================================================
    // 공통 헬퍼
    // ==================================================================
    private void validateMonth(Integer year, Integer month) {
        if (year == null || month == null || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_INPUT);
        }
        YearMonth target = YearMonth.of(year, month);
        if (target.isAfter(YearMonth.now())) {     // 미래 월 금지
            throw new BusinessException(ErrorCode.REPORT_INVALID_INPUT);
        }
    }

    // (last - prev)/prev × 100, prev=0이면 null (StatisticsServiceImpl과 동일 규칙)
    private BigDecimal calcDiffRatio(BigDecimal curr, BigDecimal prev) {
        if (prev == null || prev.signum() == 0) return null;
        return curr.subtract(prev)
                   .multiply(BigDecimal.valueOf(100))
                   .divide(prev, 2, RoundingMode.HALF_UP);
    }

    private String normalizeMood(String mood, MonthlyReport r) {
        if (mood == null) return null;
        String m = mood.trim().toLowerCase();
        return MOODS.contains(m) ? m : fallbackMood(r);
    }

    private String writeCategories(List<CategoryDiffItem> categories) {
        try {
            return objectMapper.writeValueAsString(categories);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<CategoryDiffItem> parseCategories(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<CategoryDiffItem>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private JsonNode tryParse(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String text(JsonNode node, String field, String def) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? def : v.asText();
    }

    private String won(BigDecimal v) {
        return String.format("%,d", nz(v).longValue());
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private int nz0(Integer v) {
        return v == null ? 0 : v;
    }

    private <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
