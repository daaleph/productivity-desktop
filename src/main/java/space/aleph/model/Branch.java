package space.aleph.model;

public class Branch {
    protected int id;
    protected String name;

    public Branch(int id, String name) {
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
        return name; // For TreeView display
    }
}