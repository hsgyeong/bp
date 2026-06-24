package com.bp.jaringochi.domain.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;
import com.bp.jaringochi.domain.budget.service.BudgetService;
import com.bp.jaringochi.domain.report.dao.ReportDao;
import com.bp.jaringochi.domain.report.dto.CategoryDiffItem;
import com.bp.jaringochi.domain.report.dto.MonthlyReport;
import com.bp.jaringochi.domain.report.dto.ReportExtra;
import com.bp.jaringochi.domain.report.dto.ReportNarrative;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatItem;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatistics;
import com.bp.jaringochi.domain.statistics.dto.DailyExpense;
import com.bp.jaringochi.domain.statistics.service.StatisticsService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

    // LocalDate(JavaTime) 직렬화 위해 모듈 등록 — extra_json 의 biggestDay.date
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final int EXPENSE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MEMORY_MONTHS = 12;   // 메모리(연속성) 참조 개월 수

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
            existing.setExtra(parseExtra(existing.getExtraJson()));
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
        report.setExtra(parseExtra(report.getExtraJson()));
        return report;
    }

    // ==================================================================
    // 레포트 생성 — 통계/예산 재사용해 숫자 채우고 mood 결정 후 AI 텍스트 입힘
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

        // 전월/당월 합집합 — 전월에만 있던 카테고리도 포함(양쪽 도넛 100% 정확)
        List<CategoryDiffItem> diffs = buildDiffs(curItems, prevItems);

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

        // 주차별(완료된 주) — successWeeks/totalWeeks + extra.weeks 공용
        List<WeeklyBudget> weeks = completedWeeks(budgetService.getWeeksByMonth(userId, start, end));
        applyWeekStats(report, weeks);

        // 부가 지표 스냅샷
        ReportExtra extra = buildExtra(userId, start, end, totalExpense, diffs, weeks);
        report.setExtra(extra);
        report.setExtraJson(writeExtra(extra));

        report.setGeneratedAt(LocalDateTime.now());

        // mood 는 코드가 결정(AI 호출 전) → 표정 4종 보장 + AI 가 이 mood 톤으로 글 작성
        report.setMood(computeMood(report));

        // 메모리(연속성) — 지난 최대 12개월 레포트를 AI 프롬프트에 요약 주입
        List<MonthlyReport> history = reportDao.selectRecentReports(userId, year, month, MEMORY_MONTHS);
        applyAiNarrative(report, diffs, history);

        return report;
    }

    // ===== 전월/당월 합집합 카테고리 (categoryId 기준, '기타'는 이름으로 매칭) =====
    private List<CategoryDiffItem> buildDiffs(List<CategoryStatItem> curItems, List<CategoryStatItem> prevItems) {
        List<CategoryDiffItem> diffs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (CategoryStatItem c : curItems) {
            CategoryStatItem p = findItem(prevItems, c);
            CategoryDiffItem d = new CategoryDiffItem();
            d.setCategoryId(c.getCategoryId());
            d.setCategoryName(c.getCategoryName());
            d.setAmount(nz(c.getAmount()));
            d.setRatio(nz(c.getRatio()));
            d.setPrevAmount(p == null ? BigDecimal.ZERO : nz(p.getAmount()));
            d.setPrevRatio(p == null ? BigDecimal.ZERO : nz(p.getRatio()));
            d.setDiffAmount(d.getAmount().subtract(d.getPrevAmount()));
            diffs.add(d);
            seen.add(keyOf(c));
        }
        // 전월에만 있던 카테고리(당월 0)
        for (CategoryStatItem p : prevItems) {
            if (seen.contains(keyOf(p))) continue;
            CategoryDiffItem d = new CategoryDiffItem();
            d.setCategoryId(p.getCategoryId());
            d.setCategoryName(p.getCategoryName());
            d.setAmount(BigDecimal.ZERO);
            d.setRatio(BigDecimal.ZERO);
            d.setPrevAmount(nz(p.getAmount()));
            d.setPrevRatio(nz(p.getRatio()));
            d.setDiffAmount(nz(p.getAmount()).negate());
            diffs.add(d);
        }
        return diffs;
    }

    private String keyOf(CategoryStatItem c) {
        return c.getCategoryId() != null ? "id:" + c.getCategoryId() : "name:" + c.getCategoryName();
    }

    private CategoryStatItem findItem(List<CategoryStatItem> items, CategoryStatItem target) {
        String k = keyOf(target);
        for (CategoryStatItem p : items) {
            if (keyOf(p).equals(k)) return p;
        }
        return null;
    }

    // 그 달에 걸친 주들 중 '완료된'(오늘 이전 종료) 주만 (진행 중 주는 제외)
    private List<WeeklyBudget> completedWeeks(List<WeeklyBudget> weeks) {
        LocalDate today = LocalDate.now();
        List<WeeklyBudget> done = new ArrayList<>();
        for (WeeklyBudget w : weeks) {
            if (w.getEndDate() != null && w.getEndDate().isBefore(today)) {
                done.add(w);
            }
        }
        return done;
    }

    // 주간 예산 성공/전체 주 수 (그 달 완료된 주 기준) — mood 결정의 입력
    private void applyWeekStats(MonthlyReport report, List<WeeklyBudget> weeks) {
        int total = 0, success = 0;
        for (WeeklyBudget w : weeks) {
            total++;
            if (w.getRatio() != null && w.getRatio().compareTo(HUNDRED) <= 0) {
                success++;
            }
        }
        report.setSuccessWeeks(success);
        report.setTotalWeeks(total);
    }

    // ===== 부가 지표 스냅샷 =====
    private ReportExtra buildExtra(Long userId, LocalDate start, LocalDate end,
                                   BigDecimal totalExpense, List<CategoryDiffItem> diffs,
                                   List<WeeklyBudget> weeks) {
        ReportExtra extra = new ReportExtra();

        int daysInMonth = end.getDayOfMonth();   // 그 달 전체 일수(말일)
        // 하루 평균만 나눗셈 → 원 단위 반올림(실제 거래액은 모두 원본 유지)
        extra.setDailyAvg(nz(totalExpense).divide(BigDecimal.valueOf(daysInMonth), 0, RoundingMode.HALF_UP));

        List<DailyExpense> daily = statisticsService.getDailyExpense(userId, start, end);
        extra.setNoSpendDays(daysInMonth - daily.size());

        DailyExpense biggest = null;
        for (DailyExpense d : daily) {
            if (biggest == null || nz(d.getAmount()).compareTo(nz(biggest.getAmount())) > 0) {
                biggest = d;
            }
        }
        if (biggest != null) {
            extra.setBiggestDay(new ReportExtra.DayAmount(biggest.getDate(), biggest.getAmount()));
        }

        // 전월 대비 가장 아낀(음수 최소)/가장 늘어난(양수 최대) 카테고리
        CategoryDiffItem saved = null, spent = null;
        for (CategoryDiffItem d : diffs) {
            BigDecimal da = nz(d.getDiffAmount());
            if (da.signum() < 0 && (saved == null || da.compareTo(nz(saved.getDiffAmount())) < 0)) saved = d;
            if (da.signum() > 0 && (spent == null || da.compareTo(nz(spent.getDiffAmount())) > 0)) spent = d;
        }
        if (saved != null) extra.setSavedMost(new ReportExtra.CategoryDelta(saved.getCategoryName(), saved.getDiffAmount()));
        if (spent != null) extra.setSpentMost(new ReportExtra.CategoryDelta(spent.getCategoryName(), spent.getDiffAmount()));

        // 주차별 달성
        List<ReportExtra.WeekStat> ws = new ArrayList<>();
        int i = 1;
        for (WeeklyBudget w : weeks) {
            BigDecimal ratio = w.getRatio();
            boolean pass = ratio != null && ratio.compareTo(HUNDRED) <= 0;
            ws.add(new ReportExtra.WeekStat(i + "주차", ratio, pass));
            i++;
        }
        extra.setWeeks(ws);

        return extra;
    }

    // ===== mood 결정 (그 달 예산 초과 주 수 기반, 관대하게) =====
    private String computeMood(MonthlyReport r) {
        int total = nz0(r.getTotalWeeks());
        if (total > 0) {
            int failed = total - nz0(r.getSuccessWeeks());
            if (failed <= 1) return "happy";
            if (failed == 2) return "smirk";
            if (failed == 3) return "angry";
            return "sad";
        }
        // 폴백: 그 달 예산을 안 짠 경우 전월 대비로
        BigDecimal d = r.getDiffRatio();
        if (d == null || d.signum() <= 0) return "happy";
        if (d.compareTo(BigDecimal.TEN) <= 0) return "smirk";
        if (d.compareTo(BigDecimal.valueOf(25)) <= 0) return "angry";
        return "sad";
    }

    private String moodGuide(String mood) {
        return switch (mood == null ? "happy" : mood) {
            case "smirk" -> "smirk(얄밉게 콕 집어 살짝 비꼬는 츤데레)";
            case "angry" -> "angry(따끔하게 혼내지만 미워서가 아닌)";
            case "sad"   -> "sad(속상하고 안타까워하는)";
            default       -> "happy(흐뭇하고 기특해하는, 칭찬 위주)";
        };
    }

    // ==================================================================
    // AI 호출 ① 레포트 내러티브 (구조화 출력, mood 는 우리가 주입)
    // ==================================================================
    private void applyAiNarrative(MonthlyReport r, List<CategoryDiffItem> diffs, List<MonthlyReport> history) {
        String mood = r.getMood();
        String system = PERSONA
                + " 사용자의 한 달 지출 데이터를 보고 짧고 따뜻하게 정리해줘. "
                + "이번 달 굴비의 기분은 '" + mood + "' 야 — " + moodGuide(mood)
                + ". 이 기분에 맞춰 oneLiner/categoryComment/advice 의 말투와 감정을 일치시켜줘. "
                + "oneLiner 는 40자 이내 한 줄 총평, categoryComment 는 전월보다 늘어난 카테고리 짚고 절약 포인트(80자 이내), "
                + "advice 는 다음 달 구체적 절약 조언(100자 이내).";

        try {
            ReportNarrative n = chatClient.prompt()
                    .system(system)
                    .user(buildReportPrompt(r, diffs, history))
                    .call()
                    .entity(ReportNarrative.class);

            if (n != null) {
                r.setOneLiner(StringUtils.hasText(n.getOneLiner()) ? n.getOneLiner() : fallbackOneLiner(r));
                r.setCategoryComment(StringUtils.hasText(n.getCategoryComment())
                        ? n.getCategoryComment() : fallbackCategoryComment(r));
                r.setAdvice(StringUtils.hasText(n.getAdvice()) ? n.getAdvice() : FALLBACK_ADVICE);
                return;
            }
        } catch (Exception e) {
            // 키 오류·네트워크·파싱 등 모든 실패는 폴백으로 흡수 (앱이 죽지 않게)
            log.warn("[report] AI 내러티브 생성 실패 → 폴백: {}", e.toString(), e);
        }
        applyFallbackNarrative(r);
    }

    // ==================================================================
    // AI 호출 ② 굴비 한 마디 (평문, mood 톤 + 과거 대화 주입)
    // ==================================================================
    private String generateReply(Long userId, MonthlyReport r, String userMessage) {
        String mood = r.getMood();
        String system = PERSONA
                + " 사용자가 이번 달 가계부 레포트를 보고 너에게 한 마디 건넸어. "
                + "이번 달 너의 기분은 '" + mood + "'(" + moodGuide(mood) + ")야. 그 기분을 담아 "
                + "굴비답게 짧고 다정하게 80자 이내로 한 번만 답해줘.";

        StringBuilder user = new StringBuilder();
        user.append("[이번 달 요약] ").append(reportSummaryLine(r));

        // 과거 대화(연속성) — 최근 몇 개월 사용자↔굴비 대화를 기억하듯 이어서
        List<MonthlyReport> history = reportDao.selectRecentReports(
                userId, r.getReportYear(), r.getReportMonth(), MEMORY_MONTHS);
        appendTalkHistory(user, history);

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
    private String buildReportPrompt(MonthlyReport r, List<CategoryDiffItem> diffs, List<MonthlyReport> history) {
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
        appendHistory(sb, history);
        return sb.toString();
    }

    // 과거 레포트 요약(최신순): 최근 2개월은 풀, 그 이전은 한 줄로 압축
    private void appendHistory(StringBuilder sb, List<MonthlyReport> history) {
        if (history == null || history.isEmpty()) return;
        sb.append("[과거 레포트 — 기억해서 연속성 있게 짚어줘]\n");
        int i = 0;
        for (MonthlyReport h : history) {
            String ym = h.getReportYear() + "-" + h.getReportMonth();
            sb.append("- ").append(ym).append(" 총지출 ").append(won(h.getTotalExpense())).append("원");
            if (i < 2 && StringUtils.hasText(h.getOneLiner())) {
                sb.append(", 한줄평 \"").append(h.getOneLiner()).append("\"");
            }
            if (StringUtils.hasText(h.getUserMessage())) {
                sb.append(", 사용자 다짐 \"").append(h.getUserMessage()).append("\"");
            }
            sb.append("\n");
            i++;
        }
    }

    // 굴비 한 마디용 — 최근 3개월 사용자↔굴비 대화만
    private void appendTalkHistory(StringBuilder sb, List<MonthlyReport> history) {
        if (history == null) return;
        int shown = 0;
        for (MonthlyReport h : history) {
            if (!StringUtils.hasText(h.getUserMessage())) continue;
            String ym = h.getReportYear() + "-" + h.getReportMonth();
            sb.append("\n[").append(ym).append(" 사용자] ").append(h.getUserMessage());
            if (StringUtils.hasText(h.getGulbiReply())) {
                sb.append("\n[").append(ym).append(" 굴비] ").append(h.getGulbiReply());
            }
            if (++shown >= 3) break;
        }
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

    private static final String FALLBACK_ADVICE =
            "다음 달엔 주간 예산을 미리 정해두고 큰 지출은 하루 미뤄 생각해보자.";

    private void applyFallbackNarrative(MonthlyReport r) {
        r.setOneLiner(fallbackOneLiner(r));
        r.setCategoryComment(fallbackCategoryComment(r));
        r.setAdvice(FALLBACK_ADVICE);
        // mood 는 이미 computeMood 로 설정됨 — 폴백에서 건드리지 않음
    }

    private String fallbackOneLiner(MonthlyReport r) {
        if (r.getDiffRatio() == null) return "이번 달도 굴비랑 같이 차근차근 살펴보자.";
        return r.getDiffRatio().signum() <= 0
                ? "지난달보다 아꼈네, 잘했어!"
                : "지난달보다 조금 더 썼어. 다음 달엔 같이 줄여보자.";
    }

    private String fallbackCategoryComment(MonthlyReport r) {
        return r.getTopCategory() == null
                ? "이번 달은 지출 기록이 적네. 다음 달도 같이 살펴보자."
                : "이번 달은 '" + r.getTopCategory() + "'에 가장 많이 썼어. 거기부터 조금씩 줄여보자.";
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

    private ReportExtra parseExtra(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return objectMapper.readValue(json, ReportExtra.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String writeExtra(ReportExtra extra) {
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (Exception e) {
            return null;
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
