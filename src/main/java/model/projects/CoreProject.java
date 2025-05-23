package model.projects;

import records.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class CoreProject {
    private final UUID uuid;
    private EssentialInfo essential;
    private List<Priority> priorities;
    private List<MeasuredGoal> measuredGoals;
    private MeasuredSet<Integer> necessaryTime;
    private List<Tuple<UUID, Triplet<Integer, String, Double>>> underlyingCategories;

    public CoreProject(UUID uuid) {
        this.uuid = uuid;
    }

    public void setInfo(CoreProjectInfo info) {
        this.essential = info.essential;
        this.priorities = info.priorities;
        this.measuredGoals = info.measuredGoals;
        this.necessaryTime = info.necessaryTime;
        this.underlyingCategories = info.underlyingCategories;
    }

    // Getters and other methods remain the same
    public UUID getUuid() { return uuid; }
    public String getName() { return this.essential.name(); }
    public int getType() { return this.essential.type(); }
    public boolean isFavorite() { return this.essential.favorite(); }
    public ZonedDateTime getDateToStart() { return this.essential.dateToStart(); }
    public List<Priority> getPriorities() { return priorities; }
    public List<MeasuredGoal> getMeasuredGoals() { return measuredGoals; }
    public MeasuredSet<Integer> getNecessaryTime() { return necessaryTime; }
    public List<Tuple<UUID,Triplet<Integer, String, Double>>> getUnderlyingCategories() { return underlyingCategories; }
    public void setUnderlyingCategories(List<Tuple<UUID,Triplet<Integer, String, Double>>> uc) { underlyingCategories = uc; }

    public void logEntity() {
        System.out.printf("UUID: %s%n", getUuid());
        System.out.printf("Name: %s%n", getName());
        System.out.printf("Type: %d%n", getType());
        System.out.printf("Favorite: %b%n", isFavorite());
        System.out.printf("Date to start: %s%n", getDateToStart());

        System.out.println("Priorities:");
        for (Priority priority : getPriorities()) {
            System.out.printf("  - %s%n", priority);
        }

        System.out.println("Measured Goals:");
        for (MeasuredGoal goal : getMeasuredGoals()) {
            System.out.printf("  - %s%n", goal);
        }

        System.out.printf("Necessary Time: %s%n", getNecessaryTime());

        System.out.println("Underlying Categories:");
        for (Tuple<UUID, Triplet<Integer, String, Double>> category : getUnderlyingCategories()) {
            System.out.printf("  - UUID: %s, Triplet: %s%n", category.first(), category.second());
        }
    }

    public static class CoreProjectInfo {
        protected EssentialInfo essential;
        protected List<Priority> priorities;
        protected List<MeasuredGoal> measuredGoals;
        protected MeasuredSet<Integer> necessaryTime;
        protected List<Tuple<UUID,Triplet<Integer, String, Double>>> underlyingCategories;

        public CoreProjectInfo(EssentialInfo essential,
                               List<Priority> priorities,
                               List<MeasuredGoal> measuredGoals,
                               MeasuredSet<Integer> necessaryTime,
                               List<Tuple<UUID,Triplet<Integer, String, Double>>> underlyingCategories) {
            this.essential = essential;
            this.priorities = priorities;
            this.measuredGoals = measuredGoals;
            this.necessaryTime = necessaryTime;
            this.underlyingCategories = underlyingCategories;
        }
    }
}