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

	@Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
	private String baseUrl;

	@Value("${gemini.api-key}")
	private String apiKey;

	@Value("${gemini.model:gemini-2.5-flash-image}")
	private String model;
	
	/**
	 * 굴비 옷 보상
	 *
	 * @param base64Image  편집 대상 굴비 이미지(base64)
	 * @param mimeType     그 이미지의 MIME 타입(예: image/png)
	 * @param mood         무드 키(happy, sad ...).
	 * @param referenceB64 정본(앵커) 이미지의 base64. null이면 '새로 옷 생성',
	 *                     값이 있으면 '그 옷을 그대로 따라 입히기'(7무드 옷 통일용)
	 * @return "data:image/png;base64,...." 형태의 결과 이미지 data URL
	 */
	public DressResult dressGulbi(String base64Image, String mimeType, String mood, String referenceB64) {
		try {			
			// contents[0].parts 배열을 담을 리스트
			List<Object> parts = new ArrayList<>();
			String prompt;
			if (referenceB64 == null) {
				  prompt = """
			               	Edit this mascot image.
			                Keep the same fish character, same black-and-white hand-drawn line style,
			                same pose, same facial expression, same white or transparent background.
			                Invent ONE random, creative outfit (clothing or costume) and dress the fish in it.
			                Do not add text inside the image, scenery, extra characters, shadows, or realistic photo style.
			                Also output a short 1-4 word name of the outfit in Korean as plain text.
			                Mood name: %s.
			                """.formatted(mood);
				 parts.add(Map.of("text", prompt));
				 parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Image)));
			} else {
			      // 나머지 무드: 2번 이미지의 옷을 1번 굴비에 똑같이
			      prompt = """
			                There are two images.
			                Image 1 is the mascot to edit. Image 2 shows the SAME mascot already wearing an outfit.
			                Dress the fish in Image 1 with the EXACT SAME outfit as in Image 2:
			                same outfit type, same colors, same patterns, same details.
			                The fish in the result MUST be wearing this outfit — never return it without the outfit.
			                Change ONLY the clothing. You MUST NOT change the fish itself in any way:
			                keep Image 1's exact character shape, body, face, facial expression, pose,
			                hand-drawn line style, and background completely identical.
			                Do not redraw, recolor, or restyle the fish. The expression must stay exactly as in Image 1.
			                Do not add text, scenery, extra characters, shadows, or realistic photo style.
			                Mood name: %s.
			                """.formatted(mood);
			      parts.add(Map.of("text", prompt));
				  parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Image)));    
				  parts.add(Map.of("inline_data", Map.of("mime_type", "image/png", "data", referenceB64))); 
			}
			
			Map<String, Object> body = Map.of(
					"contents", List.of(Map.of("parts", parts)),								// contents 대화 한 턴
					"generationConfig", Map.of("responseModalities", List.of("TEXT", "IMAGE"))	// generationConfig 이미지로 응답받도록 모달리티 지정
				);

				String json = objectMapper.writeValueAsString(body);
				
				// Gemini 직접 호출 주소 = 호스트 + /v1beta/models/ + 모델 + :generateContent.
				String url = baseUrl
					    + "/v1beta/models/"
					    + model + ":generateContent";

				// HTTP 요청 구성: POST + JSON 본문 + 인증 헤더(x-goog-api-key에 GMS 키).
				HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("x-goog-api-key", apiKey)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();

				HttpResponse<String> response =
					httpClient.send(request, HttpResponse.BodyHandlers.ofString());

				// 2xx가 아니면 실패. 응답 본문을 메시지에 담아 원인 파악 가능하게 던진다.
				if (response.statusCode() / 100 != 2) {
					throw new IllegalStateException("Gemini image generation failed: " + response.body());
				}

				// 응답 JSON 파싱: candidates[0].content.parts 안에서 이미지 데이터를 찾는다.
				JsonNode root = objectMapper.readTree(response.body());
				JsonNode resultParts = root.path("candidates").path(0).path("content").path("parts");
				
				String dataUrl = null;		// 이미지
				String outfitName = null;	// 옷 이름
				
				// parts에는 text/이미지가 섞여 올 수 있어 순회하며 이미지를 찾는다.
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
						outfitName = text.trim();
					}
				}
				if (dataUrl == null ) {
					throw new IllegalStateException("Gemini response has no image data: " + response.body());
				}
				return new DressResult(dataUrl, outfitName);
			} catch (Exception e) {
				throw new IllegalStateException("Failed to generate dressed Gulbi image.", e);
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
		
		// 결과: 이미지 data URL + 제미나이가 지은 옷 이름. 레퍼런스 호출 땐 outfitName=null
		public record DressResult(String dataUrl, String outfitName) {}
}