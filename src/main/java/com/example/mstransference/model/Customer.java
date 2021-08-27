package com.example.mstransference.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Customer {
    @Field(name = "customerIdentityType")
    private String customerIdentityType;
    @Field(name = "customerIdentityNumber")
    private String customerIdentityNumber;
    @Size(max = 40)
    @Field(name = "name")
    private String name;
}
