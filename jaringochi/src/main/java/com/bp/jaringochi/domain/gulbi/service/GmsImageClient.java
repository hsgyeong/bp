package com.bp.jaringochi.domain.gulbi.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GmsImageClient {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Value("${gms.base-url:https://gms.ssafy.io/gmsapi}")
	private String baseUrl;

	@Value("${gms.api-key}")
	private String apiKey;

	@Value("${gms.model:gemini-2.5-flash-image}")
	private String model;
	
	public String dressGulbi(String base64Image, String mimeType, String outfitName, String mood, String referenceB64) {
		try {
			List<Object> parts = new ArrayList<>();
			String prompt;
			if (referenceB64 == null) {
				  prompt = """
			                Edit this mascot image.
			                Keep the same fish character, same black-and-white hand-drawn line style,
			                same pose, same facial expression, same white or transparent background.
			                Add only this outfit: %s.
			                Do not add text, scenery, extra characters, shadows, or realistic photo style.
			                Mood name: %s.
			                """.formatted(outfitName, mood);
				 parts.add(Map.of("text", prompt));
				 parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Image)));
			} else {
			      // 나머지 무드: 2번 이미지의 옷을 1번 굴비에 똑같이
			      prompt = """
			                There are two images.
			                Image 1 is the mascot to edit. Image 2 shows the SAME mascot already wearing an outfit.
			                Put onto the fish in image 1 the EXACT SAME outfit as in image 2:
			                same outfit type, same colors, same patterns, same details.
			                Keep image 1's own pose, facial expression, line style, and background unchanged.
			                The outfit is "%s". Do not add text, scenery, extra characters, shadows, or realistic photo style.
			                Mood name: %s.
			                """.formatted(outfitName, mood);
			      parts.add(Map.of("text", prompt));
				  parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Image)));    
				  parts.add(Map.of("inline_data", Map.of("mime_type", "image/png", "data", referenceB64))); 
			}
			
			Map<String, Object> body = Map.of(
					"contents", List.of(Map.of("parts", parts)),
					"generationConfig", Map.of("responseModalities", List.of("TEXT", "IMAGE"))
				);

				String json = objectMapper.writeValueAsString(body);
				String url = baseUrl
					    + "/generativelanguage.googleapis.com/v1beta/models/"
					    + model + ":generateContent";

				HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("x-goog-api-key", apiKey)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();

				HttpResponse<String> response =
					httpClient.send(request, HttpResponse.BodyHandlers.ofString());

				if (response.statusCode() / 100 != 2) {
					throw new IllegalStateException("Gemini image generation failed: " + response.body());
				}

				JsonNode root = objectMapper.readTree(response.body());
				JsonNode resultParts = root.path("candidates").get(0).path("content").path("parts");
				for (JsonNode part : resultParts) {
					JsonNode inlineData = part.path("inlineData");
					if (inlineData.isMissingNode()) {
						inlineData = part.path("inline_data");
					}
					String data = inlineData.path("data").asText(null);
					String resultMime = inlineData.path("mimeType").asText("image/png");
					if (data != null) {
						Base64.getDecoder().decode(data); // 유효성 검증
						return "data:" + resultMime + ";base64," + data;
					}
				}
				throw new IllegalStateException("Gemini response has no image data: " + response.body());

			} catch (Exception e) {
				throw new IllegalStateException("Failed to generate dressed Gulbi image.", e);
			}
		}
	
}