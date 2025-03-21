package home.model;

import java.util.Map;

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