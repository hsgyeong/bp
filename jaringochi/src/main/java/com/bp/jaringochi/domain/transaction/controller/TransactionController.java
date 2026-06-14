package com.bp.jaringochi.domain.transaction.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.transaction.dto.Transaction;
import com.bp.jaringochi.domain.transaction.service.TransactionService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.global.response.Response;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionController {

	private final TransactionService transactionService;
	
	// 거래 목록 조회
	@GetMapping
	public Response<List<Transaction>> getTransactions(
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate startDate,
			
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate endDate,
			
			@RequestParam(required = false)
			Integer type,
			
			@RequestParam(required = false)
			Long categoryId,
			
			@RequestParam(required = false)
	        String keyword,
	        
	        @RequestParam(defaultValue = "date_desc")
	        String sort,
			
			Authentication authentication) {
		
		Long userId = getCurrentUserId(authentication);
		
		List<Transaction> transactions = transactionService.getTransactions(userId, startDate, endDate, type, categoryId, keyword, sort);
		return Response.success(transactions);
	}
	
	// 거래 단건 조회
	@GetMapping("/{id}")
	public Response<Transaction> getTransaction(@PathVariable Long id,
												Authentication authentication) {
		Long userId = getCurrentUserId(authentication);
		
		Transaction transaction = transactionService.getTransaction(userId, id);
		return Response.success(transaction);
	}
	
	// 거래 내역 등록
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Response<Transaction> addTransaction(@RequestBody Transaction transaction,
												Authentication authentication) {
		
		Long userId = getCurrentUserId(authentication);
		
		Transaction created = transactionService.addTransaction(userId, transaction);
		return Response.success("거래내역이 등록되었습니다.", created);
	}
	
	// 거래 내역 수정
	@PutMapping("/{id}")
	public Response<Transaction> updateTransaction(@PathVariable Long id,
												   @RequestBody Transaction transaction,
												   Authentication authentication) {
		
		Long userId = getCurrentUserId(authentication);
		
		Transaction updated = transactionService.updateTransaction(userId, id, transaction);
		return Response.success("거래내역이 수정되었습니다.", updated);
	}
	
	// 거래 내역 삭제
	@DeleteMapping("/{id}")
	public Response<Void> deleteTransaction(@PathVariable Long id,
											Authentication authentication) {
		
		Long userId = getCurrentUserId(authentication);
		
		transactionService.deleteTransaction(userId, id);
		return Response.success("거래내역이 삭제되었습니다.");
	}
	
	private Long getCurrentUserId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
		}
		
		return Long.valueOf(jwt.getSubject());
	}
	
}
