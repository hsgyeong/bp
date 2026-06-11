package com.bp.jaringochi.domain.category.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Category {
	private Long id;
	private Long userId;
	private String name;
	private Integer type;
	private Integer displayOrder;
	private Boolean isActive;
	private String icon;
	private String color;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
