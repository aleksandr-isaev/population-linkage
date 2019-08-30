package uk.ac.standrews.cs.population_linkage.experiments.linkage;

import uk.ac.standrews.cs.storr.impl.JPO;
import uk.ac.standrews.cs.storr.impl.JPOMetadata;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.storr.impl.LXPReference;
import uk.ac.standrews.cs.storr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.storr.interfaces.IBucket;
import uk.ac.standrews.cs.storr.interfaces.IStoreReference;
import uk.ac.standrews.cs.storr.types.JPO_FIELD;
import uk.ac.standrews.cs.utilities.JSONReader;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Link extends JPO {

    @JPO_FIELD
    private LXPReference<LXP> record1;
    @JPO_FIELD
    private String role1;
    @JPO_FIELD
    private LXPReference<LXP> record2;
    @JPO_FIELD
    private String role2;
    @JPO_FIELD
    private double confidence;
    @JPO_FIELD
    private String link_type;
    @JPO_FIELD
    private List<String> provenance;

    public Link() {}

    public Link(long id, JSONReader reader, IBucket bucket ) throws PersistentObjectException {
        super( id, bucket );
        readJSON(reader, true);
    }

    public Link(LXP record1, String role1, LXP record2, String role2, float confidence, String link_type, String... provenance) throws PersistentObjectException {

        this.record1 = (LXPReference) record1.getThisRef();
        this.role1 = role1;
        this.record2 = (LXPReference) record2.getThisRef();
        this.role2 = role1;
        this.confidence = confidence;
        this.link_type = link_type;
        this.provenance = Arrays.asList(provenance);
    }

    public IStoreReference<LXP> getRecord1() {
        return record1;
    }

    public String getRole1() {
        return role1;
    }

    public IStoreReference<LXP> getRecord2() {
        return record2;
    }

    public String getRole2() {
        return role2;
    }

    public String getLink_type() {
        return link_type;
    }

    public double getConfidence() { return confidence; }

    public String getLinkType() { return link_type; }

    public List<String> getProvenance() { return provenance; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(role1, link.role1) &&
                Objects.equals(role2, link.role2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role1, role2);
    }

    @Override
    public String toString() {
        return role1 + " - " + role2;
    }

    /* Storr support mechanism - ALL STORR JPO OBJECTS MUST HAVE THIS BOILERPLATE CODE */

    /*
     * This field is used to store the metadata for the class.
     */
    private static final JPOMetadata static_metadata;

    /*
     * This selector returns the class metadata.
     */
    @Override
    public JPOMetadata getMetaData() {
        return static_metadata;
    }

    /*
     * This static initialiser initialises the static meta data
     * The two parameters to the JPOMetadata constructor are the name of this class and the name the type is given in the store.
     */
    static {
        try {
            static_metadata = new JPOMetadata(Link.class,"JPOLink");
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }

}
