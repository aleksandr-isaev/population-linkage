/*
 * Copyright 2022 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.compositeMetrics;


import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.StringMetric;

import java.util.List;

/**
 * SigmaMissingHalf function for combining metrics - compares a single set of fields
 * For missing fields returns 0.5
 * Created by al on 30/9/2021
 */
public class SigmaMeanOfPresent extends Metric<LXP> {

    final StringMetric base_distance;
    final List<Integer> field_list;
    final int id_field_index;

    public SigmaMeanOfPresent(final StringMetric base_metric, final List<Integer> field_list, final int id_field_index) {

        this.base_distance = base_metric;
        this.field_list = field_list;
        this.id_field_index = id_field_index;
    }

    @Override
    public double calculateDistance(final LXP a, final LXP b) {
        
        double total_distance = 0.0d;
        int present_count = 0;

        for (int field_index : field_list) {
            try {
                String field_value1 = a.getString(field_index);
                String field_value2 = b.getString(field_index);

                if( isMissing(field_value1) || isMissing(field_value2) ) {
                    // do nothing
                } else {
                    present_count++;
                    total_distance += base_distance.distance(field_value1, field_value2);
                }

            } catch (Exception e) {
                printExceptionDebug(a, b, field_index);
                throw new RuntimeException("exception comparing field " + a.getMetaData().getFieldName(field_index) + " in records \n" + a + "\n and \n" + b, e);
            }
        }

        if( present_count > 0 ) {
            return normaliseArbitraryPositiveDistance(total_distance / present_count);
        } else {
            return 1;
        }
    }

    private boolean isMissing(String value) {
        return value.equals("") || value.contains("missing") || value.equals("--") || value.equals("----");
    }

    @Override
    public String getMetricName() {
        return base_distance.getMetricName();
    }

    private void printExceptionDebug(final LXP a, final LXP b, final int field_index) {

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println("Exception in distance calculation");
        System.out.println("field index list: " + field_index);
        System.out.println("a: " + (a == null ? "null" : "not null"));
        System.out.println("b: " + (b == null ? "null" : "not null"));
        System.out.println("id of a: " + a.getString(id_field_index));
        System.out.println("id of b: " + b.getString(id_field_index));
        System.out.println("field name a: " + a.getMetaData().getFieldName(field_index));
        System.out.println("field name b: " + b.getMetaData().getFieldName(field_index));
        System.out.println("field value a: " + a.getString(field_index));
        System.out.println("field value b: " + b.getString(field_index));
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }
}
