package home.models;

class UserBranch extends ProjectBranch {
    private User user;

    public UserBranch(int id) {
        super(id);
    }

    @Override
    public void fetchData() {
        /* Implementation to load projects for this branch */
    }
}