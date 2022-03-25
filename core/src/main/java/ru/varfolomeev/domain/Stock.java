package ru.varfolomeev.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Stock {
    private String companyName;
    private double price;
    private long amount;
}
