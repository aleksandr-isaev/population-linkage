package uk.ac.standrews.cs.population_linkage.groundTruth;

import uk.ac.standrews.cs.population_linkage.data.Utilities;
import uk.ac.standrews.cs.population_linkage.linkage.ApplicationProperties;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Marriage;
import uk.ac.standrews.cs.storr.impl.LXP;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * This class performs sibling bundling linkage analysis on data from
 * marriages looking for a groom and a bride who are brother and sister
 */
public class UmeaGroomBrideSibling extends AsymmetricSingleSourceLinkageAnalysis {

    public UmeaGroomBrideSibling(Path store_path, String repo_name, String linkage_results_filename, final String distance_results_filename, int number_of_records_to_be_checked, int number_of_runs) throws IOException {
        super(store_path, repo_name, linkage_results_filename, distance_results_filename, number_of_records_to_be_checked, number_of_runs);
    }

    @Override
    public Iterable<LXP> getSourceRecords(RecordRepository record_repository) {
        return Utilities.getMarriageRecords(record_repository);
    }

    @Override
    protected LinkStatus isTrueLink(LXP record1, LXP record2) {

        final String m1_groom_father_id = record1.getString(Marriage.GROOM_FATHER_IDENTITY);
        final String m1_groom_mother_id = record2.getString(Marriage.GROOM_MOTHER_IDENTITY);
        final String m2_bride_father_id = record1.getString(Marriage.BRIDE_FATHER_IDENTITY);
        final String m2_bride_mother_id = record1.getString(Marriage.BRIDE_MOTHER_IDENTITY);

        if (m2_bride_father_id.isEmpty() || m1_groom_father_id.isEmpty() ||  m2_bride_mother_id.isEmpty() || m1_groom_mother_id.isEmpty()) {
            return LinkStatus.UNKNOWN;
        }

        return  m2_bride_father_id.equals(m1_groom_father_id) && m2_bride_mother_id.equals(m1_groom_mother_id)  ? LinkStatus.TRUE_LINK : LinkStatus.NOT_TRUE_LINK;

    }

    @Override
    protected String getSourceType() {
        return "marriages";
    }

    @Override
    public List<Integer> getComparisonFields() {
        return Arrays.asList(
                Marriage.GROOM_FATHER_FORENAME,
                Marriage.GROOM_FATHER_SURNAME,
                Marriage.GROOM_MOTHER_FORENAME,
                Marriage.GROOM_MOTHER_MAIDEN_SURNAME);
    }

    @Override
    public List<Integer> getComparisonFields2() {
        return Arrays.asList(
                Marriage.BRIDE_FATHER_FORENAME,
                Marriage.BRIDE_FATHER_SURNAME,
                Marriage.BRIDE_MOTHER_FORENAME,
                Marriage.BRIDE_MOTHER_MAIDEN_SURNAME);
    }

    public static void main(String[] args) throws Exception {

        Path store_path = ApplicationProperties.getStorePath();
        String repo_name = "umea";

        int NUMBER_OF_RUNS = 1;

        new UmeaGroomBrideSibling(store_path, repo_name, getLinkageResultsFilename(), getDistanceResultsFilename(), DEFAULT_NUMBER_OF_RECORDS_TO_BE_CHECKED, NUMBER_OF_RUNS).run();
    }
}
