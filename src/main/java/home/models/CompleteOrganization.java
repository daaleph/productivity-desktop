package home.models;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class CompleteOrganization extends BranchedOrganization {
    private Map<UUID, User> users;

    public CompleteOrganization(int id) {
        super(id);
    }

    @Override
    public void fetchData() {
        // Fetch all branches for the organization
    }

    public Collection<User> getUsers(UUID[] userUuuid) {
        return this.users.values();
    }
}
