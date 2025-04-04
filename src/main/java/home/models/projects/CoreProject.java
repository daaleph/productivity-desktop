package home.models.projects;

import home.records.MeasuredGoal;
import home.records.MeasuredSet;
import home.records.Priority;
import home.types.Triplet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class CoreProject {
    private final UUID uuid;
    private int type;
    private String name;
    private boolean favorite;
    private List<Priority> priorities;
    private List<MeasuredGoal> measuredGoals;
    private MeasuredSet<Integer> necessaryTime;
    private List<Triplet<Integer, String, Double>> underlyingCategories;

    public CoreProject(UUID uuid) {
        this.uuid = uuid;
    }

    public void setData(
            String name,
            int type,
            boolean favorite,
            ZonedDateTime dateToStart,
            List<Priority> priorities,
            List<MeasuredGoal> measuredGoals,
            MeasuredSet<Integer> necessaryTime,
            List<Triplet<Integer, String, Double>> underlyingCategories
    ) {
        this.name = name;
        this.type = type;
        this.favorite = favorite;
        this.priorities = priorities;
        this.measuredGoals = measuredGoals;
        this.necessaryTime = necessaryTime;
        this.underlyingCategories = underlyingCategories;
    }
    public String getName() {
        return this.name;
    }

    public void fetchData() {
        // to fetch complete using this.uuid
    }

    public void updateData() {
        // to update project
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void sortUnderlyingCategories() {
        // sorts underlying categories
    }

    public int getType() {
        return type;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public List<Priority> getPriorities() {
        return priorities;
    }

    public List<MeasuredGoal> getMeasuredGoals() {
        return measuredGoals;
    }

    public MeasuredSet<Integer> getNecessaryTime() {
        return necessaryTime;
    }

    public List<Triplet<Integer, String, Double>> getUnderlyingCategories() {
        return underlyingCategories;
    }

    public void setUnderlyingCategories(List<Triplet<Integer, String, Double>> underlyingCategories) {
        this.underlyingCategories = underlyingCategories;
    }
}