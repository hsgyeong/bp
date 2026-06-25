package com.bp.jaringochi.domain.gulbi.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 옷 디자이너: 레포트와 같은 OpenAI {@link ChatClient}를 재사용해 '옷 스펙(텍스트)'을 설계한다.
 *
 * 이미지 모델(Gemini)이 다양한 표정 이미지에 매번 '동일한 옷'을 입힐 수 있도록,
 * 옷을 단순하게 + 색상 HEX·핵심 디테일을 명시한 짧은 SPEC(영문 60단어 이내)으로 받는다.
 * (이미지 모델은 긴 프롬프트일수록 디테일을 흘리므로 SPEC은 짧게 강제한다.)
 *
 * 굴비 이미지는 의도적으로 보내지 않는다 — 굴비가 네이비 톤 그림이라 이미지를 주면
 * 디자이너가 그 색에 맞춰 매번 네이비 옷만 골라, 색 다양성이 떨어지기 때문이다.
 */
@Service
public class OutfitDesignerService {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OutfitDesignerService.class);

	private final ChatClient chatClient;

	public OutfitDesignerService(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	private static final String SYSTEM = """
			You are an expert animation character costume designer.
			You design outfits for a simple, hand-drawn fish mascot character — a small
			fish with no arms and no legs. Design ONE outfit for it.

			A separate image model will later paint your outfit onto several pictures of
			the SAME fish in different facial expressions, and each picture is generated
			independently. The outfit must therefore be reproducible IDENTICALLY every time,
			so follow these rules:

			1. Keep the outfit SIMPLE. Simple outfits are reproduced consistently; complex
			   ones drift between images.
			2. You will be told WHICH outfit concept to design. Use THAT exact concept — do not
			   change it to a different one. It is a well-known outfit, so render it in its most
			   recognizable, standard, iconic form (this makes it reproduce consistently).
			3. Use a SINGLE color, or at most TWO colors, each given as a HEX code, and say
			   what each color is for. Make every part a single solid color, with no
			   unspecified white areas.
			4. State the overall feel/mood in 1-2 words.
			5. Give 2-3 decisive details. For EACH detail element, state its exact, unambiguous
			   position and count so every render places it identically, written generically as
			   "the [detail element] is located at [position]" (for example,
			   "the [element] is centered on the front", "there is exactly one [element] on the
			   left side"). Do NOT assume any specific garment parts; describe whatever this
			   outfit actually has. Prefer symmetric, centered, clearly-located elements. Avoid
			   anything whose position can vary, fine gradients, intricate textures, or more
			   than three details.
			6. This fish has NO arms and NO legs. Design only clothing worn flat on its body,
			   with NO sleeves and nothing that would require arms, hands, legs, or feet.
			7. ALWAYS state how the garment covers the body in 3D, especially the BACK. Say
			   explicitly whether it wraps fully around and covers the back, or is front-only
			   with the back left bare, and where it fastens — e.g., "wraps fully around the
			   body, back covered" or "front-only apron, the back is left bare, tied with two
			   straps at the back". This removes ambiguity so every render covers the same areas.

			Describe ONLY the outfit. Do NOT describe or change the fish, its face, pose,
			line style, or background.

			Output EXACTLY these two plain-text lines and NOTHING else:
			NAME: <a short 1-4 word outfit name in Korean>
			SPEC: <one compact English description, 60 words MAX, that an image model can
			follow. Order it as: concept first, then the 1-2 HEX colors and what each is for,
			then the 2-3 key details with their exact positions, and how it covers the body
			(front-only with bare back, or fully wrapped). Be concrete but brief.>
			""";

	// 사용자 메시지 — %s 자리에 코드가 뽑은 컨셉 키워드가 들어간다.
	private static final String USER_TEMPLATE =
			"Design a \"%s\" for the fish mascot. Elaborate THIS exact concept into a precise, "
			+ "reproducible SPEC. Render it in its most recognizable, standard form.";

	/**
	 * 옷 컨셉 카탈로그 — 이미지 모델이 사전지식을 많이 가진 '유명한 옷' 위주.
	 * 유명할수록 4장이 같은 형태로 수렴(통일성↑)한다. 매 뽑기마다 여기서 코드가 랜덤으로 1개를 고른다.
	 * (LLM에게 "다양하게 골라"라고 맡기면 편향되므로 선택은 코드가 한다.)
	 */
	private static final List<String> OUTFIT_CONCEPTS = List.of(
			"yellow raincoat", "hoodie", "dinosaur onesie pajamas", "Santa suit", "Korean hanbok",
			"school uniform", "denim overalls", "superhero cape", "princess dress", "knit sweater",
			"sailor uniform", "chef's outfit", "wizard robe", "pirate costume", "astronaut suit",
			"cowboy outfit", "ninja outfit", "graduation gown", "winter puffer jacket", "bee costume",
			"pumpkin costume", "bear onesie", "Japanese kimono", "hooded poncho", "tuxedo",
			"police uniform", "firefighter coat", "soccer jersey", "angel robe");

	/**
	 * 옷 설계. 카탈로그에서 컨셉을 랜덤으로 뽑아 디자이너에게 정밀화시킨 뒤, 옷 이름(NAME)·스펙(SPEC)을 반환한다.
	 * (굴비 이미지는 색 편향 때문에 보내지 않고 텍스트 지시만으로 설계한다.)
	 *
	 * @return 옷 이름 + 옷 스펙. 스펙을 못 뽑으면 outfitSpec=null.
	 */
	public OutfitDesign design() {
		// 컨셉은 코드가 카탈로그에서 균등 랜덤으로 선택 → 변종 다양 + 특정 옷 편향 방지
		String concept = OUTFIT_CONCEPTS.get(ThreadLocalRandom.current().nextInt(OUTFIT_CONCEPTS.size()));
		log.debug("[designer] picked concept={}", concept);

		String text = chatClient.prompt()
				.system(SYSTEM)
				.user(USER_TEMPLATE.formatted(concept))
				.call()
				.content();

		String name = parseTagged(text, "NAME");
		String spec = parseTagged(text, "SPEC");
		if (spec == null && text != null && !text.isBlank()) {
			spec = text.trim();		// 형식 안 지키면 통째로 스펙으로 사용
		}
		log.debug("[designer] name={}, spec={}", name, spec);
		return new OutfitDesign(name, spec);
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

	// 결과: 옷 이름(한글) + 옷 스펙(영문). 스펙은 이미지 모델에 그대로 전달된다.
	public record OutfitDesign(String outfitName, String outfitSpec) {}
}
