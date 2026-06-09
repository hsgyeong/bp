package com.bp.jaringochi.domain.user.dao;

import org.apache.ibatis.annotations.Mapper;

import com.bp.jaringochi.domain.user.dto.User;

@Mapper
public interface UserDao {

	int insertUser(User user);
	
	User findByEmail(String email);
	
	User findById(Long id);
	
	int countByEmail(String email);
	
	int updateUser(User user);
	
	int deleteUser(Long id);
}
