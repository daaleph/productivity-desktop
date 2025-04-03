package home.models.projects;

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