package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.linkageRunners.BitBlasterLinkageRunner;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageConfig;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_records.record_types.Death;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.storr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.utilities.metrics.JensenShannon;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DeathDeathSiblingLinkageRecipe extends LinkageRecipe {

    public static final List<Integer> COMPARISON_FIELDS = Arrays.asList(
            Death.FATHER_FORENAME,
            Death.FATHER_SURNAME,
            Death.MOTHER_FORENAME,
            Death.MOTHER_MAIDEN_SURNAME
    );

    public static void main(String[] args) throws BucketException {

        String sourceRepo = args[0]; // e.g. synthetic-scotland_13k_1_clean
        String resultsRepo = args[1]; // e.g. synth_results

        LinkageRecipe linkageRecipe = new DeathDeathSiblingLinkageRecipe(sourceRepo, resultsRepo,
                linkageType + "-links");

        new BitBlasterLinkageRunner()
                .run(linkageRecipe, new JensenShannon(2048), 0.67, true, 5, false, false, true, false
                );
    }

    public static final String linkageType = "death-death-sibling";

    public DeathDeathSiblingLinkageRecipe(String source_repository_name, String results_repository_name, String links_persistent_name) {
        super(source_repository_name, results_repository_name, links_persistent_name);
    }

    public static LinkStatus trueMatch(LXP record1, LXP record2) {

        final String d1_mother_id = record1.getString(Death.MOTHER_IDENTITY);
        final String d2_mother_id = record2.getString(Death.MOTHER_IDENTITY);

        final String d1_father_id = record1.getString(Death.FATHER_IDENTITY);
        final String d2_father_id = record2.getString(Death.FATHER_IDENTITY);

        final String d1_mother_birth_id = record1.getString(Death.MOTHER_BIRTH_RECORD_IDENTITY);
        final String d2_mother_birth_id = record2.getString(Death.MOTHER_BIRTH_RECORD_IDENTITY);

        final String d1_father_birth_id = record1.getString(Death.FATHER_BIRTH_RECORD_IDENTITY);
        final String d2_father_birth_id = record2.getString(Death.FATHER_BIRTH_RECORD_IDENTITY);

        final String d1_parent_marriage_id = record1.getString(Death.PARENT_MARRIAGE_RECORD_IDENTITY);
        final String d2_parent_marriage_id = record2.getString(Death.PARENT_MARRIAGE_RECORD_IDENTITY);

        if (equalsNonEmpty(d1_mother_id, d2_mother_id) && equalsNonEmpty(d1_father_id, d2_father_id)) return LinkStatus.TRUE_MATCH;
        if (equalsNonEmpty(d1_mother_birth_id, d2_mother_birth_id) && equalsNonEmpty(d1_father_birth_id, d2_father_birth_id)) return LinkStatus.TRUE_MATCH;
        if (equalsNonEmpty(d1_parent_marriage_id, d2_parent_marriage_id)) return LinkStatus.TRUE_MATCH;

        if (allEmpty(d1_mother_id, d2_mother_id, d1_father_id, d2_father_id, d1_mother_birth_id, d2_mother_birth_id, d1_father_birth_id, d2_father_birth_id, d1_parent_marriage_id, d2_parent_marriage_id)) return LinkStatus.UNKNOWN;

        return LinkStatus.NOT_TRUE_MATCH;
    }

    @Override
    public LinkStatus isTrueMatch(LXP record1, LXP record2) {

        return trueMatch(record1, record2);
    }

    @Override
    public String getLinkageType() {
        return linkageType;
    }

    @Override
    public Class getStoredType() {
        return Death.class;
    }

    @Override
    public Class getSearchType() {
        return Death.class;
    }

    @Override
    public String getStoredRole() {
        return Death.ROLE_DECEASED;
    }

    @Override
    public String getSearchRole() {
        return Death.ROLE_DECEASED;
    }

    @Override
    public List<Integer> getLinkageFields() {
        return getComparisonFields();
    }

    public static List<Integer> getComparisonFields() {
        return COMPARISON_FIELDS;
    }

    @Override
    public boolean isViableLink(RecordPair proposedLink) {
        return isViable(proposedLink);
    }

    public static boolean isViable(RecordPair proposedLink) {

        if (LinkageConfig.MAX_SIBLING_AGE_DIFF == null) return true;

        try {
            int year_of_birth1 = Integer.parseInt(proposedLink.record1.getString(Death.DEATH_YEAR)) - Integer.parseInt(proposedLink.record1.getString(Death.AGE_AT_DEATH));
            int year_of_birth2 = Integer.parseInt(proposedLink.record2.getString(Death.DEATH_YEAR)) - Integer.parseInt(proposedLink.record2.getString(Death.AGE_AT_DEATH));

            return Math.abs(year_of_birth1 - year_of_birth2) <= LinkageConfig.MAX_SIBLING_AGE_DIFF;

        } catch(NumberFormatException e) { // in this case a BIRTH_YEAR is invalid
            return true;
        }
    }

    @Override
    public List<Integer> getSearchMappingFields() {
        return getLinkageFields();
    }

    @Override
    public Map<String, Link> getGroundTruthLinks() {
        return getGroundTruthLinksOnSiblingSymmetric(Death.FATHER_IDENTITY, Death.MOTHER_IDENTITY);
    }

    @Override
    public int getNumberOfGroundTruthTrueLinks() { // See comment above
        return getNumberOfGroundTruthLinksOnSiblingSymmetric(Death.FATHER_IDENTITY, Death.MOTHER_IDENTITY);
    }

    @Override
    public int getNumberOfGroundTruthTrueLinksPostFilter() {
        return getNumberOfGroundTruthLinksPostFilterOnSiblingSymmetric(Death.FATHER_IDENTITY, Death.MOTHER_IDENTITY);
    }

}
