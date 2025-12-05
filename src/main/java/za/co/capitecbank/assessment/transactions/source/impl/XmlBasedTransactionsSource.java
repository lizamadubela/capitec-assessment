package za.co.capitecbank.assessment.transactions.source.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;
import za.co.capitecbank.assessment.transactions.source.TransactionSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class XmlBasedTransactionsSource implements TransactionSource {

    private final Resource xmlFile;

    public XmlBasedTransactionsSource(@Value("${app.xml-data-file}") Resource xmlFile) {
        this.xmlFile = xmlFile;
    }

    @Override
    public List<RawTransaction> fetchTransactions() {
        List<RawTransaction> rawTransactions = new ArrayList<>();

        try (InputStream inputStream = xmlFile.getInputStream()) {
            // Read and trim content to remove any leading whitespace/BOM
            String xmlContent = new String(inputStream.readAllBytes()).trim();

            // Remove BOM if present
            if (xmlContent.startsWith("\uFEFF")) {
                xmlContent = xmlContent.substring(1);
            }

            // Parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

            // Get all transaction elements
            NodeList transactionNodes = document.getElementsByTagName("transaction");

            for (int i = 0; i < transactionNodes.getLength(); i++) {
                Element transactionElement = (Element) transactionNodes.item(i);
                RawTransaction rawTransaction = parseTransaction(transactionElement);
                if (rawTransaction != null) {
                    rawTransactions.add(rawTransaction);
                }
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to read or parse XML file: " + xmlFile.getFilename(), ex);
        }

        return rawTransactions;
    }

    private RawTransaction parseTransaction(Element element) {
        try {
            String customerId = getElementText(element, "customerId", "");
            String description = getElementText(element, "description", "");
            String source = getElementText(element, "channel", "XML_SOURCE");

            // Parse amount
            BigDecimal amount = BigDecimal.ZERO;
            String amountStr = getElementText(element, "amount", "0");
            try {
                amount = new BigDecimal(amountStr.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid amount format: " + amountStr);
            }

            // Parse timestamp
            LocalDateTime timestamp = LocalDateTime.now();
            String timestampStr = getElementText(element, "timestamp", "");
            if (!timestampStr.isEmpty()) {
                try {
                    // Try ISO format first
                    timestamp = LocalDateTime.parse(timestampStr.trim());
                } catch (DateTimeParseException e1) {
                    try {
                        // Try custom format
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        timestamp = LocalDateTime.parse(timestampStr.trim(), formatter);
                    } catch (Exception e2) {
                        System.err.println("Invalid timestamp format: " + timestampStr);
                    }
                }
            }

            return new RawTransaction(customerId, description, amount, timestamp, source);

        } catch (Exception e) {
            System.err.println("Error parsing transaction element: " + e.getMessage());
            return null;
        }
    }

    private String getElementText(Element parent, String tagName, String defaultValue) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            String text = nodeList.item(0).getTextContent();
            return text != null ? text.trim() : defaultValue;
        }
        return defaultValue;
    }
}