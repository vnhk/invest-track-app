package com.bervan.investtrack.service;

import com.bervan.common.service.OpenAIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class ReceiptScanningService {
    private static final Logger log = LoggerFactory.getLogger(ReceiptScanningService.class);
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    @Value("${openai.api.key}")
    private String apiKey;

    public ReceiptScanningService() {
        this.openAIService = new OpenAIService("You are an expert receipt parser and financial category classifier.");
    }

    public List<ParsedReceiptEntry> scanReceipt(String base64Image, List<String> availableCategories) throws IOException {
        String mimeType = "image/jpeg";
        String base64Data = base64Image;

        if (base64Image.startsWith("data:")) {
            int commaIndex = base64Image.indexOf(",");
            if (commaIndex != -1) {
                String prefix = base64Image.substring(0, commaIndex);
                if (prefix.contains(":") && prefix.contains(";")) {
                    mimeType = prefix.substring(prefix.indexOf(":") + 1, prefix.indexOf(";"));
                }
                base64Data = base64Image.substring(commaIndex + 1);
            }
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thumbnails.of(new ByteArrayInputStream(imageBytes))
                .size(900, 900)
                .outputFormat("jpg")
                .outputQuality(0.8)
                .keepAspectRatio(true)
                .toOutputStream(baos);

        byte[] compressedBytes = baos.toByteArray();

        String prompt = "Analyze the attached receipt image and extract the transactions.\n" +
                "Group the purchased items logically into one or more budget entries.\n" +
                "Try to classify each group into one of the existing categories: " + availableCategories.toString() + ".\n" +
                "If an item (like a grill, electronics, clothing) clearly belongs to a different category than food/groceries, SPLIT it into a separate entry with its own price and a suitable category.\n" +
                "The sum of all entries must equal the total amount on the receipt.\n" +
                "Return the result STRICTLY as a JSON array of objects.\n" +
                "Do NOT wrap the JSON in ```json markdown code blocks. Return ONLY valid, minified raw JSON.\n" +
                "Each object in the array must have the following fields:\n" +
                " - 'name': String (descriptive name of the purchase/group, e.g. 'Biedronka - Groceries' or 'Biedronka - Grill')\n" +
                " - 'category': String (must be one of the existing categories if it matches, otherwise a new precise category)\n" +
                " - 'value': Double/BigDecimal (positive value, representing the cost)\n" +
                " - 'currency': String (e.g., 'PLN', 'EUR', 'USD')\n" +
                " - 'date': java LocalDate\n" +
                " - 'notes': String (list some of the key items included, or specific item details)\n" +
                "Make sure the currency matches the receipt's currency. If you don't see currency assume it's PLN.";

        String compressedBase64 = Base64.getEncoder().encodeToString(compressedBytes);
        log.info("Sending receipt image to OpenAI for analysis...");
        String response = openAIService.askAIWithImage(prompt, compressedBase64, mimeType, OpenAIService.GPT_4O_MINI, 0.1, apiKey);

        if (response == null) {
            log.error("Failed to get response from OpenAI or API key is not configured.");
            return Collections.emptyList();
        }

        log.info("OpenAI response successfully received.");

        // Clean response if markdown code block was returned despite instructions
        String cleanedResponse = response.trim();
        if (cleanedResponse.startsWith("```")) {
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            } else {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();
        }

        try {
            ParsedReceiptEntry[] entries = objectMapper.readValue(cleanedResponse, ParsedReceiptEntry[].class);
            return Arrays.asList(entries);
        } catch (Exception e) {
            log.error("Failed to parse JSON response from OpenAI: {}", response, e);
            return Collections.emptyList();
        }
    }

    @Getter
    @Setter
    public static class ParsedReceiptEntry {
        private String name;
        private String category;
        private BigDecimal value;
        private LocalDate date;
        private String currency;
        private String notes;

        public ParsedReceiptEntry() {
        }

    }
}
