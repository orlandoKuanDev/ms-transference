package com.example.mstransference.model.dto;

import com.example.mstransference.model.Bill;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class TransferenceCreateDTO {
    private Double amount;
    private String description;
    private String mode;
    private Bill bill;
}
