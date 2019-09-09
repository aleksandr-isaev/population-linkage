package uk.ac.standrews.cs.population_linkage.linkageRunners;

import uk.ac.standrews.cs.population_linkage.linkageRecipies.LinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRecipies.BirthBirthSiblingLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkers.Linker;
import uk.ac.standrews.cs.population_linkage.linkers.SimilaritySearchLinker;
import uk.ac.standrews.cs.population_linkage.searchStructures.BitBlasterSearchStructureFactory;
import uk.ac.standrews.cs.population_linkage.searchStructures.SearchStructureFactory;
import uk.ac.standrews.cs.population_linkage.supportClasses.*;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkagePostFilter;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.utilities.metrics.JensenShannon;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;

public class BirthBirthSiblingLinkageRunner extends LinkageRunner {

    public static final String linkageType = "birth-birth-sibling";

    protected LinkageRecipe getLinkage(final String links_persistent_name, final String gt_persistent_name,
                                       final String source_repository_name, final String results_repository_name,
                                       final RecordRepository record_repository) {

        return new BirthBirthSiblingLinkageRecipe(results_repository_name, links_persistent_name, gt_persistent_name, source_repository_name, record_repository);
    }

    protected Linker getLinker(final double match_threshold, final Metric<LXP> composite_metric, final SearchStructureFactory<LXP> search_factory) {
        return new SimilaritySearchLinker(search_factory, composite_metric, match_threshold, getNumberOfProgressUpdates(),
                linkageType, "threshold match at " + match_threshold, Birth.ROLE_BABY, Birth.ROLE_BABY, LinkagePostFilter::isViableBBSiblingLink);
    }

    protected Metric<LXP> getCompositeMetric(final LinkageRecipe linkageRecipe) {
        return new Sigma(getBaseMetric(), linkageRecipe.getLinkageFields1());
    }

    protected SearchStructureFactory<LXP> getSearchFactory(final Metric<LXP> composite_metric) {
        return new BitBlasterSearchStructureFactory<>(composite_metric, 50);
    }

    public static void main(String[] args) {

        String sourceRepo = args[0]; // e.g. synthetic-scotland_13k_1_clean
        String resultsRepo = args[1]; // e.g. synth_results

        double match_threshold = 0.67;                          // from R metric power table [FRobustness2] - original 2.03 remapped to 0.67 by normalisation.

        new BirthBirthSiblingLinkageRunner()
                .run("BirthBirthSiblingLinks", "BirthBirthSiblingGroundTruth",
                        sourceRepo, resultsRepo,
                        match_threshold, new JensenShannon(2048),
                        true, true, true, true);

    }
}
