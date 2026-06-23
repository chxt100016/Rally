package com.rally.domain.tour.model;

import lombok.Data;

@Data
public class PlayerQueryVO {
    private String id;
    private Integer rank;
    private String name;
    private CountryVO country;
    private Integer points;
    private Integer age;
    private String birthDate;
}
