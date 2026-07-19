package com.rally.home.model;

import lombok.Data;

import java.util.List;

@Data
public class HomePageDTO {
    private List<HomeDisplayItemDTO> displayItems;
}
