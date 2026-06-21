package com.bp.jaringochi.domain.gulbi.dto;

import java.util.Map;

public record GulbiDrawRequest(
	Map<String, BaseImage> baseImages
) {
	public record BaseImage(
		String mimeType,
		String base64
	) {}
}
	
