package uk.ac.standrews.cs.population_linkage.groundTruth;

import uk.ac.standrews.cs.population_linkage.data.Utilities;
import uk.ac.standrews.cs.population_linkage.linkage.ApplicationProperties;
import uk.ac.standrews.cs.population_linkage.metrics.Sigma2;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.NamedMetric;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllPairsBirthMotherUmeaIdentityLinkage extends AllPairsSameSourceLinkageAnalysis {

    public AllPairsBirthMotherUmeaIdentityLinkage(Path store_path, String repo_name, String linkage_results_filename, final String distance_results_filename) throws IOException {
        super(store_path,repo_name,linkage_results_filename, distance_results_filename);
    }

    @Override
    public Iterable<LXP> getSourceRecords(RecordRepository record_repository) {
        return Utilities.getBirthRecords( record_repository );
    }

    @Override
    protected LinkStatus isTrueLink(LXP record1, LXP record2) {

        final String b1_parent_id = record1.getString(Birth.CHILD_IDENTITY);
        final String b2_parent_id = record2.getString(Birth.MOTHER_BIRTH_RECORD_IDENTITY);

        if (b1_parent_id.isEmpty() || b2_parent_id.isEmpty()) return LinkStatus.UNKNOWN;

        return b1_parent_id.equals(b2_parent_id) ? LinkStatus.TRUE_LINK : LinkStatus.NOT_TRUE_LINK;
    }

    @Override
    protected String getSourceType() {
        return "births";
    }

    @Override
    public List<Integer> getComparisonFields() {
        return Arrays.asList(
                Birth.FORENAME,
                Birth.SURNAME );
    }

    public List<Integer> getComparisonFields2() {
        return Arrays.asList(
                Birth.MOTHER_FORENAME,
                Birth.MOTHER_MAIDEN_SURNAME );
    }

    @Override
    protected List<NamedMetric<LXP>> getCombinedMetrics() {

        final List<NamedMetric<LXP>> result = new ArrayList<>();

        for (final NamedMetric<String> base_metric : Utilities.BASE_METRICS) {
            result.add(new Sigma2(base_metric, getComparisonFields(), getComparisonFields2()));
        }
        return result;
    }

    public static void main(String[] args) throws Exception {

        Path store_path = ApplicationProperties.getStorePath();
        String repo_name = "umea";

        new AllPairsBirthMotherUmeaIdentityLinkage(store_path, repo_name, "UmeaBirthMotherIdentityLinkage", "UmeaThresholdBirthMotherIdentityDistances").run();
    }


}
