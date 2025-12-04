package com.athengaudio.backend.service;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.athengaudio.backend.model.Product;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

@Service
public class BotService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private ProductService productService;

    private Client client;

    // --- PHẦN "NÃO BỘ" CỦA BOT ---
    private static final String CORE_INSTRUCTION = "VAI TRÒ: Bạn là trợ lý ảo thông minh của 'AthengAudio'.\n" +
            "PHONG CÁCH: Nhiệt tình, ngắn gọn, hữu ích.\n\n" +

            "QUY TẮC XỬ LÝ YÊU CẦU (QUAN TRỌNG):\n" +

            "1. TÌM KIẾM THÔNG MINH & VIẾT TẮT:\n" +
            "   - Nếu khách nhập từ khóa ngắn hoặc viết tắt (vd: 'so' -> Sony, 'mar' -> Marshall, 'jbl', 'tai nghe dây'), hãy tự động hiểu và tìm sản phẩm tương ứng trong danh sách.\n"
            +
            "   - Nếu không tìm thấy sản phẩm chính xác 100%, hãy tìm sản phẩm có tên gần giống nhất và hỏi: 'Có phải bạn đang tìm [Tên sản phẩm gợi ý] không?' rồi hiển thị thông tin sản phẩm đó.\n"
            +
            "   - Nếu khách hỏi về một loại tai nghe cụ thể (vd: 'tai nghe bluetooth', 'tai nghe chụp tai'), hãy lọc và giới thiệu các mẫu phù hợp nhất.\n\n"
            +

            "2. KHÁCH HỎI CHUNG CHUNG (DANH MỤC):\n" +
            "   - Nếu khách nói muốn xem 'tai nghe', 'loa', 'sản phẩm mới', 'loa bluetooth'... mà không chỉ đích danh mẫu nào:\n"
            +
            "     + Bước 1: Gửi lời dẫn kèm link: 'Dạ, dưới đây là các mẫu [Tên danh mục] bên mình đang có ạ. Bạn có thể xem đầy đủ tại: <a href=\"http://localhost:4200/products\" target=\"_blank\" style=\"color:#667eea;text-decoration:underline;\">Cửa hàng AthengAudio</a>'\n"
            +
            "     + Bước 2: Chọn ra 2-3 sản phẩm tiêu biểu nhất của danh mục đó từ dữ liệu và hiển thị dưới dạng thẻ (như quy tắc 3) để khách tham khảo ngay.\n\n"
            +

            "3. ĐỊNH DẠNG HIỂN THỊ SẢN PHẨM (BẮT BUỘC DÙNG HTML NÀY):\n" +
            "   Khi giới thiệu sản phẩm cụ thể (dù là tìm kiếm hay gợi ý), LUÔN dùng mẫu thẻ ngang sau:\n" +
            "     <div style='margin: 8px 0; padding: 12px; background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); display: flex; gap: 12px; align-items: center;'>"
            +
            "       <img src='{LINK_ANH}' style='width: 80px; height: 80px; object-fit: cover; border-radius: 8px; flex-shrink: 0;'>"
            +
            "       <div style='flex: 1; min-width: 0;'>" + // min-width:0 giúp text không bị tràn flex container
            "         <div style='font-weight: bold; color: #2d3748; font-size: 14px; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'>{TEN_SAN_PHAM}</div>"
            +
            "         <div style='color: #e53e3e; font-weight: bold; font-size: 13px;'>{GIA_TIEN}</div>" +
            "         <div style='font-size: 11px; color: #718096; margin-bottom: 6px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;'>{MO_TA_NGAN_GON}</div>"
            +
            "         <a href='{LINK_CHI_TIET}' target='_blank' style='display: inline-block; background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 4px 10px; text-decoration: none; border-radius: 6px; font-size: 11px; font-weight: 600;'>Xem ngay ➔</a>"
            +
            "       </div>" +
            "     </div>\n\n" +

            "4. GIỚI HẠN KIẾN THỨC:\n" +
            "   - Chỉ tư vấn dựa trên 'DỮ LIỆU SẢN PHẨM' bên dưới. Không bịa đặt thông tin.\n" +
            "   - Nếu khách hỏi sản phẩm không có trong dữ liệu: 'Dạ hiện mẫu đó AthengAudio chưa kinh doanh ạ'.\n" +

            "KIẾN THỨC CỐ ĐỊNH KHÁC:\n" +
            "- Địa chỉ: 140 Trung Phụng, Đống Đa, HN.\n" +
            "- Hotline: 0919 76 45 42.\n";

    public String generateContent(String userMessage) {
        try {
            if (this.client == null) {
                this.client = Client.builder()
                        .apiKey(geminiApiKey)
                        .build();
            }

            // 1. Nạp dữ liệu sản phẩm mới nhất từ Database vào ngữ cảnh
            String productContext = getProductDataAsString();

            // 2. Ghép Instruction + Dữ liệu
            String fullSystemInstruction = CORE_INSTRUCTION + "\n\n" + productContext;

            // 3. Tạo Content
            Content systemContent = Content.builder()
                    .parts(Collections.singletonList(
                            Part.builder().text(fullSystemInstruction).build()))
                    .build();

            // 4. Cấu hình (Temperature thấp để Bot bám sát dữ liệu)
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(systemContent)
                    .temperature(0.4f)
                    .build();

            // 5. Gọi Gemini (Dùng model ổn định 2.5-flash)
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    userMessage,
                    config);

            return response.text();

        } catch (Exception e) {
            e.printStackTrace();
            return "Hic, server AI đang quá tải một chút. Bạn đợi mình vài giây rồi hỏi lại nhé! 🤖";
        }
    }

    // Hàm biến đổi danh sách sản phẩm thành văn bản để "dạy" cho Bot
    private String getProductDataAsString() {
        List<Product> products = productService.getAllProducts();

        if (products.isEmpty()) {
            return "DỮ LIỆU SẢN PHẨM: Hiện tại kho đang trống, chưa có sản phẩm nào.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== DỮ LIỆU SẢN PHẨM CỦA ATHENGAUDIO (Chỉ tư vấn trong danh sách này) ===\n");

        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

        for (Product p : products) {
            String productLink = "http://localhost:4200/products/" + p.getId();

            // Xử lý link ảnh: Đảm bảo luôn là đường dẫn tuyệt đối
            String imageUrl = "assets/images/default-product.png";
            if (p.getImage() != null && !p.getImage().isEmpty()) {
                if (p.getImage().startsWith("http")) {
                    imageUrl = p.getImage();
                } else {
                    imageUrl = "http://localhost:8080" + p.getImage();
                }
            }

            String priceStr = currencyFormatter.format(p.getPrice());
            String stockStatus = (p.getStock() != null && p.getStock() > 0) ? "Còn hàng" : "Hết hàng";

            // Cấu trúc dữ liệu nạp cho Bot (Thêm Category để Bot phân loại tốt hơn)
            sb.append(String.format("Product ID: %s\n", p.getId()));
            sb.append(String.format("- Tên: %s\n", p.getName()));
            sb.append(String.format("- Danh mục: %s\n", p.getCategory())); // Quan trọng để Bot lọc Loa/Tai nghe
            sb.append(String.format("- Thương hiệu: %s\n", p.getBrand()));
            sb.append(String.format("- Giá bán: %s\n", priceStr));
            sb.append(String.format("- Tình trạng: %s\n", stockStatus));
            sb.append(String.format("- Đặc điểm/Mô tả: %s\n", p.getDescription()));
            sb.append(String.format("- Link ảnh (dùng để hiển thị): %s\n", imageUrl));
            sb.append(String.format("- Link chi tiết (để khách mua): %s\n", productLink));
            sb.append("-----------------------------------\n");
        }

        return sb.toString();
    }

    // --- Các method phụ giữ nguyên ---
    public String getBotResponse(String userMessage) {
        return generateContent(userMessage);
    }

    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "AthengAudio AI Context-Aware");
        info.put("model", "gemini-2.5-flash");
        return info;
    }

    public long getCacheSize() {
        return 0;
    }

    public void clearCache() {
    }
}