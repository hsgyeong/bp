// ============================================================
// 자린고비 가계부 — Figma 목업 생성기
// 실행하면 캔버스에 15개 화면을 네이티브 레이어로 생성합니다.
// (HTML 변환이 아니라 Figma 오토레이아웃/도형으로 직접 그림 → 자유 편집 가능)
// ============================================================

// ---------- 컬러 토큰 ----------
const hex = (h) => ({
  r: parseInt(h.slice(1, 3), 16) / 255,
  g: parseInt(h.slice(3, 5), 16) / 255,
  b: parseInt(h.slice(5, 7), 16) / 255,
});
const C = {
  gold: hex("#F2A33C"),
  goldDeep: hex("#E0871A"),
  goldSoft: hex("#FCE8C8"),
  cream: hex("#FBF5EA"),
  cream2: hex("#F4ECDC"),
  card: hex("#FFFFFF"),
  ink: hex("#2E2820"),
  ink2: hex("#6F6557"),
  mute: hex("#A89C89"),
  line: hex("#ECE3D2"),
  income: hex("#2FA98C"),
  incomeSoft: hex("#DBF1EA"),
  expense: hex("#E8623D"),
  expenseSoft: hex("#FCE2DA"),
  blue: hex("#5B8DEF"),
  gray: hex("#B0A595"),
  bezel: hex("#1F1B14"),
};
const solid = (c, opacity) => ({ type: "SOLID", color: c, opacity: opacity == null ? 1 : opacity });

// ---------- 폰트 (설치된 것 중 우선순위로 선택) ----------
let F = { reg: { family: "Inter", style: "Regular" }, bold: { family: "Inter", style: "Bold" } };
async function loadFonts() {
  const candidates = [
    { family: "Pretendard", reg: "Regular", bold: "Bold" },
    { family: "Noto Sans KR", reg: "Regular", bold: "Bold" },
    { family: "Apple SD Gothic Neo", reg: "Regular", bold: "Bold" },
    { family: "Malgun Gothic", reg: "Regular", bold: "Bold" },
    { family: "Spoqa Han Sans Neo", reg: "Regular", bold: "Bold" },
    { family: "Inter", reg: "Regular", bold: "Bold" },
  ];
  for (const c of candidates) {
    try {
      await figma.loadFontAsync({ family: c.family, style: c.reg });
      await figma.loadFontAsync({ family: c.family, style: c.bold });
      F = { reg: { family: c.family, style: c.reg }, bold: { family: c.family, style: c.bold } };
      return c.family;
    } catch (e) { /* 다음 후보 */ }
  }
  return "Inter";
}

// ---------- 헬퍼 ----------
function T(chars, o = {}) {
  const t = figma.createText();
  t.fontName = o.w === "bold" ? F.bold : F.reg;
  t.fontSize = o.size || 14;
  t.characters = String(chars);
  t.fills = [solid(o.color || C.ink, o.op)];
  if (o.align) t.textAlignHorizontal = o.align;
  if (o.lh) t.lineHeight = { value: o.lh, unit: "PERCENT" };
  if (o.ls != null) t.letterSpacing = { value: o.ls, unit: "PERCENT" };
  if (o.width) { t.textAutoResize = "HEIGHT"; t.resize(o.width, t.height); }
  return t;
}

// 오토레이아웃 프레임
function AF(name, dir, o = {}) {
  const f = figma.createFrame();
  f.name = name;
  f.layoutMode = dir; // "VERTICAL" | "HORIZONTAL" | "NONE"
  f.fills = o.fill ? [solid(o.fill, o.fillOp)] : [];
  if (dir !== "NONE") {
    f.primaryAxisSizingMode = o.primary || "AUTO";
    f.counterAxisSizingMode = o.counter || "AUTO";
    f.itemSpacing = o.gap || 0;
    if (o.primaryAlign) f.primaryAxisAlignItems = o.primaryAlign;
    if (o.counterAlign) f.counterAxisAlignItems = o.counterAlign;
    const p = o.pad;
    if (typeof p === "number") { f.paddingTop = f.paddingBottom = f.paddingLeft = f.paddingRight = p; }
    else if (Array.isArray(p)) { f.paddingTop = p[0]; f.paddingRight = p[1]; f.paddingBottom = p[2]; f.paddingLeft = p[3]; }
  }
  if (o.radius != null) f.cornerRadius = o.radius;
  if (o.stroke) { f.strokes = [solid(o.stroke)]; f.strokeWeight = o.strokeW || 1; }
  if (o.clip != null) f.clipsContent = o.clip;
  return f;
}
function add(parent, ...kids) { kids.forEach((k) => k && parent.appendChild(k)); return parent; }
function stretch(n) { n.layoutAlign = "STRETCH"; return n; }
function grow(n) { n.layoutGrow = 1; return n; }
function sized(n, w, h) { n.resize(w, h); return n; }

// 가로 정렬(좌우 끝 배치)
function rowBetween(o = {}) {
  const r = AF(o.name || "row", "HORIZONTAL", { counterAlign: "CENTER", primaryAlign: "SPACE_BETWEEN", primary: "FIXED" });
  r.layoutAlign = "STRETCH";
  return r;
}
function rowL(gap, o = {}) {
  return AF(o.name || "row", "HORIZONTAL", { counterAlign: o.counterAlign || "CENTER", gap: gap || 0 });
}

// 카드
function card(o = {}) {
  const c = AF(o.name || "card", "VERTICAL", {
    fill: C.card, radius: o.radius || 22, gap: o.gap || 0,
    pad: o.pad != null ? o.pad : 18, stroke: C.line, strokeW: 1, clip: true,
    counterAlign: o.counterAlign,
  });
  stretch(c);
  return c;
}

