package com.bp.jaringochi.domain.category.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.category.dao.CategoryDao;
import com.bp.jaringochi.domain.category.dto.Category;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final String DEFAULT_COLOR = "#F2A33C"; // 기본 색상 - 디폴트 카테고리 색상 등

    @Autowired
    private CategoryDao categoryDao;

    // 1. 목록 조회 - 그냥 DAO에 위임
    @Override
    public List<Category> getCategories(Long userId, Integer type) {
        return categoryDao.selectCategories(userId, type);
    }

    // 2. 추가
    @Override
    @Transactional
    public Category addCategory(Long userId, Category category) {
        category.setUserId(userId);                 // 내 카테고리로 저장

        if (category.getColor() == null) {          // 색 설정 안하면 기본색
            category.setColor(DEFAULT_COLOR);
        }

        categoryDao.insertCategory(category);       // CategoryMapper 참고 - useGeneratedKeys로 id 채워짐
        return category;                            // id 포함된 객체 그대로 반환
    }

    // 3. 수정
    @Override
    @Transactional
    public Category updateCategory(Long userId, Long id, Category category) {
        Category existing = findOwned(userId, id);  // 존재·소유·기본여부 검증

        category.setId(id);
        category.setUserId(userId);
        if (category.getColor() == null) {          // 색 안 보내면 기존값 유지
            category.setColor(existing.getColor());
        }

        categoryDao.updateCategory(category);
        return categoryDao.selectCategoryById(id);  // 갱신된 최신 데이터 반환
    }

    // 4. 삭제 (소프트)
    @Override
    @Transactional
    public void deleteCategory(Long userId, Long id) {
        findOwned(userId, id);                      // 검증만 하고
        categoryDao.hideCategory(id);               // 실제론 is_active=0
    }

    // ===== 공통 검증: 존재 + 소유권 + 기본카테고리 =====
    private Category findOwned(Long userId, Long id) {
        Category c = categoryDao.selectCategoryById(id);
        if (c == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);   // 404
        }
        // user_id == null(기본제공) 이거나, 내 것이 아니면 금지
        if (c.getUserId() == null || !c.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CATEGORY_FORBIDDEN);   // 403
        }
        return c;
    }
}