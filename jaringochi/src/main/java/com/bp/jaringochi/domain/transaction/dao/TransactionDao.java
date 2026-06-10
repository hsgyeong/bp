package com.bp.jaringochi.domain.transaction.dao;


import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.transaction.dto.Transaction;

@Mapper
public interface TransactionDao {
	
	List<Transaction> selectTransactions(@Param("userId") Long userId,
									  @Param("startDate") LocalDate startDate,
									  @Param("endDate") LocalDate endDate,
									  @Param("type") Integer type,
									  @Param("categoryId") LocalDate categoryId);
	

	Transaction selectTransactionById(@Param("id") Long id,
									  @Param("userId") Long userId);

	int insertTransaction(Transaction transaction);
	
	int updateTransaction(Transaction transaction);
	
	int deleteTransaction(@Param("id") Long id,
						  @Param("userId") Long userId);
}
