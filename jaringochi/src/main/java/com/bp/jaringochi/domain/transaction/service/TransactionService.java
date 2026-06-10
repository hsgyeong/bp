package com.bp.jaringochi.domain.transaction.service;

import java.time.LocalDate;
import java.util.List;

import com.bp.jaringochi.domain.transaction.dto.Transaction;

public interface TransactionService {
	
	List<Transaction> getTransactions(Long userId,
									  LocalDate startDate,
									  LocalDate endDate,
									  Integer type,
									  Long categoryId);
	
	Transaction getTransaction(Long userId, Long id);
	
	Transaction addTransaction(Long userId, Transaction transaction);
	
	Transaction updateTransaction(Long userId, Long id, Transaction transaction);
	
	void deleteTransaction(Long userId, Long id);
		
}