// 동그란 아이콘 칩(이모지)
function iconChip(emoji, bg, size) {
  size = size || 38;
  const f = AF("ic", "HORIZONTAL", { fill: bg, radius: 12, primaryAlign: "CENTER", counterAlign: "CENTER", primary: "FIXED", counter: "FIXED" });
  sized(f, size, size);
  add(f, T(emoji, { size: size * 0.46 }));
  return f;
}

// 굴비 마스코트 (간단 도형 버전)
function gulbi(size) {
  const f = AF("gulbi", "NONE", {});
  sized(f, size, size);
  f.clipsContent = false;
  const body = figma.createEllipse();
  body.resize(size * 0.78, size * 0.52);
  body.x = size * 0.16; body.y = size * 0.26;
  body.fills = [solid(C.gold)]; body.strokes = [solid(C.goldDeep)]; body.strokeWeight = Math.max(1, size * 0.02);
  const tail = figma.createPolygon();
  tail.pointCount = 3;
  tail.resize(size * 0.22, size * 0.3);
  tail.rotation = 90;
  tail.x = size * 0.18; tail.y = size * 0.34;
  tail.fills = [solid(C.goldDeep)];
  const fin = figma.createPolygon();
  fin.pointCount = 3;
  fin.resize(size * 0.22, size * 0.16);
  fin.x = size * 0.5; fin.y = size * 0.18;
  fin.fills = [solid(C.gold)];
  const eyeW = figma.createEllipse();
  eyeW.resize(size * 0.12, size * 0.12);
  eyeW.x = size * 0.68; eyeW.y = size * 0.36;
  eyeW.fills = [solid(C.card)]; eyeW.strokes = [solid(C.goldDeep)]; eyeW.strokeWeight = 1;
  const pupil = figma.createEllipse();
  pupil.resize(size * 0.055, size * 0.055);
  pupil.x = size * 0.715; pupil.y = size * 0.4;
  pupil.fills = [solid(C.ink)];
  add(f, tail, fin, body, eyeW, pupil);
  return f;
}

// 세그먼트 탭
function seg(labels, active, o = {}) {
  const wrap = AF("seg", "HORIZONTAL", { fill: o.flat ? null : C.cream2, radius: 14, pad: 4, gap: 4, primary: "FIXED" });
  stretch(wrap);
  labels.forEach((lb, i) => {
    const on = i === active;
    const accent = o.accent || C.goldDeep;
    const s = AF("s", "HORIZONTAL", { fill: on ? C.card : (o.flat ? C.cream2 : null), radius: 11, pad: [9, 0, 9, 0], primaryAlign: "CENTER", counterAlign: "CENTER", primary: "FIXED" });
    grow(s);
    add(s, T(lb, { size: 13, w: "bold", color: on ? accent : C.ink2 }));
    add(wrap, s);
  });
  return wrap;
}

// 메뉴 행
function menuRow(emoji, label, right, last) {
  const r = AF("menu", "HORIZONTAL", { counterAlign: "CENTER", gap: 13, pad: [15, 12, 15, 12], primary: "FIXED" });
  stretch(r);
  if (!last) { r.strokes = [solid(C.line)]; r.strokeWeight = 1; r.strokeAlign = "INSIDE"; }
  const ico = T(emoji, { size: 19 }); ico.textAlignHorizontal = "CENTER";
  add(r, ico, grow(T(label, { size: 15, w: "bold" })));
  if (right) add(r, T(right, { size: 18, color: C.mute, w: "bold" }));
  return r;
}

// 거래 행
function txnRow(emoji, bg, name, memo, amt, color, noTop) {
  const r = AF("txn", "HORIZONTAL", { counterAlign: "CENTER", gap: 12, pad: [12, 4, 12, 4], primary: "FIXED" });
  stretch(r);
  if (!noTop) { r.strokes = [solid(C.line)]; r.strokeWeight = 1; r.strokeAlign = "INSIDE"; }
  const info = AF("info", "VERTICAL", { gap: 2 });
  add(info, T(name, { size: 14, w: "bold" }), T(memo, { size: 12, color: C.mute }));
  add(r, iconChip(emoji, bg), grow(info), T(amt, { size: 15, w: "bold", color }));
  return r;
}

// 진행 바
function progress(pct) {
  const track = AF("track", "HORIZONTAL", { fill: C.cream2, radius: 10, primary: "FIXED", counter: "FIXED" });
  sized(track, 284, 14);
  stretch(track);
  const fill = figma.createRectangle();
  fill.resize(284 * pct, 14);
  fill.cornerRadius = 10;
  fill.fills = [solid(C.gold)];
  fill.layoutPositioning = "ABSOLUTE";
  add(track, fill);
  fill.x = 0; fill.y = 0;
  return track;
}

// 꺾은선 차트
function lineChart(points, color, w, h) {
  const f = AF("line", "NONE", {}); sized(f, w, h); f.clipsContent = false;
  let d = "";
  points.forEach((p, i) => { d += (i ? " L " : "M ") + p[0] + " " + p[1]; });
  try {
    const v = figma.createVector();
    v.vectorPaths = [{ windingRule: "NONE", data: d }];
    v.strokes = [solid(color)]; v.strokeWeight = 3; v.strokeCap = "ROUND"; v.strokeJoin = "ROUND"; v.fills = [];
    add(f, v); v.x = 0; v.y = 0;
  } catch (e) {}
  points.forEach((p) => {
    const c = figma.createEllipse(); c.resize(8, 8); c.fills = [solid(color)];
    add(f, c); c.x = p[0] - 4; c.y = p[1] - 4;
  });
  return f;
}

// 도넛 차트
function donut(segments, size) {
  const f = AF("donut", "NONE", {}); sized(f, size, size); f.clipsContent = false;
  let start = -Math.PI / 2;
  segments.forEach((s) => {
    const span = s.pct * Math.PI * 2;
    const e = figma.createEllipse();
    e.resize(size, size);
    e.fills = [solid(s.color)];
    try { e.arcData = { startingAngle: start, endingAngle: start + span, innerRadius: 0.62 }; } catch (err) {}
    add(f, e); e.x = 0; e.y = 0;
    start += span;
  });
  return f;
}

