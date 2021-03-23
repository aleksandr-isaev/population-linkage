/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.EndtoEnd.experiments;

import uk.ac.standrews.cs.population_linkage.EndtoEnd.runners.BitBlasterSubsetOfDataEndtoEndSiblingBundleLinkageRunner;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthSiblingLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.LinkageRecipe;
import uk.ac.standrews.cs.storr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.utilities.metrics.JensenShannon;

/**
 * This class attempts to perform birth-birth sibling linkage.
 * It creates a Map of families indexed (at the momement TODO) from birth ids to families
 */
public class BirthSiblingBundleExperimentDifferentFieldsRequred {

    public static void main(String[] args) throws BucketException {

        String sourceRepo = args[0]; // e.g. synthetic-scotland_13k_1_clean
        String resultsRepo = args[1]; // e.g. synth_results

        LinkageRecipe linkageRecipe = new BirthSiblingLinkageRecipe(sourceRepo, resultsRepo, BirthSiblingLinkageRecipe.LINKAGE_TYPE + "-links");

        for( int req_fields = 0; req_fields <= 8; req_fields ++ ) {
            System.out.println( "Number of fields required = " + req_fields);
            new BitBlasterSubsetOfDataEndtoEndSiblingBundleLinkageRunner().run(linkageRecipe, new JensenShannon(2048), 0.67, true, req_fields, false, false, false, false);
        }
    }
}