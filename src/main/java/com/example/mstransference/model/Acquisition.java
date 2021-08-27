package com.example.mstransference.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Acquisition {

    private Product product;

    private List<Customer> customerHolder;

    private List<Customer> customerAuthorizedSigner;

    private String iban;

    private Bill bill;
}