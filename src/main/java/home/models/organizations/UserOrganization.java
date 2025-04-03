package home.models.organizations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import home.models.User;
import home.models.branchs.Branch;

import java.util.HashMap;
import java.util.Map;

public class UserOrganization extends BranchedOrganization {
    private User user;
    private String email;
    private Map<Integer, Branch> branches = new HashMap<>();

    @JsonCreator
    public UserOrganization(
            @JsonProperty("id") int id,
            @JsonProperty("email") String email
    ) {
        super(id);
        this.email = email;
    }

    public Map<Integer, Branch> getBranches() {
        return branches;
    }

    public void setBranches(Map<Integer, Branch> branches) {
        this.branches = branches;
    }

    @Override
    public void fetchData() {
        System.out.println(id);
    }

    public User getUser() {
        return user;
    }
}