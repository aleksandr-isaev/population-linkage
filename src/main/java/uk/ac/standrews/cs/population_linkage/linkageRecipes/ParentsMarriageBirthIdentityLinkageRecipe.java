/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.population_records.record_types.Marriage;
import uk.ac.standrews.cs.utilities.archive.ErrorHandling;

import java.util.*;

/**
 * Links two people appearing as the spouses on a marriage record with the same people appearing as the parents on a birth record.
 */
public class ParentsMarriageBirthIdentityLinkageRecipe extends LinkageRecipe {

    // TODO Redundant given BirthParentsMarriageIdentityLinkageRecipe? Does different filtering though. delete

    private static final double DISTANCE_THRESHOLD = 0; // TODO ??

    public static final String LINKAGE_TYPE = "parents-marriage-birth-identity";

    // TODO Include occupation?
    public static final List<Integer> LINKAGE_FIELDS = list(
            Marriage.BRIDE_FORENAME,
            Marriage.BRIDE_SURNAME,
            Marriage.GROOM_FORENAME,
            Marriage.GROOM_SURNAME,
            Marriage.PLACE_OF_MARRIAGE,
            Marriage.MARRIAGE_DAY,
            Marriage.MARRIAGE_MONTH,
            Marriage.MARRIAGE_YEAR
    );

    public static final List<Integer> SEARCH_FIELDS = list(
            Birth.MOTHER_FORENAME,
            Birth.MOTHER_MAIDEN_SURNAME,
            Birth.FATHER_FORENAME,
            Birth.FATHER_SURNAME,
            Birth.PARENTS_PLACE_OF_MARRIAGE,
            Birth.PARENTS_DAY_OF_MARRIAGE,
            Birth.PARENTS_MONTH_OF_MARRIAGE,
            Birth.PARENTS_YEAR_OF_MARRIAGE
    );

    public ParentsMarriageBirthIdentityLinkageRecipe(String source_repository_name, String results_repository_name, String links_persistent_name) {
        super(source_repository_name, links_persistent_name);
    }

    @Override
    public LinkStatus isTrueMatch(LXP record1, LXP record2) {
        return trueMatch(record1, record2);
    }

    @Override
    public String getLinkageType() {
        return LINKAGE_TYPE;
    }

    @Override
    public Class getStoredType() {
        return Marriage.class;
    }

    @Override
    public Class<? extends LXP> getQueryType() {
        return Birth.class;
    }

    @Override
    public String getStoredRole() {
        return Marriage.ROLE_SPOUSES;  // bride and groom
    }

    @Override
    public String getQueryRole() {
        return Birth.ROLE_PARENTS;
    } // mother and father

    @Override
    public List<Integer> getLinkageFields() {
        return LINKAGE_FIELDS;
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return true;
    }

    @Override
    public List<Integer> getQueryMappingFields() {
        return SEARCH_FIELDS;
    }

    @Override
    public Map<String, Link> getGroundTruthLinks() {

        final Map<String, Link> links = new HashMap<>();

        for (LXP marriage_record : getMarriageRecords()) {

            String marriage_key_from_marriage = toKeyFromMarriage(marriage_record);

            for (LXP birth_record : getBirthRecords()) {

                String birth_key_from_marriage = toKeyFromBirth(birth_record);

                if (birth_key_from_marriage.equals(marriage_key_from_marriage)) {
                    try {
                        Link l = new Link(marriage_record, Marriage.ROLE_BRIDES_MOTHER, birth_record, Birth.ROLE_MOTHER, 1.0f, "ground truth", -1);
                        links.put(l.toString(), l);
                    } catch (PersistentObjectException e) {
                        throw new RuntimeException("PersistentObjectException adding getGroundTruthLinks");
                    }
                }
            }
        }

        return links;
    }

    private static String toKeyFromBirth(LXP birth_record) {
        return birth_record.getString(Birth.FATHER_IDENTITY) +
                "-" + birth_record.getString(Birth.MOTHER_IDENTITY);
    }

    private static String toKeyFromMarriage(LXP marriage_record) {
        return marriage_record.getString(Marriage.GROOM_IDENTITY) +
                "-" + marriage_record.getString(Marriage.BRIDE_IDENTITY);
    }

