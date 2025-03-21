package home.model;

public class UserOrganization extends BranchedOrganization {
    private User user;

    public UserOrganization(int id) {
        super(id);
    }

    @Override
    public void fetchData() {
        // fetches and stores name and branches
    }

    public User getUser() {
        return user;
    }
}