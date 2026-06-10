api/ 폴더 = 서버 통신 코드만 모으는 폴더

참고: CategoryView.vue는 "화면 그리기"에만 집중하고, 서버 부르는 건 api/category.js에 맡김

src/api/
├─ http.js         ← 공통 설정 (baseURL, 인터셉터)
├─ category.js     ← 카테고리 요청 함수들 (목록/추가/수정/삭제)
├─ budget.js       ← 예산 요청 함수들
├─ statistics.js   ← 통계 요청 함수들
└─ ...

