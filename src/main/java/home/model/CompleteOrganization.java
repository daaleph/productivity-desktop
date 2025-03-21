package home.model;

public class CompleteOrganization extends BranchedOrganization {
    private Map<UUID, User> users;

    public CompleteOrganization(int id) {
        super(id);
    }

    @Override
    public void fetchData() {
        // Fetch all branches for the organization
    }

    public User getUsers(UUID[] userUuuid) {
        return users.get(userUuuid);
    }
}
