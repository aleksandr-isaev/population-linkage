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
import uk.ac.standrews.cs.population_records.record_types.Marriage;

import java.util.List;
import java.util.Map;

/**
 * Links a person appearing as the groom on a marriage record with a sibling appearing as the bride on another marriage record.
 */
public class GroomBrideSiblingLinkageRecipe extends LinkageRecipe {

    // TODO Redundant given BrideGroomSiblingLinkageRecipe?

    private static final double DISTANCE_THESHOLD = 0.12;

    public static final String LINKAGE_TYPE = "groom-bride-sibling";

    public static final int ID_FIELD_INDEX1 = Marriage.STANDARDISED_ID;
    public static final int ID_FIELD_INDEX2 = Marriage.STANDARDISED_ID;

    // TODO Why not father occupation?
    public static final List<Integer> LINKAGE_FIELDS = list(
            Marriage.GROOM_MOTHER_FORENAME,
            Marriage.GROOM_MOTHER_MAIDEN_SURNAME,
            Marriage.GROOM_FATHER_FORENAME,
            Marriage.GROOM_FATHER_SURNAME
    );

    public static final List<Integer> SEARCH_FIELDS = list(
            Marriage.BRIDE_MOTHER_FORENAME,
            Marriage.BRIDE_MOTHER_MAIDEN_SURNAME,
            Marriage.BRIDE_FATHER_FORENAME,
            Marriage.BRIDE_FATHER_SURNAME
    );

    /**
     * Various possible relevant sources of ground truth for siblings:
     * * identities of parents
     * * identities of parents' birth records
     */
    @SuppressWarnings("unchecked")
    public static final List<List<Pair>> TRUE_MATCH_ALTERNATIVES = list(
            list(   pair(Marriage.GROOM_MOTHER_IDENTITY, Marriage.BRIDE_MOTHER_IDENTITY),
                    pair(Marriage.GROOM_FATHER_IDENTITY, Marriage.BRIDE_FATHER_IDENTITY)),
            list(   pair(Marriage.GROOM_MOTHER_BIRTH_RECORD_IDENTITY, Marriage.BRIDE_MOTHER_BIRTH_RECORD_IDENTITY),
                    pair(Marriage.GROOM_FATHER_BIRTH_RECORD_IDENTITY, Marriage.BRIDE_FATHER_BIRTH_RECORD_IDENTITY))
    );

    public GroomBrideSiblingLinkageRecipe(String source_repository_name , String links_persistent_name) {
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
        return Marriage.class;
    }

    @Override
    public Class<? extends LXP> getQueryType() {
        return Marriage.class;
    }

    @Override
    public String getStoredRole() { return Marriage.ROLE_GROOM; }

    @Override
    public String getQueryRole() { return Marriage.ROLE_BRIDE; }

    @Override
    public List<Integer> getLinkageFields() {
        return LINKAGE_FIELDS;
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return isViable( proposedLink );
    }

    public static boolean isViable(RecordPair proposedLink) {

        if (LinkageConfig.MAX_SIBLING_AGE_DIFFERENCE == null) return true;

        try {
            int year_of_birth1 = SiblingMarriageHelper.getBirthYearOfPersonBeingMarried(proposedLink.record1, false);
            int year_of_birth2 = SiblingMarriageHelper.getBirthYearOfPersonBeingMarried(proposedLink.record2, true);

            return Math.abs(year_of_birth1 - year_of_birth2) <= LinkageConfig.MAX_SIBLING_AGE_DIFFERENCE;

        } catch(NumberFormatException e) {
            return true;
        }
    }

    @Override
    public List<Integer> getQueryMappingFields() {
        return SEARCH_FIELDS;
    }

    @Override
    public Map<String, Link> getGroundTruthLinks() {
        return getGroundTruthLinksAsymmetric();
    }

    @Override
    public int getNumberOfGroundTruthTrueLinks() {
        return getNumberOfGroundTruthLinksAsymmetric();
    }

    @Override
    public double getThreshold() {
        return DISTANCE_THESHOLD;
    }
}