// 막대 차트 (예산 vs 지출)
function barChart(weeks, w, h) {
  const f = AF("bars", "NONE", {}); sized(f, w, h); f.clipsContent = false;
  const base = h - 24;
  const groupW = w / weeks.length;
  const bw = 18;
  weeks.forEach((wk, i) => {
    const cx = groupW * i + groupW / 2;
    const mk = (val, color, dx) => {
      const r = figma.createRectangle();
      const bh = base * val;
      r.resize(bw, bh); r.cornerRadius = 4; r.fills = [solid(color)];
      add(f, r); r.x = cx + dx; r.y = base - bh;
    };
    mk(wk.budget, C.goldSoft, -bw - 2);
    mk(wk.spend, C.goldDeep, 2);
    const lb = T(wk.label, { size: 11, w: "bold", color: C.mute, align: "CENTER" });
    add(f, lb); lb.x = cx - 12; lb.y = base + 6;
  });
  return f;
}

// 상태바
function statusbar() {
  const sb = rowBetween({ name: "statusbar" });
  sb.paddingLeft = 26; sb.paddingRight = 26; sb.paddingTop = 0; sb.paddingBottom = 0;
  sb.counterAxisSizingMode = "FIXED"; sb.resize(360, 46);
  add(sb, T("9:41", { size: 13, w: "bold" }), T("📶  🔋", { size: 12 }));
  return sb;
}

// 하단 탭바
function tabbar(active) {
  const tabs = [["🏠", "홈"], ["📒", "가계부"], ["📊", "통계"], ["⋯", "더보기"]];
  const bar = AF("tabbar", "HORIZONTAL", { fill: C.card, primary: "FIXED", counter: "FIXED", pad: [11, 0, 0, 0] });
  bar.resize(360, 76); stretch(bar);
  bar.strokes = [solid(C.line)]; bar.strokeWeight = 1; bar.strokeAlign = "INSIDE";
  tabs.forEach(([ic, lb], i) => {
    const t = AF("tab", "VERTICAL", { gap: 5, counterAlign: "CENTER", primaryAlign: "MIN" });
    grow(t);
    add(t, T(ic, { size: 20 }), T(lb, { size: 11, w: "bold", color: i === active ? C.goldDeep : C.mute }));
    add(bar, t);
  });
  return bar;
}

// 플로팅 + 버튼
function fab() {
  const f = AF("fab", "HORIZONTAL", { fill: C.gold, radius: 20, primaryAlign: "CENTER", counterAlign: "CENTER", primary: "FIXED", counter: "FIXED" });
  sized(f, 58, 58);
  const plus = T("+", { size: 30, color: C.card }); add(f, plus);
  return f;
}

// 상단 타이틀바
function topbar(title, opts = {}) {
  const tb = rowBetween({ name: "topbar" });
  tb.paddingLeft = 20; tb.paddingRight = 20; tb.paddingTop = 6; tb.paddingBottom = 14;
  if (opts.left || opts.right) {
    add(tb, T(opts.left || " ", { size: 18, color: C.ink2 }), T(title, { size: 18, w: "bold" }), T(opts.right || " ", { size: 14, color: opts.rightColor || C.mute, w: "bold" }));
  } else if (opts.sub) {
    const l = AF("l", "VERTICAL", { gap: 1 });
    add(l, T(opts.subText, { size: 13, color: C.mute, w: "bold" }), T(title, { size: 21, w: "bold" }));
    add(tb, l);
  } else {
    add(tb, T(title, { size: 21, w: "bold" }), opts.suffix ? T(opts.suffix, { size: 13, color: C.mute, w: "bold" }) : T(" ", { size: 13 }));
  }
  return tb;
}

// 버튼
function btnPrimary(label, color) {
  const b = AF("btn", "HORIZONTAL", { fill: color || C.gold, radius: 16, pad: 16, primaryAlign: "CENTER", counterAlign: "CENTER", primary: "FIXED" });
  stretch(b);
  add(b, T(label, { size: 16, w: "bold", color: C.card }));
  return b;
}
function btnGhost(label, color, border) {
  const b = AF("btn", "HORIZONTAL", { fill: C.card, radius: 16, pad: 15, primaryAlign: "CENTER", counterAlign: "CENTER", primary: "FIXED", stroke: border || C.line, strokeW: 1.5 });
  stretch(b);
  add(b, T(label, { size: 15, w: "bold", color: color || C.ink2 }));
  return b;
}
// 입력 필드
function field(placeholder, o = {}) {
  const f = AF("field", "HORIZONTAL", { fill: C.card, radius: 14, pad: [15, 16, 15, 16], stroke: C.line, strokeW: 1.5, primary: "FIXED", counterAlign: "CENTER", primaryAlign: o.between ? "SPACE_BETWEEN" : "MIN" });
  stretch(f);
  add(f, T(placeholder, { size: o.size || 15, w: o.w || "reg", color: o.color || C.mute }));
  if (o.right) add(f, T(o.right, { size: 13, color: o.rightColor || C.mute, w: "bold" }));
  return f;
}
function label(txt) { const t = T(txt, { size: 13, w: "bold", color: C.ink2 }); return t; }

// ---------- 화면 셸 ----------
// body(세로 오토레이아웃, 좌우 패딩 20) 를 만들고 statusbar/tabbar 를 포함한 360x780 스크린 프레임 반환
function newScreen(o = {}) {
  const screen = AF("screen", "VERTICAL", { fill: o.bg || C.cream, primary: "FIXED", counter: "FIXED", clip: true });
  sized(screen, 360, 780);
  add(screen, statusbar());
  const body = AF("body", "VERTICAL", { gap: o.gap != null ? o.gap : 14, pad: [6, 20, 0, 20], primary: "FIXED" });
  grow(body); stretch(body); body.clipsContent = true;
  add(screen, body);
  return { screen, body };
}
function withFab(screen) { const f = fab(); add(screen, f); f.layoutPositioning = "ABSOLUTE"; f.x = 282; f.y = 624; return screen; }

