package com.bp.jaringochi.domain.category.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.category.dto.Category;
import com.bp.jaringochi.domain.category.service.CategoryService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.global.response.Response;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // 목록 조회 - GET /api/categories?type=2
    @GetMapping
    public Response<List<Category>> getCategories(@RequestParam(required = true) Integer type,
                                                  Authentication authentication) {
        List<Category> categories = categoryService.getCategories(getCurrentUserId(authentication), type);
        return Response.success(categories);
    }

    // 추가 - POST /api/categories
    @PostMapping
    public Response<Category> addCategory(@RequestBody Category category,
                                          Authentication authentication) {
        Category created = categoryService.addCategory(getCurrentUserId(authentication), category);
        return Response.success("카테고리가 등록되었습니다.", created);
    }

    // 수정 - PUT /api/categories/{id}
    @PutMapping("/{id}")
    public Response<Category> updateCategory(@PathVariable Long id, @RequestBody Category category,
                                             Authentication authentication) {
        Category updated = categoryService.updateCategory(getCurrentUserId(authentication), id, category);
        return Response.success("카테고리가 수정되었습니다.", updated);
    }

    // 삭제(소프트) - DELETE /api/categories/{id}
    @DeleteMapping("/{id}")
    public Response<Void> deleteCategory(@PathVariable Long id, Authentication authentication) {
        categoryService.deleteCategory(getCurrentUserId(authentication), id);
        return Response.success("삭제되었습니다.");
    }

    // ===== 토큰에서 userId 추출 (거래 컨트롤러와 동일 패턴) =====
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
        }
        return Long.valueOf(jwt.getSubject());
    }
}