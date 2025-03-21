package home.model;

import java.util.HashMap;

public class User {
    private static User instance;

    protected int age;
    protected String name;
    protected String email;
    protected int[] branches;
    protected int[] organizations;
    protected UUID[] coreProjects;
    protected HashMap<Integer, String> priorities;

    /**
     * Returns the single instance of User. On the first call, the provided
     * parameters are used to create the instance. Subsequent calls will return
     * the already created instance, ignoring any parameters.
     *
     * @param age the user's age (used only during initialization)
     * @param name the user's name (used only during initialization)
     * @param email the user's email (used only during initialization)
     * @param branches user's branches
     * @param priorities the map of user priorities (used only during initialization)
     * @param organizations user's organizations
     * @param organizedBranches how user's branches are related to user's organizations
     * @param coreProjects the most important projects of a user
     */
    private User(
        String name,
        int age,
        String email,
        HashMap<Integer, String> priorities,
        int[] organizations,
        int[] branches,
        int[] organizedBranches,
        UUID[] coreProjects
    ) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.priorities = priorities;
        this.organizations = organizations;
        this.branches = branches;
        this.organizedBranches = organizedBranches;
        this.coreProjects = coreProjects;
    }

    /**
     * Returns the single instance of User. On the first call, the provided
     * parameters are used to create the instance. Subsequent calls will return
     * the already created instance, ignoring any parameters.
     *
     * @param name the user's name (used only during initialization)
     * @param age the user's age (used only during initialization)
     * @param email the user's email (used only during initialization)
     * @param priorities the map of user priorities (used only during initialization)
     * @return the singleton User instance
     */
    public static User getInstance(
        String name,
        int age,
        String email,
        HashMap<Integer, String> priorities,
        int[] organizations,
        int[] branches,
        int[] organizedBranches,
        UUID[] coreProjects
    ) {
        if (instance == null) {
            instance = new User(name, age, email, priorities, organizations, branches, organizedBranches, coreProjects);
        }
        return instance;
    }

    /**
     * Returns the User instance if it has been initialized.
     * @return the singleton User instance
     * @throws IllegalStateException if the instance has not yet been initialized.
     */
    public static User getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "User has not been initialized. Call getInstance(String, int, String, HashMap<Integer, String>) first."
            );
        }
        return instance;
    }

    public String nameProperty() {
        return name;
    }

    public int ageProperty() {
        return age;
    }

    public String getPriority(Integer index) {
        return priorities.get(index);
    }

    public HashMap<Integer, String> getPriorities() {
        return priorities;
    }

    @Override
    public String toString() {
        return String.format("Name: %s, Email: %s", name, email);
    }

}
