package com.bp.jaringochi.domain.gulbi.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
	
	@Value("${gms.api-key}")
	private String apiKey;
	
	@Value("${gms.model:gemini-3.1-flash-image}")
	private String model;
	
	public String dressGulbi(String base64Image, String mimeType, String outfitName, String mood) {
		try {
			String prompt = """
				Edit this mascot image.
				Keep the same fish character, same black-and white hand-drawn line style,
                same pose, same facial expression, same white or transparent background.
                Add only this outfit: %s.
                Do not add text, scenery, extra characters, shadows, or realistic photo style.
                Mood name: %s.
                Return only the edited image.
				""".formatted(outfitName, mood);
			
			Map<String, Object> body = Map.of(
					"contents", List.of(Map.of(
							"parts", List.of(
									Map.of("text", prompt),
									Map.of("inline_data", Map.of(
											"mime_type", mimeType,
											"data", base64Image
									))
							)
					))
			);
		
			String json = objectMapper.writeValueAsString(body);
			
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent"))
					.header("x-goog-api-key", apiKey)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();
			
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() / 100 != 2) {
				throw new IllegalStateException("GMS image generation failed: " + response.body());
			}
			
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode parts = root.path("candidates").get(0).path("content").path("parts");
			
			for (JsonNode part : parts) {
				JsonNode inlineData = part.path("inlineData");
				if (inlineData.isMissingNode()) {
					inlineData = part.path("inline_data");
				}
				
				String data = inlineData.path("data").asText(null);
				String resultMime = inlineData.path("mimeType").asText("image/png");
				
				if (data != null) {
					Base64.getDecoder().decode(data);
					return "data:" + resultMime + ";base64, " + data;
				}
			}
			
			throw new IllegalStateException("GMS response has no image data.");
		} catch (Exception e) {
			throw new IllegalStateException("Failed to generate dressed Gulbi image.", e);
		}
	}
}