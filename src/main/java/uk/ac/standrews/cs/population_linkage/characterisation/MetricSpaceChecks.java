package uk.ac.standrews.cs.population_linkage.characterisation;

import java.util.Arrays;
import uk.ac.standrews.cs.population_linkage.supportClasses.Utilities;
import uk.ac.standrews.cs.population_linkage.ApplicationProperties;
import uk.ac.standrews.cs.population_linkage.supportClasses.Sigma;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static uk.ac.standrews.cs.population_linkage.supportClasses.Constants.*;

public class MetricSpaceChecks {

    private static final int DUMP_COUNT_INTERVAL = 1000000;
    private static final long SEED = 34553543456223L;
    private static final double DELTA = 0.0000001;

    private final Path store_path;
    private final String repo_name;

    private final PrintStream outstream;
    private List<Metric<LXP>> combined_metrics;

    public static final List<Integer> SIBLING_BUNDLING_BIRTH_LINKAGE_FIELDS = Arrays.asList(

            Birth.FATHER_FORENAME,
            Birth.FATHER_SURNAME,
            Birth.MOTHER_FORENAME,
            Birth.MOTHER_MAIDEN_SURNAME,
            Birth.PARENTS_PLACE_OF_MARRIAGE,
            Birth.PARENTS_DAY_OF_MARRIAGE,
            Birth.PARENTS_MONTH_OF_MARRIAGE,
            Birth.PARENTS_YEAR_OF_MARRIAGE
    );

    private MetricSpaceChecks(Path store_path, String repo_name, String filename) throws Exception {

        this.store_path = store_path;
        this.repo_name = repo_name;

        if (filename.equals("stdout")) {
            outstream = System.out;
        } else {
            outstream = new PrintStream(filename);
        }

        combined_metrics = getCombinedMetrics();
    }

    public void run() throws Exception {

        checkTriangleInequality(new RecordRepository(store_path, repo_name));
    }

    private List<Metric<LXP>> getCombinedMetrics() {

        List<Metric<LXP>> result = new ArrayList<>();

        for (Metric<String> base_metric : TRUE_METRICS) {
            result.add(new Sigma(base_metric, SIBLING_BUNDLING_BIRTH_LINKAGE_FIELDS));
        }
        return result;
    }

    private void checkTriangleInequality(RecordRepository record_repository) {

        Random random = new Random(SEED);
        final List<LXP> birth_records = Utilities.permute(Utilities.getBirthRecords(record_repository));

        long counter = 0;

        while (true) {

            int size = birth_records.size();

            LXP b1 = birth_records.get(random.nextInt(size));
            LXP b2 = birth_records.get(random.nextInt(size));
            LXP b3 = birth_records.get(random.nextInt(size));

            for (Metric<LXP> metric : combined_metrics) {

                String metric_name = metric.getMetricName();

                double distance1 = metric.distance(b1, b2);
                double distance2 = metric.distance(b1, b3);
                double distance3 = metric.distance(b2, b3);

                if (violatesTriangleInequality(distance1, distance2, distance3)) {

                    outstream.println("violation of triangle inequality for " + metric_name);
                    outstream.println(b1);
                    outstream.println(b2);
                    outstream.println(b3);
                    outstream.println(distance1);
                    outstream.println(distance2);
                    outstream.println(distance3);
                    outstream.println();
                }
            }

            if (counter % DUMP_COUNT_INTERVAL == 0) {
                System.out.println("checked: " + counter);
            }

            counter++;
        }
    }

    private boolean violatesTriangleInequality(final double distance1, final double distance2, final double distance3) {

        return distance1 > distance2 + distance3 + DELTA || distance2 > distance1 + distance3 + DELTA || distance3 > distance1 + distance2 + DELTA;
    }

    public static void main(String[] args) throws Exception {

        Path store_path = ApplicationProperties.getStorePath();
        String repo_name = "skye";

        new MetricSpaceChecks(store_path, repo_name, "TriangleInequalityChecks").run();
    }
}
