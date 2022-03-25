package ru.varfolomeev.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
public class Client {
    private final long id;
    private double money = 0;
    private final Map<String, Long> stocks = new HashMap<>();
}
