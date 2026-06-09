package com.bp.jaringochi.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.user.dao.UserDao;
import com.bp.jaringochi.domain.user.dto.User;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)	// 서비스의 기본 트랜잭션을 읽기 전용으로 설정
public class UserServiceImpl implements UserService {

	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder; 

	@Override
	@Transactional	// insert가 필요하기 때문에 @Transactional 붙여서 읽기 전용 설정을 덮어씀
	public User signup(User user) {
		if (userDao.countByEmail(user.getEmail()) > 0){
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}
		
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		userDao.insertUser(user);
		
		user.setPassword(null);		// 비밀번호 해시값 노출 방지
		return user;
	}

	@Override
	public User login(String email, String password) {
		User user = userDao.findByEmail(email);
		
		if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_LOGIN);
		}
		
		user.setPassword(null);
		return user;
	}

	@Override
	public User findByEmail(String email) {
		User user = userDao.findByEmail(email);
		
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		
		user.setPassword(null);
		
		return user;
	}
	

	@Override
	public User findById(Long id) {
		User user = userDao.findById(id);
		
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		
		user.setPassword(null);
		
		return user;
	}

	@Override
	@Transactional
	public User updateUser(Long id, User user) {
		User savedUser = userDao.findById(id);
		
		if (savedUser == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		
		boolean hasNickname = user.getNickname() != null && !user.getNickname().isBlank();
		boolean hasPassword = user.getPassword() != null && !user.getPassword().isBlank();
		
		if (!hasNickname && !hasPassword) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		
		user.setId(id);
		
		if (hasPassword) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}
		
		int result = userDao.updateUser(user);
		
		if (result == 0) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		
		User updateUser = userDao.findById(id);
		updateUser.setPassword(null);
		return updateUser;
	}

	@Override
	@Transactional
	public void deleteUser(Long id) {
		int result = userDao.deleteUser(id);
		
		if (result == 0) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		
	}

}
