/*
 * Copyright 2022 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_linkage.compositeMetrics.Sigma;
import uk.ac.standrews.cs.population_records.record_types.Marriage;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Links a person appearing as the groom on a marriage record with the same person appearing as the groom on another marriage record,
 * i.e. links a man's multiple marriages.
 */
public class GroomGroomIdentityLinkageRecipe extends LinkageRecipe {

    private static final double DISTANCE_THRESHOLD = 0.49;

    public static final String LINKAGE_TYPE = "groom-groom-identity";

    public static final int ID_FIELD_INDEX1 = Marriage.STANDARDISED_ID;
    public static final int ID_FIELD_INDEX2 = Marriage.STANDARDISED_ID;

    private int NUMBER_OF_DEATHS = EVERYTHING;
    public static final int ALL_LINKAGE_FIELDS = 8; // 8 is all of them

    public static final List<Integer> LINKAGE_FIELDS = list(
            Marriage.GROOM_FORENAME,
            Marriage.GROOM_SURNAME,
            Marriage.GROOM_MOTHER_FORENAME,
            Marriage.GROOM_MOTHER_MAIDEN_SURNAME,
            Marriage.GROOM_FATHER_FORENAME,
            Marriage.GROOM_FATHER_SURNAME,
            Marriage.GROOM_FATHER_OCCUPATION,
            Marriage.GROOM_OCCUPATION
    );

    public static final List<Integer> SEARCH_FIELDS = list(
            Marriage.GROOM_FORENAME,
            Marriage.GROOM_SURNAME,
            Marriage.GROOM_MOTHER_FORENAME,
            Marriage.GROOM_MOTHER_MAIDEN_SURNAME,
            Marriage.GROOM_FATHER_FORENAME,
            Marriage.GROOM_FATHER_SURNAME,
            Marriage.GROOM_FATHER_OCCUPATION,
            Marriage.GROOM_OCCUPATION
    );

    @SuppressWarnings("unchecked")
    public static final List<List<Pair>> TRUE_MATCH_ALTERNATIVES = list(
            list(pair(Marriage.GROOM_IDENTITY, Marriage.GROOM_IDENTITY))
    );

    public GroomGroomIdentityLinkageRecipe(String source_repository_name, String number_of_records, String links_persistent_name, NeoDbCypherBridge bridge) {
        super(source_repository_name, links_persistent_name, bridge);
        if( number_of_records.equals(EVERYTHING_STRING) ) {
            NUMBER_OF_DEATHS = EVERYTHING;
        } else {
            NUMBER_OF_DEATHS = Integer.parseInt(number_of_records);
        }
        setNumberLinkageFieldsRequired(ALL_LINKAGE_FIELDS);
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
    public double getThreshold() {
        return DISTANCE_THRESHOLD;
    }

    @Override
    public Metric<LXP> getCompositeMetric() {
        return new Sigma( getBaseMetric(),getLinkageFields(),ID_FIELD_INDEX1 );
    }

    @Override
    public List<Integer> getQueryMappingFields() {
        return SEARCH_FIELDS;
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return isViable( proposedLink );
    }

    /**
     * Checks whether the discrepancy between the recorded or calculated dates of birth on the two records is acceptably low.
     *
     * @param proposedLink the proposed link
     * @return true if the link is viable
     */
    public static boolean isViable(RecordPair proposedLink) {

        try {
            LocalDate date_of_birth1 = CommonLinkViabilityLogic.getBirthDateFromMarriageRecord(proposedLink.stored_record, false);
            LocalDate date_of_birth2 = CommonLinkViabilityLogic.getBirthDateFromMarriageRecord(proposedLink.query_record, false);

            return CommonLinkViabilityLogic.alternativeIdentityBirthDatesAreViable(date_of_birth1, date_of_birth2);

        } catch (NumberFormatException e) {
            return true;
        }
    }

    @Override
    public Map<String, Link> getGroundTruthLinks() {
        return getGroundTruthLinksAsymmetric();
    }

    @Override
    public long getNumberOfGroundTruthTrueLinks() {
        return getNumberOfGroundTruthLinksAsymmetric();
    }
}
