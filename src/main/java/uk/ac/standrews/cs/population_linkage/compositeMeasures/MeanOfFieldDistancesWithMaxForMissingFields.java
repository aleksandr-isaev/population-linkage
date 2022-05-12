/*
 * Copyright 2022 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.compositeMeasures;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthDeathIdentityLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthSiblingLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.supportClasses.Constants;
import uk.ac.standrews.cs.utilities.measures.coreConcepts.StringMeasure;

import java.util.List;

/**
 * Defines a distance function between LXP records as the mean of the distances between values of specified non-empty fields,
 * using a defined maximum distance for fields where either value is missing. The maximum distance is constrained to be one
 * where the base measure is intrinsically normalised, but may be sampled from the dataset for non-normalised measures or
 * defined arbitrarily.
 */
public class MeanOfFieldDistancesWithMaxForMissingFields extends LXPMeasure {

    private final double max_field_distance;

    public MeanOfFieldDistancesWithMaxForMissingFields(final StringMeasure base_measure, final List<Integer> field_list, final double max_field_distance) {

        this(base_measure, field_list, field_list, max_field_distance);
    }

    public MeanOfFieldDistancesWithMaxForMissingFields(final StringMeasure base_measure, final List<Integer> field_list1, final List<Integer> field_list2, final double max_field_distance) {

        super(base_measure, field_list1, field_list2);
        if (maxDistanceIsOne() && max_field_distance != 1d) throw new RuntimeException("invalid max distance");

        this.max_field_distance = max_field_distance;
    }

    @Override
    public String getMeasureName() {
        return "Mean of field distances (treating missing fields as max distance) using: " + base_measure.getMeasureName();
    }

    @Override
    public boolean maxDistanceIsOne() {
        return base_measure.maxDistanceIsOne();
    }

    @Override
    public double calculateDistance(final LXP x, final LXP y) {

        return calculateMeanDistance(x, y, max_field_distance);
    }

    public static void main(String[] args) {

        final MeanOfFieldDistancesWithMaxForMissingFields birth_birth_measure1 = new MeanOfFieldDistancesWithMaxForMissingFields(Constants.LEVENSHTEIN, BirthSiblingLinkageRecipe.LINKAGE_FIELDS, 100d);
        final MeanOfFieldDistancesWithMaxForMissingFields birth_death_measure1 = new MeanOfFieldDistancesWithMaxForMissingFields(Constants.LEVENSHTEIN, BirthDeathIdentityLinkageRecipe.LINKAGE_FIELDS, BirthDeathIdentityLinkageRecipe.SEARCH_FIELDS, 100d);

        final MeanOfFieldDistancesWithMaxForMissingFields birth_birth_measure2 = new MeanOfFieldDistancesWithMaxForMissingFields(Constants.SED, BirthSiblingLinkageRecipe.LINKAGE_FIELDS, 1d);
        final MeanOfFieldDistancesWithMaxForMissingFields birth_death_measure2 = new MeanOfFieldDistancesWithMaxForMissingFields(Constants.SED, BirthDeathIdentityLinkageRecipe.LINKAGE_FIELDS, BirthDeathIdentityLinkageRecipe.SEARCH_FIELDS, 1d);

        printExamples(birth_birth_measure1, birth_death_measure1);
        printExamples(birth_birth_measure2, birth_death_measure2);
    }
}