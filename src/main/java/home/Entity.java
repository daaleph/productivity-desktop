package home;

import model.Identifier;

import java.util.List;
import java.util.Optional;

public final class Entity {
    private final Identifier<String> userIdentifiers;
    private final Identifier<Integer> branchIdentifiers;
    private final Identifier<Integer> organizationIdentifiers;

    // Private constructor (use builder to create instances)
    private Entity(Builder builder) {
        this.userIdentifiers = builder.userIdentifiers;
        this.branchIdentifiers = builder.branchIdentifiers;
        this.organizationIdentifiers = builder.organizationIdentifiers;
    }

    // Getters (return Optional for safety)
    public Optional<Identifier<String>> getUserIdentifiers() {
        return Optional.ofNullable(userIdentifiers);
    }

    public Optional<Identifier<Integer>> getOrganizationIdentifiers() {
        return Optional.ofNullable(organizationIdentifiers);
    }

    public Optional<Identifier<Integer>> getBranchIdentifiers() {
        return Optional.ofNullable(branchIdentifiers);
    }

    // Builder pattern for flexible construction
    public static class Builder {
        private Identifier<String> userIdentifiers;
        private Identifier<Integer> organizationIdentifiers;
        private Identifier<Integer> branchIdentifiers;

        public Builder user(String email) {
            this.userIdentifiers = Identifier.of(email);
            return this;
        }

        public Builder users(List<String> emails) {
            this.userIdentifiers = Identifier.ofMultiple(emails);
            return this;
        }

        public Builder organization(Integer orgId) {
            this.organizationIdentifiers = Identifier.of(orgId);
            return this;
        }

        public Builder organizations(List<Integer> orgIds) {
            this.organizationIdentifiers = Identifier.ofMultiple(orgIds);
            return this;
        }

        public Builder branch(Integer branchId) {
            this.branchIdentifiers = Identifier.of(branchId);
            return this;
        }

        public Builder branches(List<Integer> branchIds) {
            this.branchIdentifiers = Identifier.ofMultiple(branchIds);
            return this;
        }

        public Entity build() {
            return new Entity(this);
        }
    }
}