package com.athengaudio.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BotService {

    // Lấy API Key từ file application.properties
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // URL của Gemini 1.5 Flash (nhanh và ổn định cho chatbot)
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    public String getBotResponse(String userMessage) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 1. Tạo Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Tạo Body JSON
            Map<String, Object> body = new HashMap<>();

            // === SYSTEM INSTRUCTION (Huấn luyện Bot) ===
            // Phần này định hình tính cách và kiến thức cho Bot
            Map<String, Object> systemInstruction = new HashMap<>();
            Map<String, Object> systemParts = new HashMap<>();
            systemParts.put("text",
                    "Bạn là trợ lý ảo CSKH của 'Atheng Audio' - Cửa hàng chuyên cung cấp tai nghe và loa chính hãng.\n"
                            +
                            "THÔNG TIN CỬA HÀNG:\n" +
                            "- Địa chỉ: 160 Trung Phụng, Đống Đa, Hà Nội.\n" +
                            "- Hotline: 1900 1234.\n" +
                            "- Website: athengaudio.com\n" +
                            "- Chính sách: Bảo hành chính hãng 12 tháng, lỗi 1 đổi 1 trong 30 ngày đầu.\n\n" +
                            "NHIỆM VỤ CỦA BẠN:\n" +
                            "1. Trả lời ngắn gọn, thân thiện, xưng hô 'mình' và gọi khách là 'bạn'.\n" +
                            "2. Chỉ hỗ trợ các vấn đề về sản phẩm âm thanh, tư vấn mua hàng, bảo hành, địa chỉ shop.\n"
                            +
                            "3. Nếu khách hỏi vấn đề không liên quan (code, toán, thời tiết...), hãy khéo léo từ chối: 'Xin lỗi, mình chỉ là trợ lý ảo của Atheng Audio nên chỉ hỗ trợ được các vấn đề về thiết bị âm thanh thôi ạ.'\n"
                            +
                            "4. Nếu khách muốn gặp người thật, hãy hướng dẫn họ chuyển sang chế độ 'Gặp nhân viên' trên khung chat.");
            systemInstruction.put("parts", List.of(systemParts));
            body.put("system_instruction", systemInstruction);

            // === USER CONTENT (Câu hỏi của người dùng) ===
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            Map<String, Object> userParts = new HashMap<>();
            userParts.put("text", userMessage);
            userContent.put("parts", List.of(userParts));

            body.put("contents", List.of(userContent));

            // 3. Gửi Request đến Google
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = GEMINI_URL + "?key=" + geminiApiKey;

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // 4. Xử lý kết quả trả về
            return extractTextFromResponse(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hiện tại kết nối đến máy chủ AI đang bị gián đoạn. Bạn vui lòng thử lại sau hoặc liên hệ hotline 1900 1234 nhé!";
        }
    }

    // Hàm phụ để lấy text từ JSON phức tạp của Gemini
    private String extractTextFromResponse(Map<String, Object> responseBody) {
        try {
            if (responseBody == null)
                return "Không nhận được phản hồi.";

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            return "Lỗi khi đọc phản hồi từ AI.";
        }
        return "Bot không có câu trả lời.";
    }
}