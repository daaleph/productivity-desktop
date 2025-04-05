package home.models.projects;

import home.records.MeasuredGoal;
import home.records.MeasuredSet;
import home.records.Priority;
import home.records.Triplet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class CoreProject {
    private final UUID uuid;
    private int type;
    private String name;
    private boolean favorite;
    private ZonedDateTime dateToStart;
    private List<Priority> priorities;
    private List<MeasuredGoal> measuredGoals;
    private MeasuredSet<Integer> necessaryTime;
    private List<Triplet<Integer, String, Double>> underlyingCategories;

    public CoreProject(UUID uuid) {
        this.uuid = uuid;
    }

    public void setData(CoreProjectData data) {
        this.name = data.name;
        this.type = data.type;
        this.favorite = data.favorite;
        this.dateToStart = data.dateToStart;
        this.priorities = data.priorities;
        this.measuredGoals = data.measuredGoals;
        this.necessaryTime = data.necessaryTime;
        this.underlyingCategories = data.underlyingCategories;
    }

    // Getters and other methods remain the same
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public int getType() { return type; }
    public boolean isFavorite() { return favorite; }
    public ZonedDateTime getDateToStart() { return dateToStart; }
    public List<Priority> getPriorities() { return priorities; }
    public List<MeasuredGoal> getMeasuredGoals() { return measuredGoals; }
    public MeasuredSet<Integer> getNecessaryTime() { return necessaryTime; }
    public List<Triplet<Integer, String, Double>> getUnderlyingCategories() { return underlyingCategories; }
    public void setUnderlyingCategories(List<Triplet<Integer, String, Double>> uc) { underlyingCategories = uc; }

    public static class CoreProjectData {
        protected String name;
        protected int type;
        protected boolean favorite;
        protected ZonedDateTime dateToStart;
        protected List<Priority> priorities;
        protected List<MeasuredGoal> measuredGoals;
        protected MeasuredSet<Integer> necessaryTime;
        protected List<Triplet<Integer, String, Double>> underlyingCategories;

        public CoreProjectData(String name, int type, boolean favorite, ZonedDateTime dateToStart,
                               List<Priority> priorities, List<MeasuredGoal> measuredGoals,
                               MeasuredSet<Integer> necessaryTime,
                               List<Triplet<Integer, String, Double>> underlyingCategories) {
            this.name = name;
            this.type = type;
            this.favorite = favorite;
            this.dateToStart = dateToStart;
            this.priorities = priorities;
            this.measuredGoals = measuredGoals;
            this.necessaryTime = necessaryTime;
            this.underlyingCategories = underlyingCategories;
        }
    }
}