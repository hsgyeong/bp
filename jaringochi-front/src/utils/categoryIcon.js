// 카테고리 이름 → Tabler 라인 아이콘 클래스 매핑.
// paint(그림판) 테마에서 카테고리 이모지 대신 손그림 톤의 라인 아이콘을 쓰기 위함.
// 모르는 이름은 수입/지출 기본 아이콘으로 폴백.
const MAP = {
  // 수입
  월급: 'ti-cash',
  용돈: 'ti-coins',
  부수입: 'ti-trending-up',
  // 지출
  식비: 'ti-tools-kitchen-2',
  의류: 'ti-shirt',
  교통비: 'ti-bus',
  문화생활: 'ti-movie',
  의료비: 'ti-pill',
  // 흔히 추가되는 별칭들
  교통: 'ti-bus',
  카페: 'ti-coffee',
  커피: 'ti-coffee',
  쇼핑: 'ti-shopping-bag',
  주거: 'ti-home',
  통신: 'ti-device-mobile',
  경조사: 'ti-gift',
  여행: 'ti-plane',
  교육: 'ti-book',
  저축: 'ti-pig-money',
}

export function categoryTablerIcon(name, type) {
  if (name && MAP[name]) return MAP[name]
  return type === 1 ? 'ti-cash' : 'ti-receipt'
}
