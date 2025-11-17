package com.medicine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class OpenAIService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.max-tokens}")
    private Integer maxTokens;

    @Value("${openai.temperature}")
    private Double temperature;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * 식단 이미지를 OpenAI Vision API로 분석
     *
     * @param imageBase64 Base64로 인코딩된 이미지
     * @return AI 평가 응답 (점수와 평가 내용 포함)
     */
    public Map<String, Object> analyzeMealImage(String imageBase64) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 프롬프트 생성
            String prompt = "1965년생 남성이 스텐트 3개를 시술한상태이고 LDL콜레스톨이 높아 관리가 필요한 상황에서 이 음식을 먹었을때 건강에 영향을 줄지 판단해주고 이 음식에대한 점수를 메겨줘(100점이 만점) 답변은 다른이야기없이 딱 아래의 템플릿대로 해줘 어투는 ~함 으로 답변해줘\n" +
                    "그리고 만약 이미지에서 음식을 못찾거나 음식을 특정할 수 없을경우는 NO 라고 응답을 줘\n" +
                    "-템플릿(예시)\n" +
                    "점수: 40점\n" +
                    "음식: 매운 삼계탕\n" +
                    "평가내용:\n" +
                    "고지방·고나트륨·고콜레스테롤 식품으로 분류됨. 스텐트 시술 후 고LDL 콜레스테롤 관리가 필요한 환자에게 부적절함. 닭고기는 양질의 단백질을 제공함에도 불구하고, 국물의 기름기와 조미료, 소금 함량이 높아 혈중 지질 수치 상승 및 혈관 건강 악화 위험을 초래함. 국물을 제한하고 살코기 위주로 소량 섭취하는 것이 권장됨.";

            // 요청 바디 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_completion_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            // 메시지 생성
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");

            List<Map<String, Object>> content = new ArrayList<>();

            // 텍스트 부분
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", prompt);
            content.add(textPart);

            // 이미지 부분
            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            Map<String, String> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);
            imagePart.put("image_url", imageUrl);
            content.add(imagePart);

            message.put("content", content);
            messages.add(message);

            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Sending request to OpenAI Vision API");

            // API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.info("OpenAI API response status: {}", response.getStatusCode());

            // 응답 파싱
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> messageResponse = (Map<String, Object>) choice.get("message");
                    String aiResponse = (String) messageResponse.get("content");

                    log.info("OpenAI response: {}", aiResponse);

                    // 응답에서 점수와 평가 추출
                    return parseAIResponse(aiResponse);
                }
            }

            log.error("Failed to get valid response from OpenAI API");
            return createErrorResponse();

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return createErrorResponse();
        }
    }

    /**
     * AI 응답 파싱
     */
    private Map<String, Object> parseAIResponse(String response) {
        Map<String, Object> result = new HashMap<>();

        try {
            // NO 응답 체크 (이미지에서 음식을 찾을 수 없는 경우)
            if (response.trim().toUpperCase().equals("NO") ||
                response.trim().toUpperCase().startsWith("NO")) {
                result.put("success", false);
                result.put("score", 0);
                result.put("evaluation", "사진에서 음식을 찾을 수 없습니다.");
                result.put("fullResponse", "사진에서 음식을 찾을 수 없습니다.");
                log.info("AI detected NO food in image");
                return result;
            }

            // 점수 추출 (정규표현식 사용)
            String scoreLine = "";
            String evaluationLine = "";

            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("점수:") || line.contains("점수 :")) {
                    scoreLine = line;
                } else if (line.contains("평가:") || line.contains("평가 :")) {
                    evaluationLine = line;
                }
            }

            // 점수 파싱
            int score = 0; // 기본값
            if (!scoreLine.isEmpty()) {
                String scoreStr = scoreLine.replaceAll("[^0-9]", "");
                if (!scoreStr.isEmpty()) {
                    score = Integer.parseInt(scoreStr);
                    score = Math.max(0, Math.min(100, score)); // 0-100 범위로 제한
                }
            }

            // 평가 내용 추출
            String evaluation = response.trim();
            if (!evaluationLine.isEmpty()) {
                evaluation = evaluationLine.replace("평가:", "").replace("평가 :", "").trim();
            }

            result.put("success", true);
            result.put("score", score);
            result.put("evaluation", evaluation);
            result.put("fullResponse", response);

            log.info("Parsed AI response - Score: {}, Evaluation: {}", score, evaluation);

        } catch (Exception e) {
            log.error("Error parsing AI response", e);
            return createErrorResponse();
        }

        return result;
    }

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("score", 50);
        result.put("evaluation", "AI 분석에 실패했습니다.");
        return result;
    }
}
