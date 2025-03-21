package home.model;

class CompleteBranch extends ProjectBranch {
    private User[] users;

    public CompleteBranch(int id, String name) {
        super(id);
    }

    @Override
    public void fetchData() {
        /* Implementation to load projects for this branch */
    }
}