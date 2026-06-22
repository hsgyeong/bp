package com.bp.jaringochi.domain.user.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class User {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private Long id;
	private String email;
	private String password;
	private String nickname;
	private LocalDateTime createdAt;
	private LocalDateTime deletedAt;
	private String currentOutfitKey;
	@JsonIgnore
	private String currentGulbiImagesJson;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}
	public void setDeletedAt(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
	
	public String getCurrentOutfitKey() { 
		return currentOutfitKey; 
	}
	public void setCurrentOutfitKey(String currentOutfitKey) { 
		this.currentOutfitKey = currentOutfitKey; 
	}

	public String getCurrentGulbiImagesJson() { 
		return currentGulbiImagesJson; 
	}
	
	public void setCurrentGulbiImagesJson(String currentGulbiImagesJson) {
		this.currentGulbiImagesJson = currentGulbiImagesJson;
	}

	// 응답엔 파싱된 맵으로 내려줌: { "happy": "data:image/png;base64,...", ... }
	@JsonProperty("currentGulbiImages")
	public Map<String, String> getCurrentGulbiImages(){
		if (currentGulbiImagesJson == null || currentGulbiImagesJson.isBlank()) return null;
		try {
			return MAPPER.readValue(currentGulbiImagesJson, new TypeReference<Map<String, String>>() {});
		} catch (Exception e) {
			return null;
		}
	}
}