// 디바이스 베젤 + 라벨로 감싸기
function wrap(screen, labelText) {
  const w = AF("screen-wrap", "VERTICAL", { gap: 14, counterAlign: "CENTER" });
  const device = AF("device", "VERTICAL", { fill: C.bezel, radius: 46, pad: 7, primary: "FIXED", counter: "FIXED", clip: true });
  sized(device, 374, 794);
  screen.cornerRadius = 40;
  add(device, screen);
  add(w, device, T(labelText, { size: 15, w: "bold", color: hex("#6B5E44") }));
  return w;
}

// ============================================================
// 화면들
// ============================================================
function s_login() {
  const { screen, body } = newScreen();
  body.itemSpacing = 6; body.counterAxisAlignItems = "CENTER"; body.primaryAxisAlignItems = "CENTER";
  add(body, gulbi(120));
  add(body, T("자린고비", { size: 27, w: "bold", align: "CENTER" }));
  add(body, T("굴비 보며 아끼는 우리집 가계부", { size: 14, w: "bold", color: C.mute, align: "CENTER" }));
  const form = AF("form", "VERTICAL", { gap: 8 }); stretch(form); form.paddingTop = 22;
  add(form, label("이메일"), field("example@email.com"));
  const sp = T(" ", { size: 6 });
  add(form, label("비밀번호"), field("••••••••"));
  const sp2 = AF("sp", "VERTICAL", {}); sized(sp2, 1, 14);
  add(form, sp2, btnPrimary("로그인"));
  add(form, T("아직 회원이 아니신가요?  회원가입", { size: 13, w: "bold", color: C.ink2, align: "CENTER" }));
  add(body, form);
  return wrap(screen, "0. 로그인");
}

function s_home() {
  const { screen, body } = newScreen({ tab: 0 });
  body.paddingTop = 0;
  add(body, topbar("안녕하세요, 알뜰님 👋", { sub: true, subText: "2026년 5월" }));
  // 굴비 메시지
  const msg = AF("gulbi-msg", "HORIZONTAL", { fill: C.goldSoft, radius: 22, pad: [14, 16, 14, 16], gap: 12, counterAlign: "CENTER", stroke: C.goldSoft });
  stretch(msg);
  add(msg, gulbi(52), grow(T("이번 주 예산의 68% 썼어요.\n조금만 더 아껴볼까요? 🐟", { size: 14, w: "bold", color: hex("#6B5320"), lh: 145, width: 200 })));
  add(body, msg);
  // 예산 사용률
  const c1 = card({ gap: 10 });
  const r1 = rowBetween(); add(r1, T("이번 주 예산 사용률", { size: 15, w: "bold" }), T("68%", { size: 15, w: "bold", color: C.goldDeep }));
  add(c1, r1, progress(0.68));
  const r2 = rowBetween(); add(r2, T("사용 136,000원", { size: 12, w: "bold", color: C.mute }), T("예산 200,000원", { size: 12, w: "bold", color: C.mute }));
  add(c1, r2);
  add(body, c1);
  // 월 수입/지출
  const row = AF("row", "HORIZONTAL", { gap: 12, primary: "FIXED" }); stretch(row);
  const mk = (title, val, color) => { const c = card({ pad: 16, gap: 6 }); grow(c); add(c, T(title, { size: 12, w: "bold", color: C.mute }), T(val, { size: 20, w: "bold", color })); return c; };
  add(row, mk("월 수입", "+1,000,000", C.income), mk("월 지출", "-600,000", C.expense));
  add(body, row);
  // 최근 거래
  const c2 = card({ gap: 0 });
  const rh = rowBetween(); rh.paddingBottom = 6; add(rh, T("최근 거래", { size: 15, w: "bold" }), T("더보기 ›", { size: 12, w: "bold", color: C.mute }));
  add(c2, rh);
  add(c2, txnRow("🍚", C.expenseSoft, "식비 · 토레타", "5월 28일", "-2,200", C.expense, true));
  add(c2, txnRow("💰", C.incomeSoft, "장학금 · 학자금 상환", "5월 26일", "+6,000", C.income));
  add(body, c2);
  add(screen, tabbar(0));
  return wrap(withFab(screen), "1. 홈");
}

function ledgerSummary() {
  const c = card({ pad: [14, 16, 14, 16] });
  const row = rowBetween();
  const col = (t, v, color) => { const f = AF("c", "VERTICAL", { gap: 3, counterAlign: "CENTER" }); grow(f); add(f, T(t, { size: 11, w: "bold", color: C.mute }), T(v, { size: 15, w: "bold", color })); return f; };
  add(row, col("수입", "1,000,000", C.income), col("지출", "600,000", C.expense), col("합계", "400,000", C.ink));
  add(c, row);
  return c;
}
function s_ledgerList() {
  const { screen, body } = newScreen({ tab: 1 });
  body.paddingTop = 0;
  add(body, topbar("가계부", { suffix: "2026.05 ▾" }));
  add(body, seg(["일자별", "달력"], 0));
  add(body, ledgerSummary());
  const grp = (date, dow, sum, sumColor, rows) => {
    const g = AF("daygroup", "VERTICAL", { gap: 0 }); stretch(g);
    const h = rowBetween(); h.paddingTop = 12; h.paddingBottom = 6;
    add(h, T(date + "  " + dow, { size: 13, w: "bold" }), T(sum, { size: 12, w: "bold", color: sumColor }));
    add(g, h);
    rows.forEach((r) => add(g, r));
    return g;
  };
  add(body, grp("5월 28일", "화", "-2,200", C.expense, [txnRow("🍚", C.expenseSoft, "식비", "토레타", "-2,200", C.expense)]));
  add(body, grp("5월 27일", "월", "-2,200", C.expense, [txnRow("🥤", C.expenseSoft, "식비", "이오니크", "-2,200", C.expense)]));
  add(body, grp("5월 26일", "일", "+6,000", C.income, [txnRow("💰", C.incomeSoft, "장학금", "학자금 상환", "+6,000", C.income)]));
  add(screen, tabbar(1));
  return wrap(withFab(screen), "2. 가계부 (일자별)");
}

