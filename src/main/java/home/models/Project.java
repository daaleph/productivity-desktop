package home.models;

import home.types.Triplet;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

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