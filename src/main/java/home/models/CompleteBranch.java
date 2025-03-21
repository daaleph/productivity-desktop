package home.models;

import java.util.Map;
import java.util.UUID;

class CompleteBranch extends ProjectBranch {
    private Map<UUID, Project> users;

    public CompleteBranch(int id) {
        super(id);
    }

    @Override
    public void fetchData() {
        /* Implementation to load projects for this branch */
    }
}