package com.example.mstransference.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transference {
    @Field(name = "amount")
    private Double amount;

    @Field(name = "description")
    private String description;

    @Field(name = "origen")
    private Acquisition origen;

    @Field(name = "destine")
    private Acquisition destine;

}
