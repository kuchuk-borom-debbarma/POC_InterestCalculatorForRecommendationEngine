package dev.kuku.interestcalculator.services.userInterest;

import dev.kuku.interestcalculator.Db;
import dev.kuku.interestcalculator.models.ActivityLevel;
import dev.kuku.interestcalculator.models.InteractionData;
import dev.kuku.interestcalculator.models.UserInteractionData;
import dev.kuku.interestcalculator.models.entities.ContentTopic;
import dev.kuku.interestcalculator.models.entities.PostEntity;
import dev.kuku.interestcalculator.models.entities.UserInterestEntity;
import dev.kuku.interestcalculator.repo.UserInteractionRepo;
import dev.kuku.interestcalculator.repo.UserInterestRepo;
import dev.kuku.interestcalculator.services.ai.LLMService;
import dev.kuku.interestcalculator.services.platform.PlatformService;
import dev.kuku.interestcalculator.services.topic.TopicService;
import dev.kuku.interestcalculator.services.userActivity.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserInterestService {
    private final Db db;
    private final UserInteractionRepo userInterestRepository;
    private final UserInterestRepo userInterestRepo;
    private final PlatformService platformService;
    private final LLMService llmService;
    private final UserActivityService userActivityService;
    private final List<PostEntity> posts = new ArrayList<>();
    private final Set<String> contentTopics = new HashSet<>();
    private final TopicService topicService;


    private static int getDecayedScore(long currentTime, Long lastUpdatedTimestamp, Integer score) {
        long timeElapsed = currentTime - lastUpdatedTimestamp;
        long daysElapsed = timeElapsed / (24 * 60 * 60 * 1000);
        // Apply decay factor based on elapsed time as per README
        double decayFactor = -1.0; // Default: no decay
        if (daysElapsed <= 7) {
            // Recent interactions (0-7 days): No decay (1.0x)
            decayFactor = 1.0;
        } else if (daysElapsed <= 28) {
            // Moderate age (1-4 weeks): Slight decay (0.8-0.9x)
            // Linear interpolation between 0.9 and 0.8
            decayFactor = 0.9 - ((daysElapsed - 7) / 21.0) * 0.1;
        } else if (daysElapsed <= 180) {
            // Older interactions (1-6 months): Significant decay (0.3-0.7x)
            // Linear interpolation between 0.7 and 0.3
            decayFactor = 0.7 - ((daysElapsed - 28) / 152.0) * 0.4;
        } else {
            // Historical data (6+ months): Minimal impact (0.1-0.2x)
            // Linear interpolation between 0.2 and 0.1
            decayFactor = 0.2 - Math.min(((daysElapsed - 180) / 180.0) * 0.1, 0.1);
        }
        // Apply decay to score
        int decayedScore = (int) Math.max(0, Math.round(score * decayFactor));
        return decayedScore;
    }

    public void calculateUserInterestScore(Set<UserInteractionData> userInteractions) {
        for (var ui : userInteractions) {
            /*
            We need to define some variables to store important information.

            We use uniqueTopicScores to store the accumulative score of a topic across all interactions and end up with a final score for each topic.
            We also need to avoid fetching content's topic over and over again if previous interaction already had the same contentId but we are skipping it for now
             */
            Map<String, Integer> uniqueTopicScores = new HashMap<>(); //To store combined topics score from various interaction
            List<InteractionData> interactions = ui.interactionData(); // all the interactions of the user in this batch
            String userId = ui.userId(); // the user id of the batch

            /*
            1. Decay existing topics scores. This will reduce the impact of old interactions.

            This will ONLY be done in the calculateUserInterestScore method because we do not want to decay scores intervally only when a user interactions.
            This way if a user uses an app after a break, his/her old interests will still persist with same score and after he interacts is when the decay will be applied if he/she consumes a new type of content.
             */
            decayInterestScore(userId);
            /*
            2. Calculate the activity of the user
            Based on interaction of the last 30 days we can determine if the user is active or not.
            We can go a step further and determine the user activity Yearly, 6 monthly, monthly, weekly.
            We can use them to define short-term and long-term interest

            User activity can be used to boost score of cross-topics. So a highly active user will see less related topic and more topics that he interacts with
            while a casual user will have high related topic scores and will see more variety.
            We can also use it to scale the topics which we get directly based on the interacted content.
             */
            long currentTime = Instant.now().toEpochMilli();
            long thirtyDaysAgo = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS).toEpochMilli();
            ActivityLevel userActivity = userActivityService.getActivityLevelOfUser(userId, reactor.util.function.Tuples.of(thirtyDaysAgo, currentTime));
            /*
            3. The next step is to get the weight of the interaction and topics of the content
            We then multiply the weight with the score and accumulate it on <Topic,Int> map
            We multiply the weight because some interaction needs negative score such as report or pressing "do not recommend".
             */
            for (var i : interactions) {
                //Get interaction weight
                int weight = platformService.getInteractionWeight(i.interactionType(), new Tuple<>(1, 100));
                //get the topics of the content, if it doesnt exist use llm to get the topics and save it as well
                ContentTopic contentTopic = db.contentTopics.stream().filter(ct -> ct.contentId().equals(i.contentId())).findFirst().orElse(null);
                List<String> topics;
                if (contentTopic == null) {
                    PostEntity contentData = db.posts.stream().filter(e -> e.id().equals(i.contentId())).findFirst().orElse(null);
                    if (contentData == null)
                        throw new RuntimeException("Content data not found for id: " + i.contentId());
                    topics = llmService.getTopics(db.topics.stream().toList(), contentData.data());
                    db.topics.addAll(topics);
                    db.contentTopics.add(new ContentTopic(i.contentId(), topics));
                } else {
                    topics = contentTopic.topics();
                }
                //Next we have to add the score to the unique topic scores
                topics.forEach(topic -> {
                    if (uniqueTopicScores.containsKey(topic)) {
                        uniqueTopicScores.put(topic, uniqueTopicScores.get(topic) * weight);
                    } else {
                        uniqueTopicScores.put(topic, weight);
                    }
                });

            }
            /*
            4. Next, we get the cross-topics of the user's topics.
            Once we have them we will apply user activity scaling to it.
            This way, we push trending and related topics more to less active users and push less related and direct content to active users.
             */
            /*
            5. Finally we will update ALL the scores with saturation in mind and save it to the database.
             */
            /*
            6. Next, we call updateTopicRelationships. We pass set of topics and it will create connection and strengths between them based on how many time it is related. We first decay existing score and then apply this.
             */

            //We need to normalize both the user topic score and cross toipic relationship score by keeping track of min and max and normalizing each of them

        }
    }

    public void decayInterestScore(String userId) {
        Map<String, Tuple<Integer, Long>> userInterestsMap = userInterestRepo.getUserInterests(userId);
        var currentTime = Instant.now().toEpochMilli();

        // If no interests for user, nothing to decay
        if (userInterestsMap.isEmpty()) {
            return;
        }

        // Create a map to hold the updated scores
        Map<String, Tuple<Integer, Long>> updatedTopics = new HashMap<>();

        // Process each topics and apply appropriate decay factor
        for (Map.Entry<String, Tuple<Integer, Long>> entry : userInterestsMap.entrySet()) {
            String topic = entry.getKey();
            Integer score = entry.getValue()._1();
            Long lastUpdatedTimestamp = entry.getValue()._2();
            int decayedScore = getDecayedScore(currentTime, lastUpdatedTimestamp, score);
            // Add to updated topics with the original timestamp (we're only decaying score, not updating timestamp)
            updatedTopics.put(topic, new Tuple<>(decayedScore, lastUpdatedTimestamp));
        }

        // Update the user's interests with decayed scores
        UserInterestEntity updatedEntity = new UserInterestEntity(userId, updatedTopics);

        userInterestRepo.saveUserInterests(updatedEntity);
    }

    /**
     * Calculates the saturation multiplier based on the current interest score.
     * As a user's interest score in a topics increases, new interactions with that topics
     * will have diminishing returns to prevent algorithmic "rabbit holes" and encourage
     * content diversity.
     *
     * @param currentScore The current interest score for a specific topics
     * @param scoreRange   A tuple containing (minScore, maxScore) to define the saturation thresholds
     * @return A multiplier between 0.1 and 1.0 to be applied to new score additions
     */
    public double calculateSaturationMultiplier(int currentScore, Tuple<Integer, Integer> scoreRange) {
        // Extract the min and max from the score range
        int minScore = scoreRange._1();
        int maxScore = scoreRange._2();

        // Calculate threshold values based on the provided range
        int threshold1 = minScore + (int) ((maxScore - minScore) * 0.1);  // 10% of range
        int threshold2 = minScore + (int) ((maxScore - minScore) * 0.3);  // 30% of range
        int threshold3 = minScore + (int) ((maxScore - minScore) * 0.6);  // 60% of range
        int threshold4 = minScore + (int) ((maxScore - minScore) * 0.9);  // 90% of range

        // Define multipliers based on where the current score falls within the range
        if (currentScore <= threshold1) {
            // Fresh Interest: Full impact (1.0x)
            return 1.0;
        } else if (currentScore <= threshold2) {
            // Developing Interest: Reduced impact (0.8x)
            return 0.8;
        } else if (currentScore <= threshold3) {
            // Established Interest: Diminished impact (0.6x)
            return 0.6;
        } else if (currentScore <= threshold4) {
            // Saturated Interest: Minimal impact (0.3x)
            return 0.3;
        } else {
            // Extremely Saturated Interest: Very minimal impact (0.1x)
            return 0.1;
        }
    }

    /**
     * Alternative implementation using a continuous function for smoother transitions
     * between saturation levels.
     *
     * @param currentScore The current interest score for a specific topics
     * @param scoreRange   A tuple containing (minScore, maxScore) to define the saturation curve
     * @return A multiplier between 0.1 and 1.0 to be applied to new score additions
     */
    public double calculateSaturationMultiplierContinuous(int currentScore, Tuple<Integer, Integer> scoreRange) {
        // Extract the min and max from the score range
        int minScore = scoreRange._1();
        int maxScore = scoreRange._2();

        // Calculate the midpoint of the range, which will be used as the scaling factor
        double scalingFactor = (maxScore - minScore) / 2.0;

        // Normalize the current score relative to the range
        double normalizedScore = (currentScore - minScore) / (double) (maxScore - minScore);

        // Calculate the saturation multiplier using a sigmoid-like curve
        // This will give 1.0 for low scores and gradually approach 0.1 for high scores
        double rawMultiplier = 1.0 / (1.0 + (normalizedScore * 3.0));

        // Ensure multiplier never goes below 0.1
        return Math.max(0.1, rawMultiplier);
    }


}