function s_ledgerCalendar() {
  const { screen, body } = newScreen({ tab: 1 });
  body.paddingTop = 0;
  add(body, topbar("가계부", { suffix: "2026.05 ▾" }));
  add(body, seg(["일자별", "달력"], 1));
  const cal = card({ pad: [14, 10, 14, 10], gap: 0 });
  const head = AF("calhead", "HORIZONTAL", { primary: "FIXED" }); stretch(head); head.paddingBottom = 8;
  ["일", "월", "화", "수", "목", "금", "토"].forEach((d, i) => {
    const cell = AF("h", "HORIZONTAL", { primaryAlign: "CENTER" }); grow(cell);
    add(cell, T(d, { size: 12, w: "bold", color: i === 0 ? C.expense : i === 6 ? C.blue : C.ink2 }));
    add(head, cell);
  });
  add(cal, head);
  const days = [
    [["27", "m"], ["28", "m"], ["29", "m"], ["30", "m"], ["1"], ["2", "e", "-5천"], ["3"]],
    [["4", "i", "+10만"], ["5"], ["6", "e", "-2천"], ["7"], ["8", "e", "-3만"], ["9"], ["10"]],
    [["11", "e", "-8천"], ["12"], ["13"], ["14", "e", "-1만"], ["15"], ["16"], ["17", "i", "+5천"]],
    [["18"], ["19", "e", "-4천"], ["20"], ["21"], ["22", "e", "-2만"], ["23"], ["24"]],
    [["25"], ["26", "i", "+6천", "on"], ["27", "e", "-2천"], ["28", "e", "-2천"], ["29"], ["30"], ["31"]],
  ];
  days.forEach((week) => {
    const wr = AF("w", "HORIZONTAL", { primary: "FIXED" }); stretch(wr);
    week.forEach((d) => {
      const on = d[3] === "on";
      const cell = AF("d", "VERTICAL", { gap: 2, counterAlign: "CENTER", primary: "FIXED", counter: "FIXED", fill: on ? C.goldSoft : null, radius: 12 });
      sized(cell, 44, 50); cell.paddingTop = 4; grow(cell);
      const mut = d[1] === "m";
      add(cell, T(d[0], { size: 12, w: "bold", color: mut ? hex("#CFC4B0") : C.ink }));
      if (d[2]) add(cell, T(d[2], { size: 9, w: "bold", color: d[1] === "i" ? C.income : C.expense }));
      add(wr, cell);
    });
    add(cal, wr);
  });
  add(body, cal);
  const detail = card({ gap: 8 });
  const dh = rowBetween(); add(dh, T("5월 26일  일", { size: 14, w: "bold" }), T("+6,000", { size: 14, w: "bold", color: C.income }));
  add(detail, dh, txnRow("💰", C.incomeSoft, "장학금", "학자금 상환", "+6,000", C.income, true));
  add(body, detail);
  add(screen, tabbar(1));
  return wrap(withFab(screen), "2-2. 가계부 (달력)");
}

function statsTabs(monthWeek, incomeExpense, amountCat) {
  const f = AF("tabs", "VERTICAL", { gap: 10 }); stretch(f);
  add(f, seg(["월", "주"], monthWeek));
  add(f, seg(["지출", "수입"], incomeExpense, { flat: true, accent: C.goldDeep }));
  add(f, seg(["단순 금액", "카테고리별"], amountCat));
  return f;
}
function s_statsMonthAmount() {
  const { screen, body } = newScreen({ tab: 2 });
  body.paddingTop = 0;
  add(body, topbar("통계"));
  add(body, statsTabs(0, 0, 0));
  const c = card({ gap: 4 });
  add(c, T("월별 지출 추이", { size: 15, w: "bold" }), T("최근 6개월", { size: 12, w: "bold", color: C.mute }));
  const chart = lineChart([[20, 70], [70, 45], [120, 80], [170, 30], [220, 55], [264, 15]], C.expense, 284, 110);
  chart.paddingTop = 10; add(c, chart);
  add(body, c);
  const c2 = card({ gap: 10 });
  const r1 = rowBetween(); add(r1, T("5월 지출", { size: 13, w: "bold", color: C.mute }), T("600,000원", { size: 17, w: "bold", color: C.expense }));
  const r2 = rowBetween(); add(r2, T("전월 대비", { size: 13, w: "bold", color: C.mute }), T("▼ 12% 절약", { size: 15, w: "bold", color: C.income }));
  add(c2, r1, r2);
  add(body, c2);
  add(screen, tabbar(2));
  return wrap(screen, "3. 통계 (월 · 단순금액)");
}

