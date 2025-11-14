package com.athengaudio.backend.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

@Service
public class BotService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private Client client;

    // System Instruction định hình tính cách cho Bot
    private static final String SYSTEM_INSTRUCTION = "Bạn là trợ lý ảo CSKH của 'Atheng Audio' - Cửa hàng chuyên cung cấp tai nghe và loa chính hãng.\n"
            + "THÔNG TIN CỬA HÀNG:\n"
            + "- Địa chỉ: 160 Trung Phụng, Đống Đa, Hà Nội.\n"
            + "- Hotline: 1900 1234.\n"
            + "- Website: athengaudio.com\n"
            + "- Chính sách: Bảo hành chính hãng 12 tháng, lỗi 1 đổi 1 trong 30 ngày đầu.\n\n"
            + "NHIỆM VỤ CỦA BẠN:\n"
            + "1. Trả lời ngắn gọn, thân thiện, xưng hô 'mình' và gọi khách là 'bạn'.\n"
            + "2. Chỉ hỗ trợ các vấn đề về sản phẩm âm thanh, tư vấn mua hàng, bảo hành, địa chỉ shop.\n"
            + "3. Nếu khách hỏi vấn đề không liên quan (code, toán, thời tiết...), hãy khéo léo từ chối.\n"
            + "4. Nếu khách muốn gặp người thật, hãy hướng dẫn họ chuyển sang chế độ 'Gặp nhân viên'.";

    // Method chính để gọi AI
    public String generateContent(String userMessage) {
        try {
            // Khởi tạo client nếu chưa có (Singleton scope)
            if (this.client == null) {
                this.client = Client.builder()
                        .apiKey(geminiApiKey)
                        .build();
            }

            // 1. Tạo nội dung System Instruction
            Content systemContent = Content.builder()
                    .parts(Collections.singletonList(
                            Part.builder().text(SYSTEM_INSTRUCTION).build()))
                    .build();

            // 2. Cấu hình (Config)
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(systemContent)
                    .temperature(0.7f) // Độ sáng tạo
                    .build();

            // 3. Gọi API với model "gemini-2.5-flash"
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash", // <--- CẬP NHẬT MODEL TẠI ĐÂY
                    userMessage,
                    config);

            return response.text();

        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hiện tại hệ thống AI đang bận. Bạn vui lòng thử lại sau nhé! (" + e.getMessage() + ")";
        }
    }

    // --- Các method phụ hỗ trợ Controller ---

    public String getBotResponse(String userMessage) {
        return generateContent(userMessage);
    }

    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Google GenAI SDK");
        info.put("model", "gemini-2.5-flash"); // Cập nhật thông tin model
        info.put("status", "Online");
        return info;
    }

    public long getCacheSize() {
        return 0;
    }

    public void clearCache() {
        // Mock method
    }
}