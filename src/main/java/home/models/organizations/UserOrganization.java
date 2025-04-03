package home.models.organizations;

import java.util.Map;
import home.models.User;
import java.util.HashMap;
import home.models.branchs.Branch;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class UserOrganization extends BranchedOrganization {
    private User user;
    private final String email;
    private Map<Integer, Branch> branches = new HashMap<>();

    @JsonCreator
    public UserOrganization(
            @JsonProperty("id") int id,
            @JsonProperty("email") String email
    ) {
        super(id);
        this.email = email;
    }

    public void setBranches(Map<Integer, Branch> branches) {
        this.branches = branches;
    }

    @Override
    public void fetchData() {
        System.out.println(id);
    }

    public Map<Integer, Branch> getBranches() {
        return branches;
    }

    public User getUser() {
        return user;
    }

    public String getEmail() {
        return this.email;
    }
}