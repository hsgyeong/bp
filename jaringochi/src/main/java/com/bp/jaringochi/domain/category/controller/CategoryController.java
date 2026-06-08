package com.bp.jaringochi.domain.category.controller;

import java.util.List;

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
import com.bp.jaringochi.global.response.Response;

@RestController
@RequestMapping("/api/categories")   
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // 목록 조회 — GET /api/categories?type=2
    @GetMapping
    public Response<List<Category>> getCategories(@RequestParam(required = true) Integer type) {
        List<Category> categories = categoryService.getCategories(getCurrentUserId(), type);
        return Response.success(categories);
    }

    // 추가 — POST /api/categories
    @PostMapping
    public Response<Category> addCategory(@RequestBody Category category) {
        Category created = categoryService.addCategory(getCurrentUserId(), category);
        return Response.success("카테고리가 등록되었습니다.", created);
    }

    // 수정 — PUT /api/categories/{id}
    @PutMapping("/{id}")
    public Response<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category updated = categoryService.updateCategory(getCurrentUserId(), id, category);
        return Response.success("카테고리가 수정되었습니다.", updated);
    }

    // 삭제(소프트) — DELETE /api/categories/{id}
    @DeleteMapping("/{id}")
    public Response<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(getCurrentUserId(), id);
        return Response.success("삭제되었습니다.");
    }

    // ===== 임시 인증 (TODO: 6/9 Security/JWT 적용 후 실제 로그인 사용자 id로 교체) =====
    private Long getCurrentUserId() {
        return 1L;
    }
}