package dev.kuku.interestcalculator.dto;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
//Its ok to let this be singleton, as it's value gets replaced any ways for now
public class OperationDetailMap {
    public Map<String, Object> operationDetailMap = new HashMap<>();
}