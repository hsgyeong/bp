package com.bp.jaringochi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bp.jaringochi.global.response.Response;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<Response<Void>> handleBusinessException(BusinessException e) {
	    ErrorCode errorCode = e.getErrorCode();
	    log.warn("BusinessException occurred: code={}, message={}",
	            errorCode.getCode(), errorCode.getMessage());

	    return ResponseEntity
	            .status(errorCode.getStatus())
	            .body(Response.error(errorCode.getCode(), errorCode.getMessage()));
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Response<Void>> handleException(Exception e) {
		// 예상치 못한 예외는 ERROR 레벨로 스택 트레이스까지 함께 로깅 (디버깅용)
		log.error("Unhandled exception occurred", e);

		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(Response.error(errorCode.getCode(), errorCode.getMessage()));
	}
}
