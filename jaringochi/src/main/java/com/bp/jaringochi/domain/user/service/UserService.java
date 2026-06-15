package com.bp.jaringochi.domain.user.service;

import com.bp.jaringochi.domain.user.dto.User;

public interface UserService {

	User signup(User user);
	
	User login(String email, String password);
	
	User findById(Long id);
	
	User updateUser(Long id, User user);
	
	void deleteUser(Long id);
	
	boolean isNicknameAvailable(String nickname);
}
