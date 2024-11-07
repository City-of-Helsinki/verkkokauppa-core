package fi.hel.verkkokauppa.common.elastic.search.dto;

import lombok.Data;

@Data()
public class SearchResultDto {
    private String id;
    private String title;
    private String description;
    private Double price;
    // Add other fields as needed

    // Getters and Setters
}
