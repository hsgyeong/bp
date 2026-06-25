package com.bp.jaringochi.domain.gulbi.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GmsImageClient {

	// JSON 직렬화/역직렬화용. 요청 본문 만들 때 + 응답 파싱할 때 사용
	private final ObjectMapper objectMapper = new ObjectMapper();
	// 표준 자바 HTTP 클라이언트. 외부 API 호출용
	private final HttpClient httpClient = HttpClient.newHttpClient();

	// 결과 이미지 최대 한 변 (px)
	private static final int MAX_EDGE = 256;

	// 일시적 오류(503 과부하 / 429 쿼터) 재시도 횟수 + 요청 타임아웃
	private static final int MAX_ATTEMPTS = 4;
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

	@Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
	private String baseUrl;

	@Value("${gemini.api-key}")
	private String apiKey;

	@Value("${gemini.model:gemini-2.5-flash-image}")
	private String model;

	/**
	 * 앵커 생성: base 표정 굴비(옷 없음)에 '새 옷'을 입히고,
	 * 옷 이름(NAME)과 상세 묘사(DESC)를 텍스트로 함께 받는다.
	 * 나머지 무드는 이 DESC(상세 묘사)를 텍스트로 받아 같은 옷을 입히기 때문에(아래 dressWithDescription),
	 * 입력 이미지를 1장만 보내 '굴비 2마리 합성' 없이 옷을 통일한다.
	 *
	 * @param base64Image base 표정 굴비(옷 없음) 이미지 base64
	 * @param mimeType    그 이미지 MIME (예: image/png)
	 * @param mood        앵커로 쓸 무드 키(happy, sad ...)
	 * @return 옷 입은 결과 data URL + 옷 이름 + 옷 상세 묘사
	 */
	public DressResult dressAnchor(String base64Image, String mimeType, String mood) {
		String prompt = """
		               	Edit this mascot image. The fish has NO arms and NO legs.
		                Keep the same fish character, same hand-drawn line style,
		                same pose, same facial expression, same white or transparent background.
		                Invent ONE random, creative outfit (clothing or costume) and dress the fish in it.
		                The clothing has NO sleeves; never add arms, hands, legs, or feet.
		                Output EXACTLY ONE fish. Never draw a second fish or character.
		                Do not add text inside the image, scenery, extra characters, shadows, or realistic photo style.
		                After the image, output two plain-text lines, nothing else:
		                NAME: <a short 1-4 word name of the outfit in Korean>
		                DESC: <a precise English description of the outfit so it can be reproduced exactly:
		                list the garment type(s), every color, patterns, material/texture, and all distinctive
		                details and accessories>
		                Mood name: %s.
		                """.formatted(mood);

		List<Object> parts = new ArrayList<>();
		parts.add(Map.of("text", prompt));
		parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Image)));

		GeminiResult r = callGemini(parts);
		String outfitName = parseTagged(r.text(), "NAME");
		String outfitDesc = parseTagged(r.text(), "DESC");
		if (outfitName == null && !r.text().isBlank()) {
			outfitName = r.text().trim();		// 형식 안 지키면 통째로 이름 폴백
		}
		if (outfitDesc == null) {
			outfitDesc = outfitName;			// DESC를 못 뽑으면 이름이라도 묘사로 사용
		}
		return new DressResult(r.dataUrl(), outfitName, outfitDesc);
	}

	/**
	 * 옷 입히기: base 표정 굴비(옷 없음)에 '앵커가 만든 옷의 상세 묘사'를 텍스트로 받아 같은 옷을 입힌다.
	 * 입력 이미지가 1장뿐이라(묘사는 텍스트) '굴비 2마리 합성' 문제가 발생하지 않는다.
	 * 무드별 base 이미지의 표정은 그대로 유지된다.
	 *
	 * @param base64Image       base 표정 굴비(옷 없음) 이미지 base64
	 * @param mimeType          그 이미지 MIME (예: image/png)
	 * @param mood              무드 키
	 * @param outfitDescription 앵커가 만든 옷의 상세 묘사(영문)
	 * @return 옷 입은 결과 data URL
	 */
	public String dressWithDescription(String base64Image, String mimeType, String mood, String outfitDescription) {
		String prompt = """
		                Edit this single mascot image. There is exactly ONE fish, and it has NO arms and NO legs.
		                Dress this fish in this EXACT outfit, reproducing every detail identically:
		                %s
		                Hard rules:
		                - Reproduce it precisely: the same garment, the exact HEX colors on the same parts,
		                  and every listed detail element in the same position with the same shape, count,
		                  and color. Do NOT add, remove, move, resize, or recolor any detail element.
		                  Color every part exactly as the HEX codes say; do not leave any area white unless
		                  the description explicitly says white.
		                - This character has NO arms, NO hands, NO legs, NO feet, and the clothing has NO sleeves.
		                  NEVER add arms, hands, legs, feet, or sleeves. The outfit lies flat on the fish's body.
		                - Change ONLY the clothing. Keep the fish 100%% identical: same shape, body, face,
		                  facial expression, pose, and the same hand-drawn line style, on the same white or
		                  transparent background. Do not redraw, recolor, restyle, or reshape the fish, and do
		                  not change its facial expression.
		                - Output EXACTLY ONE fish. No second character, no text, no scenery, no shadows,
		                  no realistic photo style.
		                """.formatted(outfitDescription);

		List<Object> parts = new ArrayList<>();
		parts.add(Map.of("text", prompt));
		parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Image)));

		return callGemini(parts).dataUrl();
	}

	// Gemini generateContent 호출 + 응답에서 이미지 data URL·텍스트를 뽑는 공통 로직.
	private GeminiResult callGemini(List<Object> parts) {
		try {
			Map<String, Object> body = Map.of(
					"contents", List.of(Map.of("parts", parts)),								// contents 대화 한 턴
					"generationConfig", Map.of("responseModalities", List.of("TEXT", "IMAGE"))	// 이미지로 응답받도록 모달리티 지정
				);

			String json = objectMapper.writeValueAsString(body);

			// Gemini 직접 호출 주소 = 호스트 + /v1beta/models/ + 모델 + :generateContent.
			String url = baseUrl
				    + "/v1beta/models/"
				    + model + ":generateContent";

			// 503(과부하)·429(쿼터)·5xx는 일시적이라 지수 백오프로 재시도한다.
			HttpResponse<String> response = null;
			for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
				// HTTP 요청 구성: POST + JSON 본문 + 인증 헤더(x-goog-api-key에 Gemini 키) + 타임아웃.
				HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("x-goog-api-key", apiKey)
					.header("Content-Type", "application/json")
					.timeout(REQUEST_TIMEOUT)
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();

				response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				int sc = response.statusCode();
				if (sc / 100 == 2) {
					break;	// 성공
				}
				// 일시적 오류만 재시도(429/5xx). 그 외(400/401/403 등)는 즉시 실패.
				boolean retryable = (sc == 429 || sc >= 500);
				if (!retryable || attempt == MAX_ATTEMPTS) {
					throw new IllegalStateException("Gemini image generation failed: " + response.body());
				}
				// 지수 백오프 + 지터 (≈1s, 2s, 4s)
				long waitMs = (long) (Math.pow(2, attempt - 1) * 1000) + (long) (Math.random() * 400);
				Thread.sleep(waitMs);
			}

			// 응답 JSON 파싱: candidates[0].content.parts 안에서 이미지·텍스트를 모은다.
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode resultParts = root.path("candidates").path(0).path("content").path("parts");

			String dataUrl = null;						// 이미지
			StringBuilder textBuf = new StringBuilder();	// 텍스트(옷 이름 등) 모음

			// parts에는 text/이미지가 섞여 올 수 있어 순회하며 이미지·텍스트를 모은다.
			for (JsonNode part : resultParts) {
				JsonNode inlineData = part.path("inlineData");
				if (inlineData.isMissingNode()) {
					inlineData = part.path("inline_data");
				}
				String data = inlineData.path("data").asText(null);						 // base64 이미지 데이터
				if (data != null) {
					Base64.getDecoder().decode(data); // 깨진 base64면 여기서 예외 → 유효성 검증
					dataUrl = downscaleToDataUrl(data, MAX_EDGE);	// 축소 + 재인코딩 후 반환
				}
				String text = part.path("text").asText(null);
				if (text != null && !text.isBlank()) {
					textBuf.append(text).append('\n');
				}
			}
			if (dataUrl == null) {
				throw new IllegalStateException("Gemini response has no image data: " + response.body());
			}
			return new GeminiResult(dataUrl, textBuf.toString());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to generate Gulbi image.", e);
		}
	}

		private String downscaleToDataUrl(String rawBase64, int maxEdge) {
			try {
				byte[] src = Base64.getDecoder().decode(rawBase64);
				BufferedImage img = ImageIO.read(new ByteArrayInputStream(src));
				if (img == null) {
					return "data:image/png;base64," + rawBase64;	// 디코드 실패 → 원본 유지
				}

				int w = img.getWidth();
				int h = img.getHeight();
				int max = Math.max(w, h);
				if (max <= maxEdge) {
					return "data:image/png;base64," + rawBase64; 	// 이미 충분히 작음
				}

				double scale = (double) maxEdge / max;
				int neww = Math.max(1, (int) Math.round(w * scale));
				int newh = Math.max(1, (int) Math.round(h * scale));

				BufferedImage out = new BufferedImage(neww,  newh, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = out.createGraphics();
				// 축소 시 픽셀 계산 방식 : 바이리니어 → 부드럽게 줄임
				graphics .setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				// 렌더링 품질 우선
				graphics .setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				// 안티 앨리어싱 켜기 → 가장자리 계단 현상 완화
				graphics .setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				// 원본 img를 (0, 0)부터 nw x nh 크기로 out에 그려넣음 (실제 축소)
				graphics .drawImage(img,0, 0, neww, newh, null);
				// 그래픽 자원 해제
				graphics .dispose();

				// 축소된 이미지를 PNG 바이트로 다시 인코딩
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				// 프론트가 바로 <img src>로 쓸 수 있도록 data URL 형태로 반환
				ImageIO.write(out, "png", baos);

				// PNG 바이트 → base64 문자열로 인코딩
				String shrunk = Base64.getEncoder().encodeToString(baos.toByteArray());
				return "data:image/png;base64," + shrunk;

			} catch (Exception e) {
				return "data:image/png;base64," + rawBase64;	// 무슨 문제든 원본 유지
			}
		}

		// "TAG: value" 형태의 라인에서 value를 뽑는다(대소문자 무시). 없으면 null.
		private String parseTagged(String text, String tag) {
			if (text == null) {
				return null;
			}
			String prefix = tag + ":";
			for (String line : text.split("\\R")) {
				String t = line.strip();
				if (t.regionMatches(true, 0, prefix, 0, prefix.length())) {
					String v = t.substring(prefix.length()).strip();
					if (!v.isEmpty()) {
						return v;
					}
				}
			}
			return null;
		}

		// callGemini 내부 결과: 이미지 data URL + 응답 텍스트(옷 이름 등)
		private record GeminiResult(String dataUrl, String text) {}

		// 결과: 이미지 data URL + 옷 이름 + 옷 상세 묘사. (앵커 생성에서만 이름·묘사가 채워짐)
		public record DressResult(String dataUrl, String outfitName, String outfitDescription) {}
}
