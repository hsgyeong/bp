package com.bp.jaringochi.domain.transaction.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.transaction.dto.Transaction;
import com.bp.jaringochi.domain.transaction.service.TransactionService;
import com.bp.jaringochi.global.response.Response;

import io.swagger.v3.oas.annotations.parameters.RequestBody;


@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	@Autowired
	private TransactionService transactionService;
	
	
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
			Long categoryId) {
		List<Transaction> transactions = transactionService.getTransactions(getCurrentUserId(), startDate, endDate, type, categoryId);
		return Response.success(transactions);
	}
	
	// 거래 단건 조회
	@GetMapping("/{id}")
	public Response<Transaction> getTransaction(@PathVariable Long id) {
		Transaction transaction = transactionService.getTransaction(getCurrentUserId(), id);
		return Response.success(transaction);
	}
	
	// 거래 내역 등록
	@PostMapping
	public Response<Transaction> addTransaction(@RequestBody Transaction transaction) {
		Transaction created = transactionService.addTransaction(getCurrentUserId(), transaction);
		return Response.success("거래내역이 등록되었습니다.", created);
	}
	
	// 거래 내역 수정
	@PutMapping("/{id}")
	public Response<Transaction> updateTransaction(@PathVariable Long id,
												   @RequestBody Transaction Transaction) {
		Transaction updated = transactionService.updateTransaction(getCurrentUserId(), id, Transaction);
		return Response.success("거래내역이 수정되었습니다.", updated);
	}
	
	// 거래 내역 삭제
	@DeleteMapping("/{id}")
	public Response<Void> deleteTransaction(@PathVariable Long id) {
		transactionService.deleteTransaction(getCurrentUserId(), id);
		return Response.success("거래내역이 삭제되었습니다.");
	}
	
	
	// JWT 적용 후 실제 로그인 사용자 id로 교체 필요
	 private Long getCurrentUserId() {
	        return 1L;
	 }
	
}