function s_statsMonthCategory() {
  const { screen, body } = newScreen({ tab: 2 });
  body.paddingTop = 0;
  add(body, topbar("통계"));
  add(body, statsTabs(0, 0, 1));
  const c = card({ gap: 0 });
  const head = rowBetween(); head.paddingBottom = 6; add(head, T("5월 지출 구성", { size: 15, w: "bold" }), T("600,000원", { size: 15, w: "bold", color: C.expense }));
  add(c, head);
  const segs = [
    { pct: 0.38, color: C.expense, name: "식비", amt: "228,000", p: "38%" },
    { pct: 0.22, color: C.gold, name: "교통", amt: "132,000", p: "22%" },
    { pct: 0.18, color: C.blue, name: "쇼핑", amt: "108,000", p: "18%" },
    { pct: 0.12, color: C.income, name: "문화", amt: "72,000", p: "12%" },
    { pct: 0.10, color: C.gray, name: "기타", amt: "60,000", p: "10%" },
  ];
  const dwrap = AF("dw", "HORIZONTAL", { primaryAlign: "CENTER", primary: "FIXED" }); stretch(dwrap); dwrap.paddingTop = 8; dwrap.paddingBottom = 14;
  add(dwrap, donut(segs, 150)); add(c, dwrap);
  segs.forEach((s, i) => {
    const lg = AF("legend", "HORIZONTAL", { counterAlign: "CENTER", gap: 9, pad: [9, 4, 9, 4], primary: "FIXED" }); stretch(lg);
    if (i > 0) { lg.strokes = [solid(C.line)]; lg.strokeWeight = 1; lg.strokeAlign = "INSIDE"; }
    const dot = figma.createRectangle(); dot.resize(11, 11); dot.cornerRadius = 4; dot.fills = [solid(s.color)];
    const nm = rowL(9); add(nm, dot, T(s.name, { size: 14, w: "bold" })); grow(nm);
    add(lg, nm, T(s.amt, { size: 14, w: "bold" }));
    const pc = T(s.p, { size: 13, w: "bold", color: C.mute, align: "RIGHT" }); pc.resize(44, pc.height); pc.textAutoResize = "HEIGHT"; pc.textAlignHorizontal = "RIGHT";
    add(lg, pc);
    add(c, lg);
  });
  add(body, c);
  add(screen, tabbar(2));
  return wrap(screen, "3-2. 통계 (월 · 카테고리별)");
}

function s_statsWeekAmount() {
  const { screen, body } = newScreen({ tab: 2 });
  body.paddingTop = 0;
  add(body, topbar("통계"));
  const tabs = AF("tabs", "VERTICAL", { gap: 12 }); stretch(tabs);
  add(tabs, seg(["월", "주"], 1), seg(["단순 금액", "카테고리별"], 0));
  add(body, tabs);
  const c1 = card({ gap: 2 });
  add(c1, T("주별 예산 vs 지출", { size: 15, w: "bold" }));
  const leg = rowL(14); leg.paddingBottom = 4;
  const tag = (color, t) => { const r = rowL(5); const d = figma.createRectangle(); d.resize(10, 10); d.cornerRadius = 3; d.fills = [solid(color)]; add(r, d, T(t, { size: 11, w: "bold", color: C.mute })); return r; };
  add(leg, tag(C.goldSoft, "예산"), tag(C.goldDeep, "지출")); add(c1, leg);
  const bars = barChart([
    { label: "1주", budget: 0.8, spend: 0.65 }, { label: "2주", budget: 0.8, spend: 0.9 },
    { label: "3주", budget: 0.8, spend: 0.48 }, { label: "4주", budget: 0.8, spend: 0.54 },
  ], 284, 140);
  bars.paddingTop = 8; add(c1, bars); add(body, c1);
  const c2 = card({ gap: 8 });
  add(c2, T("예산 대비 지출 달성률", { size: 15, w: "bold" }));
  add(c2, lineChart([[30, 35], [110, 22], [190, 58], [264, 48]], C.income, 284, 90));
  add(body, c2);
  add(screen, tabbar(2));
  return wrap(screen, "3-3. 통계 (주 · 단순금액)");
}

function s_more() {
  const { screen, body } = newScreen({ tab: 3 });
  add(body, topbar("더보기"));
  const prof = card({ gap: 14 }); prof.layoutMode = "HORIZONTAL"; prof.counterAxisAlignItems = "CENTER";
  const av = AF("av", "HORIZONTAL", { fill: C.goldSoft, radius: 27, primaryAlign: "CENTER", counterAlign: "CENTER", primary: "FIXED", counter: "FIXED" }); sized(av, 54, 54); add(av, gulbi(40));
  const info = AF("i", "VERTICAL", { gap: 2 }); grow(info); add(info, T("알뜰이", { size: 18, w: "bold" }), T("example@email.com", { size: 13, w: "bold", color: C.mute }));
  const edit = btnGhost("수정"); edit.layoutAlign = "INHERIT"; edit.layoutGrow = 0; edit.primaryAxisSizingMode = "AUTO"; edit.paddingLeft = 14; edit.paddingRight = 14; edit.paddingTop = 9; edit.paddingBottom = 9;
  add(prof, av, info, edit); add(body, prof);

  const sec = (t) => { const l = label(t); l.fontSize = 13; const w = AF("s", "VERTICAL", { gap: 8 }); stretch(w); w.paddingTop = 8; add(w, l); return w; };
  const s1 = sec("분류 / 관리");
  const c1 = card({ pad: 4, gap: 0 });
  add(c1, menuRow("📥", "수입 분류 관리", "›"), menuRow("📤", "지출 분류 관리", "›"), menuRow("🎯", "예산 설정", "›", true));
  add(s1, c1); add(body, s1);
  const s2 = sec("계정");
  const c2 = card({ pad: 4, gap: 0 });
  add(c2, menuRow("🔒", "비밀번호 수정", "›"), menuRow("🚪", "로그아웃", "›", true));
  add(s2, c2); add(body, s2);
  add(body, T("회원 탈퇴", { size: 12, w: "bold", color: C.mute, align: "CENTER" }));
  add(screen, tabbar(3));
  return wrap(screen, "4. 더보기 (설정)");
}

