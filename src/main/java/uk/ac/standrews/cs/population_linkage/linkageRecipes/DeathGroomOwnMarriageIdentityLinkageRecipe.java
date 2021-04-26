/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_records.record_types.Death;
import uk.ac.standrews.cs.population_records.record_types.Marriage;
import uk.ac.standrews.cs.storr.impl.LXP;

import java.util.List;
import java.util.Map;

/**
 * EvidencePair Recipe
 * In all linkage recipies the naming convention is:
 *     the stored type is the first part of the name
 *     the query type is the second part of the name
 * So for example in BirthBrideIdentityLinkageRecipe the stored type (stored in the search structure) is a birth and Marriages are used to query.
 * In all recipes if the query and the stored types are not the same the query type is converted to a stored type using getQueryMappingFields() before querying.
 *
 */
public class DeathGroomOwnMarriageIdentityLinkageRecipe extends LinkageRecipe {

    public static final List<Integer> LINKAGE_FIELDS = list(
            Death.FORENAME,
            Death.SURNAME,
            Death.FATHER_FORENAME,
            Death.FATHER_SURNAME,
            Death.MOTHER_FORENAME,
            Death.MOTHER_MAIDEN_SURNAME
    );

    public static final List<Integer> SEARCH_FIELDS = list(
                    Marriage.GROOM_FORENAME,
                    Marriage.GROOM_SURNAME,
                    Marriage.GROOM_FATHER_FORENAME,
                    Marriage.GROOM_FATHER_SURNAME,
                    Marriage.GROOM_MOTHER_FORENAME,
                    Marriage.GROOM_MOTHER_MAIDEN_SURNAME
    );

    public static final String LINKAGE_TYPE = "death-groom-identity";

    public static final List<List<Pair>> TRUE_MATCH_ALTERNATIVES = list(
            list(pair(Death.DECEASED_IDENTITY, Marriage.GROOM_IDENTITY))
    );

    public DeathGroomOwnMarriageIdentityLinkageRecipe(String source_repository_name, String results_repository_name, String links_persistent_name) {
        super(source_repository_name, results_repository_name, links_persistent_name, 0);
    }

    @Override
    public LinkStatus isTrueMatch(LXP death, LXP marriage) {
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
    public Class getStoredType() {
        return Death.class;
    }

    @Override
    public Class getQueryType() {
        return Marriage.class;
    }

    @Override
    public String getStoredRole() {
        return Death.ROLE_DECEASED;
    }

    @Override
    public String getQueryRole() { return Marriage.ROLE_GROOM; }

    @Override
    public List<Integer> getLinkageFields() {
        return LINKAGE_FIELDS;
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return isViable(proposedLink);
    }

    public static boolean isViable(final RecordPair proposedLink) {
        return deathMarriageIdentityLinkIsViable(proposedLink);
    }

    @Override
    public List<Integer> getQueryMappingFields() {
        return SEARCH_FIELDS;
    }

    @Override
    public Map<String, Link> getGroundTruthLinks() {
        return getGroundTruthLinksOn(Death.DECEASED_IDENTITY, Marriage.GROOM_IDENTITY);
    }

    public int getNumberOfGroundTruthTrueLinks() {
        return getNumberOfGroundTruthTrueLinksOn(Death.DECEASED_IDENTITY, Marriage.GROOM_IDENTITY);
    }

    @Override
    public int getNumberOfGroundTruthTrueLinksPostFilter() {
        return getNumberOfGroundTruthTrueLinksPostFilterOn(Death.DECEASED_IDENTITY, Marriage.GROOM_IDENTITY);
    }

    @Override
    public Iterable<LXP> getPreFilteredStoredRecords() {
        return filterBySex(
                super.getPreFilteredStoredRecords(),
                Death.SEX, "m");
    }
}