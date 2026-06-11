api/ 폴더 = 서버 통신 코드만 모으는 폴더

참고: CategoryView.vue는 "화면 그리기"에만 집중하고, 서버 부르는 건 api/category.js에 맡김

src/api/
├─ http.js         ← 공통 설정 (baseURL, 인터셉터)
├─ category.js     ← 카테고리 요청 함수들 (목록/추가/수정/삭제)
├─ budget.js       ← 예산 요청 함수들
├─ statistics.js   ← 통계 요청 함수들
└─ ...




---

## 요청 한 번이 도는 전체 흐름 (카테고리 목록 예시)

나가는 길: 컴포넌트 → category.js(함수) → http.js(주소완성 + 토큰부착) → 프록시 → 백엔드
백엔드 안: JWT필터 검문 → Security 통과 → Controller → Service → DAO → DB
돌아오는 길: DB결과를 {code,message,data} 봉투로 싸서 반환 → 그래서 res.data.data 로 꺼냄
화면: 받은 배열을 ref(categories)에 담으면 Vue가 자동으로 다시 그림

  ① CategoryView.vue   onMounted/watch(type) → listCategories(2)
        ▼
  ② category.js        http.get('/categories', {params:{type:2}})
        ▼
  ③ http.js            baseURL '/api' 붙음 + Authorization: Bearer <토큰> 부착
        ▼
  ④ Vite 프록시        '/api/*' → :8080 으로 전달
        ▼
  ⑤ JwtAuthenticationFilter   토큰 검증 → "1번 유저" 도장 (없으면 401 컷)
        ▼
  ⑥ SecurityConfig     .anyRequest().authenticated() 통과
        ▼
  ⑦ CategoryController getCategories(type=2), getCurrentUserId()→1L
        ▼
  ⑧ CategoryServiceImpl → ⑨ CategoryDao + CategoryMapper.xml (SELECT)
        ▼

  ⑩ MySQL
     SELECT 결과 = 행 5개 (raw 데이터)
        │  ▲ 올라감
        ▼
  ⑨ CategoryDao + CategoryMapper.xml
     MyBatis가 DB 행 → Java 객체로 변환
     (snake_case 컬럼 → camelCase 필드: user_id → userId)
     결과: List<Category> (자바 객체 5개)
        │  ▲
        ▼
  ⑧ CategoryServiceImpl
     그 List<Category> 를 그대로 컨트롤러에 반환
        │  ▲
        ▼
  ⑦ CategoryController        ★여기서 '봉투'에 담음★
     return Response.success(list)
     → { code:"SUCCESS", message:"...", data:[ ...5개 ] }
     → Spring이 이 객체를 JSON 문자열로 변환(직렬화)
        │  ▲
        ▼
  ⑥⑤ Security 필터 체인을 거꾸로 통과해서 응답이 밖으로 나감
        │  ▲
        ▼  (네트워크 경계 다시 넘음)
  ④ Vite 프록시
     :8080 의 응답을 :5173 쪽으로 그대로 전달
        │  ▲
        ▼
  ③ http.js (axios)
     JSON 문자열을 받아 자바스크립트 객체로 파싱
     · axios가 본문을 res.data 에 넣음
     · 그 안에 우리 봉투가 또 있어서 → res.data.data 가 진짜 배열
        │  ▲
        ▼
  ② category.js → ① CategoryView.vue 로 res 도착

* 토큰 없으면 ⑤에서 401 → 그래서 http.js 인터셉터가 토큰을 붙여야 함
* 응답이 '봉투'라 한 겹 더 → res.data.data (axios의 .data + 우리 봉투의 .data)

