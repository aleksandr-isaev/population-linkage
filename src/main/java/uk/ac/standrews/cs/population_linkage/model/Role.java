package uk.ac.standrews.cs.population_linkage.model;

import java.util.Objects;

public class Role {

    private String record_id;  // TODO change to IStoreReference
    private String role_type;

    public Role(String record_id, String role_type) {

        this.record_id = record_id;
        this.role_type = role_type;
    }

    public String getRecordId() {

        return record_id;
    }

    public String getRoleType() {

        return role_type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role1 = (Role) o;
        return Objects.equals(record_id, role1.record_id) &&
                Objects.equals(role_type, role1.role_type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(record_id, role_type);
    }
}
