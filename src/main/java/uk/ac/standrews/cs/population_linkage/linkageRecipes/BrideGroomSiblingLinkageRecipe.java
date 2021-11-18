/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_records.record_types.Marriage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uk.ac.standrews.cs.population_linkage.helpers.RecordFiltering.filter;

/**
 * Links a person appearing as the bride on a marriage record with a sibling appearing as the groom on another marriage record.
 */
public class BrideGroomSiblingLinkageRecipe extends LinkageRecipe {

    private static final double DISTANCE_THRESHOLD = 0.12;

    public static final String LINKAGE_TYPE = "bride-groom-sibling";

    public static final int ID_FIELD_INDEX1 = Marriage.STANDARDISED_ID;
    public static final int ID_FIELD_INDEX2 = Marriage.STANDARDISED_ID;

    private int NUMBER_OF_MARRIAGES;
    public static final int ALL_LINKAGE_FIELDS = 4;
    private ArrayList<LXP> cached_records;

    // TODO Why not father occupation?
    public static final List<Integer> LINKAGE_FIELDS = list(
            Marriage.BRIDE_MOTHER_FORENAME,
            Marriage.BRIDE_MOTHER_MAIDEN_SURNAME,
            Marriage.BRIDE_FATHER_FORENAME,
            Marriage.BRIDE_FATHER_SURNAME
    );

    public static final List<Integer> SEARCH_FIELDS = list(
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
            list(   pair(Marriage.BRIDE_MOTHER_IDENTITY, Marriage.GROOM_MOTHER_IDENTITY),
                    pair(Marriage.BRIDE_FATHER_IDENTITY, Marriage.GROOM_FATHER_IDENTITY)),
            list(   pair(Marriage.BRIDE_MOTHER_BIRTH_RECORD_IDENTITY, Marriage.GROOM_MOTHER_BIRTH_RECORD_IDENTITY),
                    pair(Marriage.BRIDE_FATHER_BIRTH_RECORD_IDENTITY, Marriage.GROOM_FATHER_BIRTH_RECORD_IDENTITY))
    );

    public BrideGroomSiblingLinkageRecipe(String source_repository_name, String number_of_records, String links_persistent_name, NeoDbCypherBridge bridge) {
        super(source_repository_name, links_persistent_name, bridge);
        if( number_of_records.equals(EVERYTHING_STRING) ) {
            NUMBER_OF_MARRIAGES = EVERYTHING;
        } else {
            NUMBER_OF_MARRIAGES = Integer.parseInt(number_of_records);
        }
        setNoLinkageFieldsRequired( ALL_LINKAGE_FIELDS );
    }

    @Override
    protected Iterable<LXP> getMarriageRecords() {
        if (cached_records == null) {
            cached_records = filter(getNoLinkageFieldsRequired(), NUMBER_OF_MARRIAGES, super.getMarriageRecords(), getLinkageFields());
        }
        return cached_records;
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
    public String getStoredRole() { return Marriage.ROLE_BRIDE; }

    @Override
    public String getQueryRole() { return Marriage.ROLE_GROOM; }

    @Override
    public List<Integer> getLinkageFields() {
        return LINKAGE_FIELDS;
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return isViable( proposedLink );
    }

    public static boolean isViable(RecordPair proposedLink) {

        try {
            final LocalDate date_of_birth1 = CommonLinkViabilityLogic.getBirthDateFromMarriageRecord(proposedLink.stored_record, true);
            final LocalDate date_of_birth2 = CommonLinkViabilityLogic.getBirthDateFromMarriageRecord(proposedLink.query_record, false);

            return CommonLinkViabilityLogic.siblingBirthDatesAreViable(date_of_birth1, date_of_birth2);

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
    public long getNumberOfGroundTruthTrueLinks() {
        return getNumberOfGroundTruthLinksAsymmetric();
    }

    @Override
    public double getThreshold() {
        return DISTANCE_THRESHOLD;
    }
}
