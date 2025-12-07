package za.co.capitecbank.assessment.mapper;

import org.mapstruct.Mapper;
import za.co.capitecbank.assessment.api.model.AggregatedTransaction;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper
public interface AggregatedTransactionResponseMapper {
    AggregatedTransactionResponseMapper INSTANCE = Mappers.getMapper(AggregatedTransactionResponseMapper.class);
    List<AggregatedTransaction> mapTransactionsFromDB(List<za.co.capitecbank.assessment.domain.entity.AggregatedTransaction> transaction);
    AggregatedTransaction mapTransactionFromDB(za.co.capitecbank.assessment.domain.entity.AggregatedTransaction transaction);

    default OffsetDateTime map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
