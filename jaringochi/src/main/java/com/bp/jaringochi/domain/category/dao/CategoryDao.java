package com.bp.jaringochi.domain.category.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.category.dto.Category;

@Mapper
public interface CategoryDao {
	
	int insertCategory(Category category);
	
	// userId와 type(1=수입, 2=지출)을 받아 필터링해서 조회해야 하기 때문에 @Param으로 인자를 받음
	List<Category> selectCategories(@Param("userId") Long userId, @Param("type") Integer type);
	
	Category selectCategoryById(Long id);
	
	int updateCategory(Category category);
	
	int hideCategory(Long id); // softdelete 
	
}

/*

@Param("userId") = "이 매개변수를 Mapper XML에서 #{userId}로 부를 수 있게 이름표를 붙임." 
그리고 그 값은 Service가 호출하면서 넣어주는 것(궁극적으론 로그인한 유저 id).

매개변수가 1개면 MyBatis가 알아서 매칭해서 @Param 없어도 됨.
그러나 2개 이상이면 MyBatis가 매개변수 중 뭐가 userId인지 모름 → 그래서 이름표(@Param)를 직접 붙여줘야 XML에서 이름으로 꺼낼 수 있음.

*/ 
