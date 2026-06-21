/*package com.bp.jaringochi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


// GMS 는 OpenAI 호환 엔드포인트를 그대로 프록시한다. base-url 을 GMS 로 두고
// Authorization 헤더에 발급키만 실으면 동일한 코드로 호출할 수 있다.
// 이미지 생성은 Spring AI 의 ImageModel 로 호출하므로(설정: spring.ai.openai.image.*)
// 여기서 JSON RestClient 빈은 두지 않는다. 이미지 <b>편집</b>(/edit)만 multipart 전용
@Configuration
public class GmsConfig {

	@Value("${gms.base-url:https://gms.ssafy.io/gmsapi}")
	private String baseUrl;
	
	@Value("${gms.key}")
	private String key;
	
	@Bean("gmsMultipartRestClient")
	RestClient gmsMultipartRestClient() {
		return null;
	}
}*/
