package com.bp.jaringochi.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 빈 구성.
 *  - chatClient : 모든 채팅 호출(레포트 생성·굴비 한마디)에 사용.
 *    OpenAI 직접 호출 라우팅은 application.properties(spring.ai.openai.base-url=https://api.openai.com/v1)에서 처리한다.
 *
 * 굴비 한마디는 1회성 응답이라 대화 메모리가 필요 없어 ChatMemory 는 두지 않는다.
 * 월간 연속성(지난달 참조)은 DB(monthly_report) 조회로 처리한다.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
