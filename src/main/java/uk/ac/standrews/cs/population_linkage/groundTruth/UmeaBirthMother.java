package uk.ac.standrews.cs.population_linkage.groundTruth;

import uk.ac.standrews.cs.population_linkage.ApplicationProperties;
import uk.ac.standrews.cs.population_linkage.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.supportClasses.Utilities;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.storr.impl.LXP;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/* Performs linkage analysis on data from births.
 * It compares the baby's names on a birth record with the mother's names on another birth record.
 * The fields used for comparison are listed in getComparisonFields() and getComparisonFields2().
 * This is identity linkage between the baby on one record and the mother on another record.
 * The ground truth is listed in isTrueLink.
 **/
public class UmeaBirthMother extends AsymmetricSingleSourceLinkageAnalysis {

    UmeaBirthMother(Path store_path, String repo_name, int number_of_records_to_be_checked, int number_of_runs) throws IOException {
        super(store_path, repo_name, getLinkageResultsFilename(), getDistanceResultsFilename(), number_of_records_to_be_checked, number_of_runs);
    }

    @Override
    public Iterable<LXP> getSourceRecords(RecordRepository record_repository) {
        return Utilities.getBirthRecords(record_repository);
    }

    @Override
    protected LinkStatus isTrueMatch(LXP record1, LXP record2) {

        final String child_id = record1.getString(Birth.CHILD_IDENTITY);
        final String mother_id = record2.getString(Birth.MOTHER_IDENTITY);

        if (child_id.isEmpty() || mother_id.isEmpty()) return LinkStatus.UNKNOWN;

        return child_id.equals(mother_id) ? LinkStatus.TRUE_MATCH : LinkStatus.NOT_TRUE_MATCH;
    }

    @Override
    String getDatasetName() {
        return "Umea";
    }

    @Override
    String getLinkageType() {
        return "identity linkage between baby on birth record and mother on birth record";
    }

    @Override
    protected String getSourceType() {
        return "births";
    }

    @Override
    public List<Integer> getComparisonFields() {
        return Arrays.asList(
                Birth.FORENAME,
                Birth.SURNAME);
    }

    @Override
    public List<Integer> getComparisonFields2() {
        return Arrays.asList(
                Birth.MOTHER_FORENAME,
                Birth.MOTHER_MAIDEN_SURNAME);
    }

    public static void main(String[] args) throws Exception {

        Path store_path = ApplicationProperties.getStorePath();
        String repo_name = "umea";
        int NUMBER_OF_RUNS = 1;

        new UmeaBirthMother(store_path, repo_name, DEFAULT_NUMBER_OF_RECORDS_TO_BE_CHECKED, NUMBER_OF_RUNS).run();
    }
}
