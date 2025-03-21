package home.model;

public class Organization {
    protected int id;
    protected String name;

    public Organization(int id) {
        this.id = id;
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

public abstract class BranchedOrganization extends Organization {
    protected Map<Integer, Branch> branches;

    public BranchedOrganization(int id) {
        super(id);
        fetchData();
    }

    public abstract void fetchData();

    public Map<Integer, Branch> getBranches() {
        return branches;
    }
}