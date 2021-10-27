package fi.hel.verkkokauppa.common.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * This class holds a list of {@code ErrorModel} that describe the error raised on rejected validation
 * @author ROUSSI Abdelghani
 * https://www.linkedin.com/pulse/spring-boot-handling-exceptionserrors-restful-api-abdelghani-roussi
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private List<ErrorModel> errorMessage;

}