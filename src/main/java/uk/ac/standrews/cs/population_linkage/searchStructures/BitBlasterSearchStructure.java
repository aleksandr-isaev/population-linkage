package uk.ac.standrews.cs.population_linkage.searchStructures;

import uk.ac.standrews.cs.utilities.metrics.JensenShannon;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.DataDistance;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;
import uk.al_richard.metricbitblaster.production.ParallelBitBlaster2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BitBlasterSearchStructure<T> implements SearchStructure<T> {

    // TOM - was 20
    private static final int DEFAULT_NUMBER_OF_REFERENCE_POINTS = 70;
    private static long SEED = 34258723425L;
    private ParallelBitBlaster2<T> bit_blaster;

    public BitBlasterSearchStructure(Metric<T> distance_metric, Iterable<T> data) {
        this(distance_metric, data, DEFAULT_NUMBER_OF_REFERENCE_POINTS);
    }

    public BitBlasterSearchStructure(Metric<T> distance_metric, Iterable<T> data, int numberOfReferenceObjects) {
        List<T> copy_of_data = copyData(data);

        boolean initiliased = false;
        int tries = 0;

        int maxTries = 5;

        while(!initiliased && tries < maxTries) {
            try {
                init(distance_metric, chooseRandomReferencePoints(copy_of_data, numberOfReferenceObjects), copy_of_data);
                initiliased = true;
            } catch (Exception e) {
                tries++;
                SEED = SEED * 17 + 23; // These magic numbers were carefully chosen by Prof. al
                System.out.println("Initilisation exception - trying again with diferent reference points - new seed: " + SEED);
            }
        }

        if(tries == maxTries)
            throw new RuntimeException("Failed to init");

    }

    public BitBlasterSearchStructure(Metric<T> distance_metric, List<T> reference_points, Iterable<T> data) {

        boolean initiliased = false;
        int tries = 0;

        while(!initiliased && tries < 4) {
            try {
                init(distance_metric, reference_points, copyData(data));
                initiliased = true;
            } catch (Exception e) {
                System.out.println("Initilisation exception - trying again with diferent reference points");
                tries++;
                SEED = SEED * 17 + 23; // These magic numbers were carefully chosen by Prof. al
            }
        }

    }

    public void terminate() {
        bit_blaster.terminate();
    }

    private void init(final Metric<T> distance_metric, final List<T> reference_points, final List<T> data) throws Exception {

        boolean fourPoint = distance_metric.getMetricName().equals(JensenShannon.metricName);

        bit_blaster = new ParallelBitBlaster2<>(distance_metric::distance, reference_points, data, 2,
                Runtime.getRuntime().availableProcessors(), fourPoint, true);


    }

    @Override
    public List<DataDistance<T>> findWithinThreshold(final T record, final double threshold) {

        try {
            return bit_blaster.rangeSearch(record, threshold);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <X> List<X> copyData(final Iterable<X> data) {

        List<X> copy_of_data = new ArrayList<>();

        for (X x : data) {
            copy_of_data.add(x);
        }
        return copy_of_data;
    }

    public static <X> List<X> chooseRandomReferencePoints(final List<X> data, int number_of_reference_points) {

        Random random = new Random(SEED);
        List<X> reference_points = new ArrayList<>();

        if (number_of_reference_points >= data.size()) {
            return data;
        }

        while (reference_points.size() < number_of_reference_points) {
            X item = data.get(random.nextInt(data.size()));
            if (!reference_points.contains(item)) {
                reference_points.add(item);
            }
        }

        return reference_points;
    }
}