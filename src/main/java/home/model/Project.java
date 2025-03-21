package home.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CoreProject {
    private final UUID uuid;
    private int type;
    private String name;
    private boolean favorite;
    private int years, months, days;
    private ZonedDateTime dateToStart;
    private List<Triplet<Integer, String, Double>> underlyingCategories;

    public CoreProject(UUID uuid) {
        this.uuid = uuid;
    }

    public void fetchData() {
        // to fetch complete data about a core
    }

    public void updateData() {
        // to update project
    }

    public UUID getUuid() {
        return uuid;
    }
}

public class Project extends CoreProject {
    public Project(UUID uuid) {
        super(uuid);
    }

    @Override
    public void fetchData() {
        super.fetchData(); // Optional: Use parent's logic
        // Additional Project-specific logic
    }

    @Override
    public void updateData() {
        super.updateData(); // Optional
        // Additional logic
    }
}