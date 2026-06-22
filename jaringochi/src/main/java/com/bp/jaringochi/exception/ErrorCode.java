package com.bp.jaringochi.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	
	// ===== 공통 =====
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 내부 오류가 발생했습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "E400", "잘못된 요청입니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E405", "지원하지 않는 HTTP 메서드입니다."),
	
	// ===== 인증 =====
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A409", "이미 사용중인 이메일입니다."),
	INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "A401", "이메일 또는 비밀번호가 올바르지 않습니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A402", "유효하지 않은 토큰입니다."),
		
	// ===== 사용자 =====
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U404", "해당 사용자를 찾을 수 없습니다."),
	USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U401", "로그인이 필요합니다."),
	USER_FORBIDDEN(HttpStatus.FORBIDDEN, "U403", "권한이 없습니다."),
	
	// ===== 카테고리 =====
	CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "C404", "해당 카테고리를 찾을 수 없습니다."),
	CATEGORY_FORBIDDEN(HttpStatus.FORBIDDEN, "C403", "해당 카테고리에 접근할 권한이 없습니다."),
	
	// ===== 거래내역 =====
	TRANSACTION_INVALID_INPUT(HttpStatus.BAD_REQUEST, "T400", "입력값이 올바르지 않습니다."),
	TRANSACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "T403", "해당 거래내역에 접근할 권한이 없습니다."),
	TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "T404", "해당 거래내역을 찾을 수 없습니다."),
	
	// ===== 주간 예산 =====
	BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "B404", "해당 예산을 찾을 수 없습니다."),
	BUDGET_ALREADY_EXISTS(HttpStatus.CONFLICT, "B409", "해당 주의 예산이 이미 존재합니다."),
	BUDGET_UPDATE_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "B403", "이번 달 예산 수정 횟수를 초과하였습니다."),
	BUDGET_INVALID_INPUT(HttpStatus.BAD_REQUEST, "B400", "예산 입력값이 올바르지 않습니다."),
	
	// ===== 알림 =====
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N404", "해당 알림을 찾을 수 없습니다."),
	NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "N403", "해당 알림에 접근할 권한이 없습니다."),

	// ===== 통계 =====
	STATISTICS_INVALID_INPUT(HttpStatus.BAD_REQUEST, "S400", "통계 조회 입력값이 올바르지 않습니다."),

	// ===== 월간 레포트 =====
	REPORT_INVALID_INPUT(HttpStatus.BAD_REQUEST, "R400", "레포트 조회 입력값이 올바르지 않습니다."),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "R404", "해당 월의 레포트를 찾을 수 없습니다."),
	REPORT_ALREADY_REPLIED(HttpStatus.CONFLICT, "R409", "이번 달 굴비와의 한 마디는 이미 사용했어요."),
	REPORT_AI_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "R503", "굴비가 지금 답을 떠올리지 못했어요. 잠시 후 다시 시도해 주세요."),
	
	
	// ===== 굴비 보상 =====
	REWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "G404", "보상 정보를 찾을 수 없습니다."),
	REWARD_NOT_ELIGIBLE(HttpStatus.FORBIDDEN, "G403", "예산 절약에 성공한 주만 굴비 옷을 뽑을 수 있습니다."),
	REWARD_ALREADY_DECIDED(HttpStatus.CONFLICT, "G409", "이미 처리된 보상입니다."),
	REWARD_INVALID_DECISION(HttpStatus.BAD_REQUEST, "G400", "decision은 ACCEPT 또는 DECLINE이어야 합니다."),
	REWARD_NO_DRAW(HttpStatus.BAD_REQUEST, "G401", "먼저 굴비 옷을 뽑아야 합니다.");
	
	private final HttpStatus status;
	private final String code;
	private final String message;
	
	private ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
	
	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getMessage() { return message; }
	
}
