package fi.hel.verkkokauppa.order.model.voucher;

import lombok.Data;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "reservedvouchercodes")
@Data
@Setter
public class ReservedVoucherCode {
    @Id
    String code;

    @Field(
            type = FieldType.Date,
            format = {},
            pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'"
    )
    LocalDateTime createdAt;
}