    public int getNumberOfGroundTruthTrueLinks() {

        int count = 0;

        for (LXP marriage : getMarriageRecords()) {

            String marriage_key_from_marriage = toKeyFromMarriage(marriage);

            for (LXP birth : getBirthRecords()) {

                String birth_key_from_marriage = toKeyFromBirth(birth);

                if (birth_key_from_marriage.equals(marriage_key_from_marriage)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public Iterable<LXP> getStoredRecords() {

        Collection<LXP> filteredMarriageRecords = new HashSet<>();

        for (LXP record : getMarriageRecords()) {

            String groomForename = record.getString(Marriage.GROOM_FORENAME).trim();
            String groomSurname = record.getString(Marriage.GROOM_SURNAME).trim();
            String brideForename = record.getString(Marriage.BRIDE_FORENAME).trim();
            String brideSurname = record.getString(Marriage.BRIDE_SURNAME).trim();

            String pom = record.getString(Marriage.PLACE_OF_MARRIAGE).trim();
            String dom = record.getString(Marriage.MARRIAGE_DAY).trim();
            String mom = record.getString(Marriage.MARRIAGE_MONTH).trim();
            String yom = record.getString(Marriage.MARRIAGE_YEAR).trim();


            int populatedFields = 0;

            if (!(groomForename.equals("") || groomForename.equals("missing"))) {
                populatedFields++;
            }
            if (!(groomSurname.equals("") || groomSurname.equals("missing"))) {
                populatedFields++;
            }
            if (!(brideForename.equals("") || brideForename.equals("missing"))) {
                populatedFields++;
            }
            if (!(brideSurname.equals("") || brideSurname.equals("missing"))) {
                populatedFields++;
            }
            if (!(pom.equals("") || pom.equals("missing"))) {
                populatedFields++;
            }
            if (!(dom.equals("") || dom.equals("missing"))) {
                populatedFields++;
            }
            if (!(mom.equals("") || mom.equals("missing"))) {
                populatedFields++;
            }
            if (!(yom.equals("") || yom.equals("missing"))) {
                populatedFields++;
            }

            if (populatedFields >= requiredNumberOfPreFilterFields()) {
                filteredMarriageRecords.add(record);
            } // else reject record for linkage - not enough info
        }
        return filteredMarriageRecords;
    }

    private int requiredNumberOfPreFilterFields() {
        return 5;
    }

    @Override
    public Iterable<LXP> getQueryRecords() {

        HashSet<LXP> filteredBirthRecords = new HashSet<>();

        for (LXP record : getBirthRecords()) {

            String fatherForename = record.getString(Birth.FATHER_FORENAME).trim();
            String fatherSurname = record.getString(Birth.FATHER_SURNAME).trim();
            String motherForename = record.getString(Birth.MOTHER_FORENAME).trim();
            String motherSurname = record.getString(Birth.MOTHER_SURNAME).trim();

            String pom = record.getString(Birth.PARENTS_PLACE_OF_MARRIAGE).trim();
            String dom = record.getString(Birth.PARENTS_DAY_OF_MARRIAGE).trim();
            String mom = record.getString(Birth.PARENTS_MONTH_OF_MARRIAGE).trim();
            String yom = record.getString(Birth.PARENTS_YEAR_OF_MARRIAGE).trim();

            int populatedFields = 0;

            if (!(fatherForename.equals("") || fatherForename.equals("missing"))) {
                populatedFields++;
            }
            if (!(fatherSurname.equals("") || fatherSurname.equals("missing"))) {
                populatedFields++;
            }
            if (!(motherForename.equals("") || motherForename.equals("missing"))) {
                populatedFields++;
            }
            if (!(motherSurname.equals("") || motherSurname.equals("missing"))) {
                populatedFields++;
            }
            if (!(pom.equals("") || pom.equals("missing"))) {
                populatedFields++;
            }
            if (!(dom.equals("") || dom.equals("missing"))) {
                populatedFields++;
            }
            if (!(mom.equals("") || mom.equals("missing"))) {
                populatedFields++;
            }
            if (!(yom.equals("") || yom.equals("missing"))) {
                populatedFields++;
            }

            if (populatedFields >= requiredNumberOfPreFilterFields()) {
                filteredBirthRecords.add(record);
            } // else reject record for linkage - not enough info
        }
        return filteredBirthRecords;
    }

    public String toKey(LXP query_record, LXP stored_record) {
        String s1 = stored_record.getString(Marriage.ORIGINAL_ID);
        String s2 = query_record.getString(Birth.ORIGINAL_ID);

        if (s1.compareTo(s2) < 0)
            return s1 + "-" + s2;
        else
            return s2 + "-" + s1;
    }

    @Override
    public double getThreshold() {
        return DISTANCE_THRESHOLD;
    }

    public static LinkStatus trueMatch(LXP birth, LXP marriage) {

        if (birth.getString(Birth.FATHER_IDENTITY).isEmpty() ||
                birth.getString(Birth.MOTHER_IDENTITY).isEmpty() ||
                marriage.getString(Marriage.GROOM_IDENTITY).isEmpty() ||
                marriage.getString(Marriage.BRIDE_IDENTITY).isEmpty()) {

            return LinkStatus.UNKNOWN;
        }
        String birth_key_from_marriage = toKeyFromBirth(birth);
        String marriage_key_from_marriage = toKeyFromMarriage(marriage);

        if (marriage_key_from_marriage.equals(birth_key_from_marriage)) {
            return LinkStatus.TRUE_MATCH;
        } else {
            return LinkStatus.NOT_TRUE_MATCH;
        }
    }
}