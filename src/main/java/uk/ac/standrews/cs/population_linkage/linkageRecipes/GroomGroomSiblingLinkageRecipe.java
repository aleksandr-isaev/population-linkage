/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_records.record_types.Marriage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Links a person appearing as the groom on a marriage record with a sibling appearing as the groom on another marriage record.
 */
public class GroomGroomSiblingLinkageRecipe extends LinkageRecipe {

    private static final double DISTANCE_THRESHOLD = 0.14;

    public static final String LINKAGE_TYPE = "groom-groom-sibling";

    public static final int ID_FIELD_INDEX = Marriage.STANDARDISED_ID;

    public static final List<Integer> LINKAGE_FIELDS = list(
            Marriage.GROOM_MOTHER_FORENAME,
            Marriage.GROOM_MOTHER_MAIDEN_SURNAME,
            Marriage.GROOM_FATHER_FORENAME,
            Marriage.GROOM_FATHER_SURNAME
    );

    /**
     * Various possible relevant sources of ground truth for siblings:
     * * identities of parents
     * * identities of parents' birth records
     */
    @SuppressWarnings("unchecked")
    public static final List<List<Pair>> TRUE_MATCH_ALTERNATIVES = list(
            list(pair(Marriage.GROOM_MOTHER_IDENTITY, Marriage.GROOM_MOTHER_IDENTITY),
                    pair(Marriage.GROOM_FATHER_IDENTITY, Marriage.GROOM_FATHER_IDENTITY)),
            list(pair(Marriage.GROOM_MOTHER_BIRTH_RECORD_IDENTITY, Marriage.GROOM_MOTHER_BIRTH_RECORD_IDENTITY),
                    pair(Marriage.GROOM_FATHER_BIRTH_RECORD_IDENTITY, Marriage.GROOM_FATHER_BIRTH_RECORD_IDENTITY))
    );

    public GroomGroomSiblingLinkageRecipe(String source_repository_name, String links_persistent_name) {
        super(source_repository_name, links_persistent_name);
    }

    @Override
    public LinkStatus isTrueMatch(LXP record1, LXP record2) {
        return trueMatch(record1, record2);
    }

    public static LinkStatus trueMatch(LXP record1, LXP record2) {

        final String m1_groom_id = record1.getString(Marriage.GROOM_IDENTITY);
        final String m2_groom_id = record2.getString(Marriage.GROOM_IDENTITY);

        // Exclude matches for multiple marriages of the same groom.
        if (m1_groom_id.equals(m2_groom_id)) return LinkStatus.NOT_TRUE_MATCH;

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
    public String getStoredRole() {
        return Marriage.ROLE_GROOM;
    }

    @Override
    public String getQueryRole() {
        return Marriage.ROLE_GROOM;
    }

    @Override
    public List<Integer> getLinkageFields() {
        return LINKAGE_FIELDS;
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return isViable(proposedLink);
    }

    public static boolean isViable(RecordPair proposedLink) {

        if (proposedLink.stored_record.getString(Marriage.STANDARDISED_ID).equals(proposedLink.query_record.getString(Marriage.STANDARDISED_ID))) { // avoid self links.
            return false;
        }

        try {
            final LocalDate date_of_birth1 = CommonLinkViabilityLogic.getBirthDateFromMarriageRecord(proposedLink.stored_record, false);
            final LocalDate date_of_birth2 = CommonLinkViabilityLogic.getBirthDateFromMarriageRecord(proposedLink.query_record, false);

            return CommonLinkViabilityLogic.siblingBirthDatesAreViable(date_of_birth1, date_of_birth2);

        } catch (NumberFormatException e) {
            return true;
        }
    }

    @Override
    public List<Integer> getQueryMappingFields() {
        return LINKAGE_FIELDS;
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
        return DISTANCE_THRESHOLD;
    }
}
