/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageConfig;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_records.record_types.Birth;

import java.util.List;
import java.util.Map;

/**
 * Links a person appearing as the child on a birth record with a sibling appearing as the child on another birth record.
 */
public class BirthSiblingLinkageRecipe extends LinkageRecipe {

    private static final double THRESHOLD = 0.67;

    public static final String LINKAGE_TYPE = "birth-birth-sibling";

    public static final int ID_FIELD_INDEX = Birth.STANDARDISED_ID;

    public static final List<Integer> LINKAGE_FIELDS = list(
            Birth.MOTHER_FORENAME,
            Birth.MOTHER_MAIDEN_SURNAME,
            Birth.FATHER_FORENAME,
            Birth.FATHER_SURNAME,
            Birth.PARENTS_PLACE_OF_MARRIAGE,
            Birth.PARENTS_DAY_OF_MARRIAGE,
            Birth.PARENTS_MONTH_OF_MARRIAGE,
            Birth.PARENTS_YEAR_OF_MARRIAGE
    );

    /**
     * Various possible relevant sources of ground truth for siblings:
     * * identities of parents
     * * identities of parents' marriage record
     * * identities of parents' birth records
     */
    @SuppressWarnings("unchecked")
    public static final List<List<Pair>> TRUE_MATCH_ALTERNATIVES = list(
            list(pair(Birth.MOTHER_IDENTITY, Birth.MOTHER_IDENTITY), pair(Birth.FATHER_IDENTITY, Birth.FATHER_IDENTITY)),
            list(pair(Birth.PARENT_MARRIAGE_RECORD_IDENTITY, Birth.PARENT_MARRIAGE_RECORD_IDENTITY)),
            list(pair(Birth.MOTHER_BIRTH_RECORD_IDENTITY, Birth.MOTHER_BIRTH_RECORD_IDENTITY), pair(Birth.FATHER_BIRTH_RECORD_IDENTITY, Birth.FATHER_BIRTH_RECORD_IDENTITY))
    );

    public BirthSiblingLinkageRecipe(String source_repository_name, String links_persistent_name) {
        super(source_repository_name, links_persistent_name);
    }

    @Override
    public LinkStatus isTrueMatch(LXP record1, LXP record2) {
        return trueMatch(record1, record2);
    }

    public static LinkStatus trueMatch(LXP record1, LXP record2) {
        return trueMatch(record1, record2, TRUE_MATCH_ALTERNATIVES);
    }
    @Override
    public String getLinkageType() {
        return LINKAGE_TYPE;
    }

    @Override
    public Class<? extends LXP> getStoredType() {
        return Birth.class;
    }

    @Override
    public Class<? extends LXP> getQueryType() {
        return Birth.class;
    }

    @Override
    public String getStoredRole() {
        return Birth.ROLE_BABY;
    }

    @Override
    public String getQueryRole() {
        return Birth.ROLE_BABY;
    }

    @Override
    public List<Integer> getLinkageFields() {
        return LINKAGE_FIELDS;
    }

    public static boolean isViable(RecordPair proposedLink) {

        try {
            int year_of_birth1 = Integer.parseInt(proposedLink.record1.getString(Birth.BIRTH_YEAR));
            int year_of_birth2 = Integer.parseInt(proposedLink.record2.getString(Birth.BIRTH_YEAR));

            return Math.abs(year_of_birth1 - year_of_birth2) <= LinkageConfig.MAX_SIBLING_AGE_DIFFERENCE;

        } catch (NumberFormatException e) { // in this case a BIRTH_YEAR is invalid
            return true;
        }
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return isViable(proposedLink);
    }

    @Override
    public List<Integer> getQueryMappingFields() {
        return getLinkageFields();
    }

    @Override
    public Map<String, Link> getGroundTruthLinks() {
        return getGroundTruthLinksSymmetric();
    }

    @Override
    public int getNumberOfGroundTruthTrueLinks() {
        return getNumberOfGroundTruthLinksSymmetric();
    }

    @Override
    public double getThreshold() {
        return THRESHOLD;
    }
}
