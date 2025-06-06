package dev.kuku.interestcalculator.fakeDatabase;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class ContentDb {
    private final Map<String, ContentRow> contentTopicRows = new HashMap<>();

    public List<ContentRow> getAllContents() {
        return List.copyOf(contentTopicRows.values());
    }

    // Method to insert one ContentRow into the map
    private void add(String contentId, String content, Set<String> topics, String userId, long timestamp) {
        contentTopicRows.put(contentId, new ContentRow(contentId, content, topics, userId, timestamp));
    }

    @PostConstruct
    private void init() {
        add("post001", "Breaking: Massive fire breaks out in downtown LA. Firefighters on the scene.", Set.of("news", "local", "emergency"), "user001", 1717665600L);
        add("post002", "Just finished God of War Ragnarok. What a ride!", Set.of("gaming", "reviews", "ps5"), "user002", 1717669200L);
        add("post003", "JavaScript closures finally make sense to me now. Thanks MDN!", Set.of("programming", "javascript", "webdev"), "user003", 1717672800L);
        add("post004", "Push day at the gym! Bench 225lbs for 3 reps 💪", Set.of("fitness", "gym", "workout"), "user004", 1717676400L);
        add("post005", "Trying out this butter chicken recipe tonight 🍛", Set.of("food", "cooking", "indian"), "user005", 1717680000L);
        add("post006", "Top 5 beginner-friendly stocks for June 2025 📈", Set.of("finance", "stocks", "investment"), "user006", 1717683600L);
        add("post007", "Backpacking across Europe – Day 4: Prague is magical!", Set.of("travel", "europe", "adventure"), "user007", 1717687200L);
        add("post008", "Just published a new tutorial: Build a REST API with Spring Boot", Set.of("programming", "java", "springboot"), "user008", 1717690800L);
        add("post009", "Nintendo just dropped a Zelda remake trailer 😱", Set.of("gaming", "nintendo", "zelda"), "user009", 1717694400L);
        add("post010", "Study tip: Active recall beats passive reading every time!", Set.of("education", "studytips", "productivity"), "user010", 1717698000L);

        // News
        add("post011", "Heavy rains expected in Mumbai over the weekend – stay safe!", Set.of("news", "weather", "mumbai"), "user011", 1717701600L);
        add("post012", "Election 2025: Who’s leading the polls?", Set.of("news", "politics", "election"), "user012", 1717705200L);
        add("post013", "Tech giants face new antitrust investigation in EU.", Set.of("news", "technology", "legal"), "user013", 1717708800L);
        add("post014", "NASA confirms Artemis II launch window.", Set.of("news", "space", "science"), "user014", 1717712400L);
        add("post015", "Oil prices spike after geopolitical tensions rise.", Set.of("news", "economy", "energy"), "user015", 1717716000L);

        // Gaming
        add("post016", "Is Elden Ring worth it in 2025?", Set.of("gaming", "eldenring", "reviews"), "user016", 1717719600L);
        add("post017", "Trying speedrunning Hollow Knight 🦋", Set.of("gaming", "speedrun", "indie"), "user017", 1717723200L);
        add("post018", "My Valorant rank just dropped. Need teammates!", Set.of("gaming", "valorant", "fps"), "user018", 1717726800L);
        add("post019", "Stardew Valley update brings co-op pets!", Set.of("gaming", "stardew", "co-op"), "user019", 1717730400L);
        add("post020", "Retro gaming weekend: NES Marathon 🎮", Set.of("gaming", "retro", "nostalgia"), "user020", 1717734000L);

        // Programming
        add("post021", "Finally understood monads. It only took 3 years!", Set.of("programming", "functional", "haskell"), "user021", 1717737600L);
        add("post022", "Angular or React in 2025?", Set.of("programming", "webdev", "frameworks"), "user022", 1717741200L);
        add("post023", "Wrote a Python script to auto-organize my photos", Set.of("programming", "python", "automation"), "user023", 1717744800L);
        add("post024", "Rust’s ownership model is hard but powerful.", Set.of("programming", "rust", "systems"), "user024", 1717748400L);
        add("post025", "Deployed my first microservice architecture project today.", Set.of("programming", "backend", "devops"), "user025", 1717752000L);

        // Fitness
        add("post026", "Down 5kg in 2 months. Clean eating works!", Set.of("fitness", "weightloss", "health"), "user026", 1717755600L);
        add("post027", "Trying calisthenics. Anyone else on this journey?", Set.of("fitness", "bodyweight", "training"), "user027", 1717759200L);
        add("post028", "Leg day destroyed me 😵", Set.of("fitness", "gym", "routine"), "user028", 1717762800L);
        add("post029", "Running 10K every morning now. Feeling unstoppable!", Set.of("fitness", "running", "endurance"), "user029", 1717766400L);
        add("post030", "Should I try CrossFit? Opinions?", Set.of("fitness", "crossfit", "community"), "user030", 1717770000L);

        // Food
        add("post031", "Street tacos in Mexico are undefeated 🌮", Set.of("food", "mexican", "travel"), "user031", 1717773600L);
        add("post032", "Baking sourdough bread every Sunday 🍞", Set.of("food", "baking", "homemade"), "user032", 1717777200L);
        add("post033", "Korean BBQ night with friends 🥩", Set.of("food", "korean", "bbq"), "user033", 1717780800L);
        add("post034", "Making vegan lasagna today – tips?", Set.of("food", "vegan", "recipes"), "user034", 1717784400L);
        add("post035", "Best filter coffee I’ve had in ages ☕", Set.of("food", "coffee", "cafe"), "user035", 1717788000L);

        // Travel
        add("post036", "Switzerland’s train rides are next-level scenic 🚆", Set.of("travel", "europe", "nature"), "user036", 1717791600L);
        add("post037", "Off to Thailand for some island hopping 🏝️", Set.of("travel", "asia", "adventure"), "user037", 1717795200L);
        add("post038", "Bucket list: Hike in Patagonia ✅", Set.of("travel", "southamerica", "trekking"), "user038", 1717798800L);
        add("post039", "Solo travel is the best therapy.", Set.of("travel", "life", "solo"), "user039", 1717802400L);
        add("post040", "Japan's cherry blossom season is unreal 🌸", Set.of("travel", "japan", "spring"), "user040", 1717806000L);

        // Education
        add("post041", "Graduated with honors! 🎓", Set.of("education", "college", "achievement"), "user041", 1717809600L);
        add("post042", "Studying data structures all day. Pointers suck.", Set.of("education", "cs", "studygrind"), "user042", 1717813200L);
        add("post043", "Anyone using Notion to study? Loving it.", Set.of("education", "tools", "study"), "user043", 1717816800L);
        add("post044", "Flashcards + Pomodoro = Productivity boost!", Set.of("education", "studytips", "techniques"), "user044", 1717820400L);
        add("post045", "Math is beautiful once you stop fearing it.", Set.of("education", "math", "motivation"), "user045", 1717824000L);

        // Finance
        add("post046", "Crypto is down again. Buying the dip?", Set.of("finance", "crypto", "market"), "user046", 1717827600L);
        add("post047", "Opened my first Roth IRA today.", Set.of("finance", "retirement", "savings"), "user047", 1717831200L);
        add("post048", "Building wealth starts with budgeting.", Set.of("finance", "personal", "tips"), "user048", 1717834800L);
        add("post049", "Made $300 from side hustle in a week 💰", Set.of("finance", "sidehustle", "income"), "user049", 1717838400L);
        add("post050", "Credit card debt is no joke. Be careful!", Set.of("finance", "debt", "awareness"), "user050", 1717842000L);
        add("post051", "Local startup raises $10M in Series A funding!", Set.of("news", "startup", "tech"), "user051", 1717845600L);
        add("post052", "Earthquake hits Turkey, magnitude 6.2 reported.", Set.of("news", "disaster", "emergency"), "user052", 1717849200L);
        add("post053", "Apple announces WWDC 2025 date – what to expect", Set.of("news", "apple", "tech"), "user053", 1717852800L);
        add("post054", "Government launches new healthcare initiative", Set.of("news", "health", "policy"), "user054", 1717856400L);
        add("post055", "UN summit to discuss climate emergency this week", Set.of("news", "climate", "global"), "user055", 1717860000L);

        // Gaming
        add("post056", "Trying out Palworld – Pokémon meets survival?", Set.of("gaming", "palworld", "newrelease"), "user056", 1717863600L);
        add("post057", "Anyone else addicted to Hades 2 already?", Set.of("gaming", "roguelike", "indie"), "user057", 1717867200L);
        add("post058", "GTA VI trailer breakdown – thoughts?", Set.of("gaming", "gta", "hype"), "user058", 1717870800L);
        add("post059", "Beat Sekiro without dying 😤", Set.of("gaming", "challenge", "sekiro"), "user059", 1717874400L);
        add("post060", "Is mobile gaming taking over PC?", Set.of("gaming", "mobile", "debate"), "user060", 1717878000L);

        // Programming
        add("post061", "What's the best IDE for Kotlin?", Set.of("programming", "kotlin", "tools"), "user061", 1717881600L);
        add("post062", "Built a Markdown-to-PDF converter with Python.", Set.of("programming", "tools", "python"), "user062", 1717885200L);
        add("post063", "Exploring WebAssembly – so much potential!", Set.of("programming", "wasm", "webdev"), "user063", 1717888800L);
        add("post064", "CI/CD finally set up using GitHub Actions.", Set.of("programming", "devops", "automation"), "user064", 1717892400L);
        add("post065", "Tried pair programming for the first time. Surprisingly fun.", Set.of("programming", "collaboration", "xp"), "user065", 1717896000L);

        // Fitness
        add("post066", "Meal prepped for the whole week 😎", Set.of("fitness", "nutrition", "mealprep"), "user066", 1717899600L);
        add("post067", "Hit a new PR in squats today: 180kg!", Set.of("fitness", "strength", "gym"), "user067", 1717903200L);
        add("post068", "Trying intermittent fasting. Anyone else doing it?", Set.of("fitness", "health", "diet"), "user068", 1717906800L);
        add("post069", "Yoga session cleared my mind 🧘‍♂️", Set.of("fitness", "mentalhealth", "wellness"), "user069", 1717910400L);
        add("post070", "Can you get ripped with just resistance bands?", Set.of("fitness", "homeworkout", "debate"), "user070", 1717914000L);

        // Food
        add("post071", "Discovered a street vendor who sells *perfect* samosas", Set.of("food", "snacks", "streetfood"), "user071", 1717917600L);
        add("post072", "Chocolate lava cake with ice cream = perfection 🍫", Set.of("food", "dessert", "indulgence"), "user072", 1717921200L);
        add("post073", "Tried Ethiopian food for the first time today!", Set.of("food", "ethiopian", "culture"), "user073", 1717924800L);
        add("post074", "Homemade ramen > instant noodles", Set.of("food", "ramen", "homemade"), "user074", 1717928400L);
        add("post075", "My favorite midnight snack: cheese toast 🧀", Set.of("food", "snacks", "comfort"), "user075", 1717932000L);

        // Travel
        add("post076", "Rode a camel in the Sahara Desert 🐪", Set.of("travel", "desert", "africa"), "user076", 1717935600L);
        add("post077", "Northern lights in Iceland. Unreal experience!", Set.of("travel", "nature", "iceland"), "user077", 1717939200L);
        add("post078", "Planning a van life road trip through Canada 🚐", Set.of("travel", "roadtrip", "vanlife"), "user078", 1717942800L);
        add("post079", "Venice is slowly sinking – go before it’s too late.", Set.of("travel", "europe", "climate"), "user079", 1717946400L);
        add("post080", "How to travel on a budget – tips from a broke explorer", Set.of("travel", "budget", "advice"), "user080", 1717950000L);

        // Education
        add("post081", "Passed my AWS Certification exam today!", Set.of("education", "cloud", "certification"), "user081", 1717953600L);
        add("post082", "Online courses vs. college – what do you prefer?", Set.of("education", "debate", "learning"), "user082", 1717957200L);
        add("post083", "Learning about World War II from a historian's lens.", Set.of("education", "history", "reading"), "user083", 1717960800L);
        add("post084", "Trying to learn Mandarin – any tips?", Set.of("education", "language", "mandarin"), "user084", 1717964400L);
        add("post085", "Flashcards app that syncs across all devices? 😮", Set.of("education", "tools", "recommendation"), "user085", 1717968000L);

        // Finance
        add("post086", "How I saved 1 lakh in 3 months 📉", Set.of("finance", "personal", "saving"), "user086", 1717971600L);
        add("post087", "The truth about passive income streams in 2025", Set.of("finance", "income", "realistic"), "user087", 1717975200L);
        add("post088", "Should you invest in gold or crypto?", Set.of("finance", "debate", "strategy"), "user088", 1717978800L);
        add("post089", "Planning early retirement at 40 – possible?", Set.of("finance", "fire", "retirement"), "user089", 1717982400L);
        add("post090", "Understanding compound interest changed my life.", Set.of("finance", "basics", "wealth"), "user090", 1717986000L);

        // General / Mixed
        add("post091", "Met my internet friend in real life for the first time 🫶", Set.of("life", "friendship", "story"), "user091", 1717989600L);
        add("post092", "When was the last time you watched a sunset without your phone?", Set.of("life", "mindfulness", "reflection"), "user092", 1717993200L);
        add("post093", "Moving into a new apartment. Any space-saving hacks?", Set.of("life", "home", "organization"), "user093", 1717996800L);
        add("post094", "Why does time fly when you're happy?", Set.of("life", "thoughts", "psychology"), "user094", 1718000400L);
        add("post095", "Started journaling – already feeling more in control.", Set.of("life", "mentalhealth", "habit"), "user095", 1718004000L);
        add("post096", "Built a habit tracker in Excel. Works surprisingly well.", Set.of("life", "productivity", "tools"), "user096", 1718007600L);
        add("post097", "Reading 'Atomic Habits' again – always relevant", Set.of("life", "books", "selfimprovement"), "user097", 1718011200L);
        add("post098", "Rainy days and lo-fi beats ☔🎶", Set.of("life", "music", "mood"), "user098", 1718014800L);
        add("post099", "Practicing gratitude every morning. Highly recommend.", Set.of("life", "gratitude", "routine"), "user099", 1718018400L);
        add("post100", "Dreamt of flying last night. What do dreams like that mean?", Set.of("life", "dreams", "curiosity"), "user100", 1718022000L);
    }

    public ContentRow getContentById(String contentId) {
        return contentTopicRows.get(contentId);
    }

    public static record ContentRow(String contentId, String content, Set<String> topics, String userId,
                                    long timestamp) {
    }
}
