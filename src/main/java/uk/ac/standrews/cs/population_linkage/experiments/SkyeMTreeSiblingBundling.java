package uk.ac.standrews.cs.population_linkage.experiments;

import uk.ac.standrews.cs.population_linkage.data.Utilities;
import uk.ac.standrews.cs.population_linkage.linkage.ApplicationProperties;
import uk.ac.standrews.cs.population_linkage.metrics.Sigma;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.StringMetric;

import java.nio.file.Path;

public class SkyeMTreeSiblingBundling extends MTreeSiblingBundling {

    private static final double MATCH_THRESHOLD = 4.0;
    private static final int NUMBER_OF_PROGRESS_UPDATES = 100;

    private SkyeMTreeSiblingBundling(Path store_path, String repo_name) {

        super(store_path, repo_name);
    }

    public static void main(String[] args) throws Exception {

        final Path store_path = ApplicationProperties.getStorePath();
        final String repository_name = "skye";

        new SkyeMTreeSiblingBundling(store_path, repository_name).run();
    }

    @Override
    protected StringMetric getBaseMetric() {

        return Utilities.JENSEN_SHANNON;
    }

    @Override
    protected Metric<LXP> getCompositeMetric() {

        return new Sigma(getBaseMetric(), getMatchFields());
    }

    @Override
    protected double getMatchThreshold() {

        return MATCH_THRESHOLD;
    }

    @Override
    protected int getNumberOfProgressUpdates() {

        return NUMBER_OF_PROGRESS_UPDATES;
    }
}
