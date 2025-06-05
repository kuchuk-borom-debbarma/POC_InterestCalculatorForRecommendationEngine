package dev.kuku.interestcalculator.config;

import dev.kuku.interestcalculator.config.userTopic.UserTopicScoreAccumulatorConfig;
import dev.kuku.interestcalculator.config.userTopic.UserTopicScoreDecayConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "user-topic-score")
public class UserTopicScoreConfiguration {
    /**
     * Minimum allowed topic score value
     */
    private double topicScoreMin = 0.0;

    /**
     * Maximum allowed topic score value
     */
    private double topicScoreMax = 10.0;

    private UserTopicScoreAccumulatorConfig accumulator = new UserTopicScoreAccumulatorConfig();
    private UserTopicScoreDecayConfig decay = new UserTopicScoreDecayConfig();
}
