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
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_records.record_types.Death;
import uk.ac.standrews.cs.population_records.record_types.Marriage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Links a person appearing as the deceased on a death record with the same person appearing as the bride on a marriage record.
 */
public class DeathBrideIdentityLinkageRecipe extends LinkageRecipe {

    public static final double DISTANCE_THRESHOLD = 0.49;

    public static final String LINKAGE_TYPE = "death-bride-identity";

    private int NUMBER_OF_DEATHS = EVERYTHING;
    public static final int ALL_LINKAGE_FIELDS = 6; // 6 is all of them but not occupation - FORENAME,SURNAME,FATHER_FORENAME,FATHER_SURNAME,MOTHER_FORENAME,MOTHER_SURNAME
    private ArrayList<LXP> cached_records = null;

    public static final List<Integer> LINKAGE_FIELDS = list(
            Death.FORENAME,
            Death.SURNAME,
            Death.MOTHER_FORENAME,
            Death.MOTHER_MAIDEN_SURNAME,
            Death.FATHER_FORENAME,
            Death.FATHER_SURNAME
    );

    public static final List<Integer> SEARCH_FIELDS = list(
            Marriage.BRIDE_FORENAME,
            Marriage.BRIDE_SURNAME,
            Marriage.BRIDE_MOTHER_FORENAME,
            Marriage.BRIDE_MOTHER_MAIDEN_SURNAME,
            Marriage.BRIDE_FATHER_FORENAME,
            Marriage.BRIDE_FATHER_SURNAME
    );

    @SuppressWarnings("unchecked")
    public static final List<List<Pair>> TRUE_MATCH_ALTERNATIVES = list(
            list(pair(Death.DECEASED_IDENTITY, Marriage.BRIDE_IDENTITY))
    );

    public DeathBrideIdentityLinkageRecipe(String source_repository_name, String number_of_records, String links_persistent_name, NeoDbCypherBridge bridge) {
        super(source_repository_name, links_persistent_name, bridge);
        if( number_of_records.equals(EVERYTHING_STRING) ) {
            NUMBER_OF_DEATHS = EVERYTHING;
        } else {
            NUMBER_OF_DEATHS = Integer.parseInt(number_of_records);
        }
        setNoLinkageFieldsRequired(ALL_LINKAGE_FIELDS);
    }

    @Override
    protected Iterable<LXP> getDeathRecords() {
        if( cached_records == null ) {
            Iterable<LXP> filtered = filterBySex(super.getDeathRecords(), Death.SEX, "f");
            cached_records = RecordFiltering.filter( getNoLinkageFieldsRequired(), NUMBER_OF_DEATHS, filtered, getLinkageFields() );
        }
        return cached_records;
    }

    @Override
    public LinkStatus isTrueMatch(LXP death, LXP marriage)  {
        return trueMatch(death,marriage);
    }

    public static LinkStatus trueMatch(LXP death, LXP marriage) {
        return trueMatch(death, marriage, TRUE_MATCH_ALTERNATIVES);
    }

    @Override
    public String getLinkageType() {
        return LINKAGE_TYPE;
    }

    @Override
    public Class<? extends LXP> getStoredType() {
        return Death.class;
    }

    @Override
    public Class<? extends LXP> getQueryType() {
        return Marriage.class;
    }

    @Override
    public String getStoredRole() {
        return Death.ROLE_DECEASED;
    }

    @Override
    public String getQueryRole() { return Marriage.ROLE_BRIDE; }

    @Override
    public List<Integer> getLinkageFields() { return LINKAGE_FIELDS; }

    @Override
    public boolean isViableLink(RecordPair proposedLink) { return isViable(proposedLink); }

    public static boolean isViable(final RecordPair proposedLink) {
        return CommonLinkViabilityLogic.deathMarriageIdentityLinkIsViable(proposedLink, true);
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
        return DISTANCE_THRESHOLD;
    }
}
