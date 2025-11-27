package za.co.capitecbank.assessment.service;

import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.RawTransaction;
import za.co.capitecbank.assessment.domain.Transaction;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TxCategorizationEngineImpl implements TxCategorizationEngine {


    @Override
    public Transaction categorize(RawTransaction raw, String source) {
        String desc = raw.getDescription() == null ? "" : raw.getDescription().toLowerCase();
        String category;


        if (desc.contains("grocery") || desc.contains("spar") || desc.contains("supermarket")) {
            category = "Food";
        } else if (desc.contains("uber") || desc.contains("taxi") || desc.contains("ride")) {
            category = "Transport";
        } else if (desc.contains("netflix") || desc.contains("spotify") || desc.contains("subscription")) {
            category = "Entertainment";
        } else if (desc.contains("salary") || raw.getAmount().compareTo(BigDecimal.ZERO) > 0 && desc.contains("salary")) {
            category = "Income";
        } else if (desc.contains("coffee") || desc.contains("starbucks")) {
            category = "Food";
        }else if (desc.contains("Caltex") || desc.contains("BP Garage")) {
            category = "Fuel";
        } else {
            category = "Other";
        }


        return new Transaction(UUID.randomUUID().toString(),raw.getCustomerId(), raw.getAmount(), raw.getTimestamp(), raw.getDescription(), category, source);
    }
}

