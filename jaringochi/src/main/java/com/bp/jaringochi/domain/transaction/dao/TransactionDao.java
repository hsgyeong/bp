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
									  @Param("categoryId") Long categoryId,
									  @Param("keyword") String keyword,
                                      @Param("sort") String sort);
	

	Transaction selectTransactionById(@Param("userId") Long userId,
									  @Param("id") Long id);

	int insertTransaction(Transaction transaction);
	
	int updateTransaction(Transaction transaction);
	
	int deleteTransaction(@Param("userId") Long userId,
						  @Param("id") Long id);
}
