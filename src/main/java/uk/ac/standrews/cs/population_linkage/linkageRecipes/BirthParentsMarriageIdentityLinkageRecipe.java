/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.helpers.RecordFiltering;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageConfig;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_linkage.compositeMetrics.Sigma;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.population_records.record_types.Marriage;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Links two people appearing as the parents on a birth record with the same people appearing as the spouses on a marriage record.
 */
public class BirthParentsMarriageIdentityLinkageRecipe extends LinkageRecipe {

    private static final double DISTANCE_THRESHOLD = 0.4;

    public static final String LINKAGE_TYPE = "birth-parents-marriage-identity";

    public static final int ID_FIELD_INDEX1 = Birth.STANDARDISED_ID;
    public static final int ID_FIELD_INDEX2 = Marriage.STANDARDISED_ID;

    public static final int ALL_LINKAGE_FIELDS = 8;
    private int NUMBER_OF_BIRTHS;

    private List<LXP> cached_records = null;

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

    public static final List<Integer> SEARCH_FIELDS = list(
            Marriage.BRIDE_FORENAME,
            Marriage.BRIDE_SURNAME,
            Marriage.GROOM_FORENAME,
            Marriage.GROOM_SURNAME,
            Marriage.PLACE_OF_MARRIAGE,
            Marriage.MARRIAGE_DAY,
            Marriage.MARRIAGE_MONTH,
            Marriage.MARRIAGE_YEAR
    );

    @SuppressWarnings("unchecked")
    public static final List<List<Pair>> TRUE_MATCH_ALTERNATIVES = list(
            list(pair(Birth.MOTHER_IDENTITY, Marriage.BRIDE_IDENTITY), pair(Birth.FATHER_IDENTITY, Marriage.GROOM_IDENTITY))
    );

    public BirthParentsMarriageIdentityLinkageRecipe(String source_repository_name, String number_of_records, String links_persistent_name, NeoDbCypherBridge bridge) {
        super(source_repository_name, links_persistent_name, bridge);
        if( number_of_records.equals(EVERYTHING_STRING) ) {
            NUMBER_OF_BIRTHS = EVERYTHING;
        } else {
            NUMBER_OF_BIRTHS = Integer.parseInt(number_of_records);
        }
        setNoLinkageFieldsRequired( ALL_LINKAGE_FIELDS );
    }

    @Override
    public Iterable<LXP> getBirthRecords() {
        if( cached_records == null ) {
            cached_records = RecordFiltering.filter(getNoLinkageFieldsRequired(), NUMBER_OF_BIRTHS, super.getBirthRecords(), getLinkageFields());
        }
        return cached_records;
    }

    @Override
    public LinkStatus isTrueMatch(LXP birth, LXP marriage) {
        return trueMatch(birth, marriage);
    }

    public static LinkStatus trueMatch(LXP birth, LXP marriage) {
        return trueMatch(birth, marriage, TRUE_MATCH_ALTERNATIVES);
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
        return Marriage.class;
    }

    @Override
    public String getStoredRole() {
        return Birth.ROLE_PARENTS;  // mother and father
    }

    @Override
    public String getQueryRole() {
        return Marriage.ROLE_SPOUSES;  // bride and groom
    }

    @Override
    public List<Integer> getLinkageFields() {
        return LINKAGE_FIELDS;
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return isViable(proposedLink);
    }

    /**
     * Checks whether a plausible period has elapsed between the marriage and the birth.
     *
     * @param proposedLink the proposed link
     * @return true if the link is viable
     */
    public static boolean isViable(final RecordPair proposedLink) {

        try {
            final LXP birth_record = proposedLink.stored_record;
            final LXP marriage_record = proposedLink.query_record;

            final LocalDate date_of_child_birth = CommonLinkViabilityLogic.getBirthDateFromBirthRecord(birth_record);
            final LocalDate date_of_parents_marriage = CommonLinkViabilityLogic.getMarriageDateFromMarriageRecord(marriage_record);

            final long years_from_marriage_to_birth = date_of_parents_marriage.until(date_of_child_birth, ChronoUnit.YEARS);

            return years_from_marriage_to_birth >= LinkageConfig.MIN_MARRIAGE_BIRTH_DIFFERENCE &&
                    years_from_marriage_to_birth <= LinkageConfig.MAX_MARRIAGE_BIRTH_DIFFERENCE;

        } catch (NumberFormatException e) {
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
    public long getNumberOfGroundTruthTrueLinks() {
        return getNumberOfGroundTruthLinksAsymmetric();
    }

    @Override
    public double getThreshold() {
        return DISTANCE_THRESHOLD;
    }

    @Override
    public Metric<LXP> getCompositeMetric() {
        return new Sigma( getBaseMetric(),getLinkageFields(),ID_FIELD_INDEX1 );
    }
}
