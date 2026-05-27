package com.bervan.investtrack.service;

import com.bervan.common.service.BaseScanningService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class ReceiptScanningService extends BaseScanningService {
    private static final Logger log = LoggerFactory.getLogger(ReceiptScanningService.class);

    public ReceiptScanningService(@Value("${openai.api.key}") String apiKey) {
        super("You are an expert receipt parser and financial category classifier.", apiKey);
    }


    public List<ParsedReceiptEntry> scanReceipt(String base64Image, List<String> availableCategories) throws IOException {
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

        log.info("Sending receipt image to OpenAI for analysis...");
        String response = super.askAIWithImage(base64Image, prompt);

        if (response == null) {
            log.error("Failed to receive a response from OpenAI.");
            return Collections.emptyList();
        }

        log.info("OpenAI receipt response successfully received.");


        try {
            ParsedReceiptEntry[] entries = objectMapper.readValue(response, ParsedReceiptEntry[].class);
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
