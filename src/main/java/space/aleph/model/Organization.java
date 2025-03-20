package space.aleph.model;

public class Organization {
    protected int id;
    protected String name;

    public Organization(int id, String name) {
        this.id = id;
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
        return name; // For ListView display
    }
}