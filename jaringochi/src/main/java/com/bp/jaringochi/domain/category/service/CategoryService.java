package com.bp.jaringochi.domain.category.service;

import java.util.List;
import com.bp.jaringochi.domain.category.dto.Category;

public interface CategoryService {

    // 목록 조회 (type별: 1=수입 / 2=지출)
    List<Category> getCategories(Long userId, Integer type);

    // 추가 (생성된 카테고리 반환)
    Category addCategory(Long userId, Category category);

    // 수정 (수정된 카테고리 반환)
    Category updateCategory(Long userId, Long id, Category category);

    // 삭제 (소프트 삭제 — 반환값 없음)
    void deleteCategory(Long userId, Long id);
}