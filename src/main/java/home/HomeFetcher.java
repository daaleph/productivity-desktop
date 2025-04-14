package home;

public abstract class HomeFetcher<C> {
    protected Entity entity;

    // Constructor takes configuration to pass to createEntity
    protected HomeFetcher(C config) {
        this.entity = createEntity(config);
        System.out.println("Common initialization logic");
    }

    // Subclasses implement this to build Entity with their specific config
    protected abstract Entity createEntity(C config);

    // Non-public method (for subclasses)
    protected void internalLogic() {
        System.out.println("Internal logic for subclasses");
    }

    // Public method (available to all users)
    public void sharedPublicMethod() {
        System.out.println("Public method from base class");
    }
}