function s_addTxn() {
  const { screen, body } = newScreen();
  body.paddingTop = 0;
  add(body, topbar("거래 등록", { left: "✕", right: "저장", rightColor: C.mute }));
  add(body, seg(["수입", "지출"], 1, { accent: C.expense }));
  const amt = card({ pad: [22, 18, 22, 18], gap: 6, counterAlign: "CENTER" });
  add(amt, T("금액", { size: 12, w: "bold", color: C.mute }), T("- 2,200 원", { size: 30, w: "bold", color: C.expense }));
  add(body, amt);
  add(body, label("분류"));
  const chips = AF("chips", "HORIZONTAL", { gap: 9, primary: "FIXED" }); stretch(chips); chips.layoutWrap = "WRAP"; chips.counterAxisSpacing = 9;
  const chip = (t, on) => { const c = AF("chip", "HORIZONTAL", { fill: on ? C.expenseSoft : C.card, radius: 14, pad: [10, 15, 10, 15], stroke: on ? C.expense : C.line, strokeW: 1.5 }); add(c, T(t, { size: 14, w: "bold", color: on ? C.expense : C.ink2 })); return c; };
  add(chips, chip("🍚 식비", true), chip("🚌 교통"), chip("🛍 쇼핑"), chip("🎬 문화"), chip("＋"));
  add(body, chips);
  add(body, label("날짜"), field("2026년 5월 28일 (화)", { color: C.ink, w: "bold", between: true, right: "📅" }));
  add(body, label("메모"), field("토레타"));
  const sp = AF("sp", "VERTICAL", {}); grow(sp); add(body, sp);
  const foot = AF("foot", "VERTICAL", {}); stretch(foot); foot.paddingBottom = 20; add(foot, btnPrimary("저장하기")); add(body, foot);
  return wrap(screen, "5-1. 거래 등록");
}

function s_categorySettings() {
  const { screen, body } = newScreen();
  body.paddingTop = 0;
  add(body, topbar("지출 분류 관리", { left: "‹", right: " " }));
  add(body, seg(["수입 분류", "지출 분류"], 1));
  const c = card({ pad: 4, gap: 0 });
  const cats = [["🍚", "식비", C.expenseSoft], ["🚌", "교통", hex("#FDEFD6")], ["🛍", "쇼핑", hex("#E4ECFD")], ["🎬", "문화", C.incomeSoft], ["📦", "기타", hex("#F0ECE4")]];
  cats.forEach((ct, i) => {
    const r = AF("menu", "HORIZONTAL", { counterAlign: "CENTER", gap: 13, pad: [13, 12, 13, 12], primary: "FIXED" }); stretch(r);
    if (i < cats.length - 1) { r.strokes = [solid(C.line)]; r.strokeWeight = 1; r.strokeAlign = "INSIDE"; }
    add(r, iconChip(ct[0], ct[2]), grow(T(ct[1], { size: 15, w: "bold" })), T("≡", { size: 18, color: C.mute }));
    add(c, r);
  });
  add(body, c);
  add(body, btnGhost("＋ 분류 추가", C.goldDeep, C.goldSoft));
  return wrap(screen, "5-2. 카테고리 설정");
}

function s_budget() {
  const { screen, body } = newScreen();
  body.paddingTop = 0;
  add(body, topbar("예산 설정", { left: "‹", right: "저장" }));
  const msg = AF("gulbi-msg", "HORIZONTAL", { fill: C.goldSoft, radius: 22, pad: [14, 16, 14, 16], gap: 12, counterAlign: "CENTER" }); stretch(msg);
  add(msg, gulbi(46), grow(T("예산을 정하면 굴비가 매주 사용률을 알려드려요!", { size: 13, w: "bold", color: hex("#6B5320"), lh: 140, width: 190 })));
  add(body, msg);
  const c = card({ gap: 10 });
  add(c, label("한 달 예산"));
  add(c, field("800,000 원", { color: C.ink, w: "bold", size: 22 }));
  add(c, T("주간 약 200,000원", { size: 12, w: "bold", color: C.mute, align: "RIGHT" }));
  add(body, c);
  const sl = label("분류별 예산 (선택)"); sl.fontSize = 13; add(body, sl);
  const c2 = card({ pad: 4, gap: 0 });
  const items = [["🍚", "식비", C.expenseSoft, "300,000"], ["🚌", "교통", hex("#FDEFD6"), "150,000"], ["🛍", "쇼핑", hex("#E4ECFD"), "100,000"]];
  items.forEach((it, i) => {
    const r = AF("menu", "HORIZONTAL", { counterAlign: "CENTER", gap: 13, pad: [13, 12, 13, 12], primary: "FIXED" }); stretch(r);
    if (i < items.length - 1) { r.strokes = [solid(C.line)]; r.strokeWeight = 1; r.strokeAlign = "INSIDE"; }
    add(r, iconChip(it[0], it[2]), grow(T(it[1], { size: 15, w: "bold" })), T(it[3], { size: 14, w: "bold" }));
    add(c2, r);
  });
  add(body, c2);
  const sp = AF("sp", "VERTICAL", {}); grow(sp); add(body, sp);
  const foot = AF("foot", "VERTICAL", {}); stretch(foot); foot.paddingBottom = 20; add(foot, btnPrimary("예산 저장")); add(body, foot);
  return wrap(screen, "5-3. 예산 설정");
}

function s_profile() {
  const { screen, body } = newScreen();
  add(body, topbar("내 프로필", { left: "‹", right: " " }));
  const avwrap = AF("aw", "HORIZONTAL", { primaryAlign: "CENTER", primary: "FIXED" }); stretch(avwrap); avwrap.paddingTop = 10; avwrap.paddingBottom = 22;
  const av = AF("av", "HORIZONTAL", { fill: C.goldSoft, radius: 44, primaryAlign: "CENTER", counterAlign: "CENTER", primary: "FIXED", counter: "FIXED" }); sized(av, 88, 88); add(av, gulbi(62));
  add(avwrap, av); add(body, avwrap);
  add(body, label("닉네임"), field("알뜰이", { color: C.ink, w: "bold", between: true, right: "변경", rightColor: C.goldDeep }));
  add(body, label("이메일"), field("example@email.com"));
  const sp = AF("sp", "VERTICAL", {}); sized(sp, 1, 12); add(body, sp);
  add(body, btnGhost("🔒 비밀번호 수정"));
  add(body, btnGhost("회원 탈퇴", C.expense, C.expenseSoft));
  return wrap(screen, "5-4. 내 프로필");
}

