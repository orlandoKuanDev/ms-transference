package com.example.mstransference.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateDepositWithCardDTO {
    private String cardNumber;
    private Double amount;
    private String description;
    private String accountNumber;
}