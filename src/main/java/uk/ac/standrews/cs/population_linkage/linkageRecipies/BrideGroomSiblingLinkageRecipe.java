package uk.ac.standrews.cs.population_linkage.linkageRecipies;

import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.supportClasses.Constants;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Marriage;
import uk.ac.standrews.cs.storr.impl.LXP;

import java.util.*;

public class BrideGroomSiblingLinkageRecipe extends LinkageRecipe {

    public BrideGroomSiblingLinkageRecipe(String results_repository_name, String links_persistent_name, String source_repository_name, RecordRepository record_repository) {
        super(results_repository_name, links_persistent_name, source_repository_name, record_repository);
    }

    @Override
    public LinkStatus isTrueMatch(LXP record1, LXP record2) {
        final String m1_father_id = record1.getString(Marriage.BRIDE_FATHER_IDENTITY);
        final String m1_mother_id = record1.getString(Marriage.BRIDE_MOTHER_IDENTITY);

        final String m2_father_id = record2.getString(Marriage.GROOM_FATHER_IDENTITY);
        final String m2_mother_id = record2.getString(Marriage.GROOM_MOTHER_IDENTITY);

        if (!m1_mother_id.isEmpty() && m1_mother_id.equals(m2_mother_id) && !m1_father_id.isEmpty() && m1_father_id.equals(m2_father_id)) return LinkStatus.TRUE_MATCH;

        return LinkStatus.NOT_TRUE_MATCH;
    }

    @Override
    public String getLinkageType() {
        return "sibling bundling between groom and bride on marriage records";
    }

    @Override
    public String getSourceType1() {
        return "marriages";
    }

    @Override
    public String getSourceType2() {
        return "marriages";
    }

    @Override
    public String getRole1() { return Marriage.ROLE_BRIDE; }

    @Override
    public String getRole2() { return Marriage.ROLE_GROOM; }

    @Override
    public List<Integer> getLinkageFields1() {
        return Constants.SIBLING_BUNDLING_BRIDE_MARRIAGE_LINKAGE_FIELDS;
    }

    @Override
    public List<Integer> getLinkageFields2() {
        return Constants.SIBLING_BUNDLING_GROOM_MARRIAGE_LINKAGE_FIELDS;
    }

    @Override
    public Map<String, Link> getGroundTruthLinks() {
        return getGroundTruthLinksOnSiblingNonSymmetric(
                Marriage.BRIDE_FATHER_IDENTITY, Marriage.BRIDE_MOTHER_IDENTITY,
                Marriage.GROOM_FATHER_IDENTITY, Marriage.GROOM_MOTHER_IDENTITY);
    }

    public int numberOfGroundTruthTrueLinks() {
        return getNumberOfGroundTruthLinksOnSiblingNonSymmetric(
                Marriage.BRIDE_FATHER_IDENTITY, Marriage.BRIDE_MOTHER_IDENTITY,
                Marriage.GROOM_FATHER_IDENTITY, Marriage.GROOM_MOTHER_IDENTITY);
    }

    @Override
    public Iterable<LXP> getPreFilteredSourceRecords1() {
        return filterSourceRecords(getSourceRecords1(), new int[]{
                Marriage.BRIDE_FATHER_FORENAME, Marriage.BRIDE_FATHER_SURNAME,
                Marriage.BRIDE_MOTHER_FORENAME, Marriage.BRIDE_MOTHER_MAIDEN_SURNAME
        }, 3);
    }

    @Override
    public Iterable<LXP> getPreFilteredSourceRecords2() {
        return filterSourceRecords(getSourceRecords1(), new int[]{
                Marriage.GROOM_FATHER_FORENAME, Marriage.GROOM_FATHER_SURNAME,
                Marriage.GROOM_MOTHER_FORENAME, Marriage.GROOM_MOTHER_MAIDEN_SURNAME
        }, 3);
    }
}