function s_withdraw() {
  const { screen, body } = newScreen({ bg: hex("#79706A") });
  body.itemSpacing = 0;
  // 흐릿한 배경 타이틀
  const tb = topbar("내 프로필", { left: "‹", right: " " }); tb.opacity = 0.35; add(body, tb);
  const sp = AF("sp", "VERTICAL", {}); grow(sp); add(body, sp);
  // 바텀시트
  const sheet = AF("sheet", "VERTICAL", { fill: C.card, radius: 28, pad: [14, 22, 24, 22], gap: 0, counterAlign: "CENTER" });
  stretch(sheet);
  const grip = figma.createRectangle(); grip.resize(42, 5); grip.cornerRadius = 3; grip.fills = [solid(C.line)];
  add(sheet, grip); const g1 = AF("g", "VERTICAL", {}); sized(g1, 1, 14); add(sheet, g1);
  add(sheet, gulbi(50));
  add(sheet, T("정말 탈퇴하시겠어요?", { size: 18, w: "bold", align: "CENTER" }));
  add(sheet, T("탈퇴하면 모든 가계부 기록이 사라져요.\n확인을 위해 비밀번호를 입력해주세요.", { size: 13, w: "bold", color: C.mute, align: "CENTER", lh: 150, width: 280 }));
  const g2 = AF("g", "VERTICAL", {}); sized(g2, 1, 14); add(sheet, g2);
  add(sheet, field("현재 비밀번호"));
  const g3 = AF("g", "VERTICAL", {}); sized(g3, 1, 12); add(sheet, g3);
  add(sheet, btnPrimary("탈퇴하기", C.expense));
  const g4 = AF("g", "VERTICAL", {}); sized(g4, 1, 10); add(sheet, g4);
  add(sheet, btnGhost("취소"));
  // body의 좌우 패딩 제거(시트는 끝까지)
  body.paddingLeft = 0; body.paddingRight = 0; body.paddingTop = 0;
  tb.paddingLeft = 20; tb.paddingRight = 20;
  add(body, sheet);
  return wrap(screen, "5-5. 비밀번호 확인 · 탈퇴");
}

function s_changePw() {
  const { screen, body } = newScreen();
  add(body, topbar("비밀번호 수정", { left: "‹", right: " " }));
  const g0 = AF("g", "VERTICAL", {}); sized(g0, 1, 8); add(body, g0);
  add(body, label("현재 비밀번호"), field("현재 비밀번호"));
  const g1 = AF("g", "VERTICAL", {}); sized(g1, 1, 4); add(body, g1);
  add(body, label("새 비밀번호"), field("8자 이상"));
  add(body, label("새 비밀번호 확인"), field("새 비밀번호 재입력"));
  const sp = AF("sp", "VERTICAL", {}); grow(sp); add(body, sp);
  const foot = AF("foot", "VERTICAL", {}); stretch(foot); foot.paddingBottom = 20; add(foot, btnPrimary("변경하기")); add(body, foot);
  return wrap(screen, "5-4-2. 비밀번호 수정");
}

function s_signup() {
  const { screen, body } = newScreen();
  add(body, topbar("회원가입"));
  body.counterAxisAlignItems = "STRETCH"; body.primaryAxisAlignItems = "CENTER"; body.itemSpacing = 8;
  const top = AF("t", "VERTICAL", { gap: 6, counterAlign: "CENTER" }); stretch(top); top.paddingBottom = 12;
  add(top, gulbi(74), T("굴비와 함께 절약을 시작해요", { size: 14, w: "bold", color: C.mute, align: "CENTER" }));
  add(body, top);
  add(body, label("이메일"), field("example@email.com"));
  add(body, label("닉네임"), field("알뜰이"));
  add(body, label("비밀번호"), field("8자 이상"));
  add(body, label("비밀번호 확인"), field("비밀번호 재입력"));
  const g = AF("g", "VERTICAL", {}); sized(g, 1, 10); add(body, g);
  add(body, btnPrimary("가입하기"));
  return wrap(screen, "0-2. 회원가입");
}

// ============================================================
// 실행
// ============================================================
async function main() {
  const fam = await loadFonts();
  const builders = [
    s_login, s_home, s_ledgerList, s_statsMonthAmount, s_signup,
    s_ledgerCalendar, s_statsMonthCategory, s_statsWeekAmount, s_more, s_addTxn,
    s_categorySettings, s_budget, s_profile, s_withdraw, s_changePw,
  ];
  const root = AF("자린고비 가계부 — 목업", "HORIZONTAL", { fill: hex("#E8E0D0"), gap: 56, pad: 60, primaryAlign: "MIN", counterAlign: "MIN" });
  root.layoutWrap = "WRAP";
  root.counterAxisSpacing = 56;
  root.resize(5 * 374 + 4 * 56 + 120, 100); // 한 줄에 5개
  root.primaryAxisSizingMode = "FIXED";
  root.counterAxisSizingMode = "AUTO";

  for (const b of builders) {
    try { add(root, b()); }
    catch (e) { console.log("화면 생성 실패: " + b.name + " → " + e.message); }
  }
  figma.currentPage.appendChild(root);
  root.x = 0; root.y = 0;
  figma.currentPage.selection = [root];
  figma.viewport.scrollAndZoomIntoView([root]);
  figma.notify("자린고비 목업 생성 완료! (폰트: " + fam + ")");
  figma.closePlugin();
}
main();
