package model.organizations;

import java.util.Map;
import model.branchs.Branch;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Organization {
    protected int id;
    protected String name;
    public Organization(int id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Name: %s", id, name);
    }
}

abstract class BranchedOrganization extends Organization {
    protected Map<Integer, Branch> branches;
    public BranchedOrganization(int id) {
        super(id);
    }
    public abstract void fetchData();

    @JsonProperty("branches")
    public void setBranches(Map<Integer, Branch> branches) {
        this.branches = branches;
    }
    public Map<Integer, Branch> getBranches() {
        return branches;
    }
}