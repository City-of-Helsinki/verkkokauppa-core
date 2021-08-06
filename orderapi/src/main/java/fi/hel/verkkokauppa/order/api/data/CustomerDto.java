package fi.hel.verkkokauppa.order.api.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto {

    @NotBlank(message = "firstname required")
    private String customerFirstName;

    @NotBlank(message = "lastname required")
    private String customerLastName;

    @Email(message = "email must be in correct format")
    @NotBlank(message = "email required")
    private String customerEmail;
}
