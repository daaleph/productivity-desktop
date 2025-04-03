package home.models.branchs;

import home.models.User;

class UserBranch extends ProjectedBranch {
    private User user;

    public UserBranch(int id) {
        super(id);
    }

    @Override
    public void fetchData() {
        /* Implementation to load projects for this branch */
    }
}