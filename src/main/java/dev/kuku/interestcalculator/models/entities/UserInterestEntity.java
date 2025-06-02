package dev.kuku.interestcalculator.models.entities;

import org.yaml.snakeyaml.util.Tuple;

import java.util.Map;

///  userId -> topics & score, timestamp
public record UserInterestEntity(String userId, Map<String, Tuple<Long, Long>> topics) {
}
