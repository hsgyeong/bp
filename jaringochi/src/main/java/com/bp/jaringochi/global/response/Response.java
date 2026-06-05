package com.bp.jaringochi.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 API 응답")
public class Response<T> {
	
	@Schema(description = "비즈니스 상태 코드", example = "SUCCESS")
	private String code;
	
	@Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
	private String message;
	
	@Schema(description = "실제 데이터")
	private T data;
	
	public Response() {

	}

	public Response(String code, String message, T data) {
		super();
		this.code = code;
		this.message = message;
		this.data = data;
	}
	
	// ==================================================================
	// 성공 응답
	// ==================================================================
	
	// 성공 응답 (데이터만 전달, 메시지 기본값)
	public static <T> Response<T> success(T data) {
		return new Response<>("SUCCESS", "요청이 성공적으로 처리되었습니다.", data);
	}
	
	// 성공 응답 (커스텀 메시지 + 데이터)
	public static <T> Response<T> success(String message, T data) {
		return new Response<>("SUCCESS", message, data);
	}
	
	// 성공 응답 (메시지만, 데이터 없음) (등록, 수정, 삭제 성공 시 사용)
	public static <T> Response<T> success(String message) {
		return new Response<>("SUCCESS", message, null);
	}
	
	// ==================================================================
	// 실패 응답
	// ==================================================================
	public static <T> Response<T> error(String code, String message) {
		return new Response<>(code, message, null);
	}
	
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	
	public T getData() { return data; }
	public void setData(T data) { this.data = data; }

}
