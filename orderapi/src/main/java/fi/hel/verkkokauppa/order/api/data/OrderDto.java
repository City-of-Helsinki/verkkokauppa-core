package fi.hel.verkkokauppa.order.api.data;

import java.util.ArrayList;
import java.util.List;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

    private String orderId;

    private String namespace;

    private String user;

    private String createdAt;

    private String status;

    private String type;

    @NotBlank(message = "firstname required")
    private String customerFirstName;

    @NotBlank(message = "lastname required")
    private String customerLastName;

    @Email(message = "email must be in correct format")
    @NotBlank(message = "email required")
    private String customerEmail;
}