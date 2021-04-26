/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.EndtoEnd.builders;

import uk.ac.standrews.cs.population_linkage.EndtoEnd.Recipies.BirthGroomIdentitySubsetLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.graph.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthGroomIdentityLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.LinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRunners.BitBlasterLinkageRunner;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageConfig;
import uk.ac.standrews.cs.storr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.utilities.metrics.JensenShannon;

/**
 *  This class attempts to find birth-groom links: links a baby on a birth to the same person as a groom on a marriage.
 *  This is NOT STRONG: uses the 3 names: the groom/baby and the names of the mother and father.
 */
public class BirthGroomOwnMarriageExperiment {

    private static final int PREFILTER_FIELDS = 6; // 6 is all of them

    public static void main(String[] args) throws BucketException {

        String sourceRepo = args[0]; // e.g. synthetic-scotland_13k_1_clean
        String resultsRepo = args[1]; // e.g. synth_results

        try (NeoDbCypherBridge bridge = new NeoDbCypherBridge(); ) {
            LinkageRecipe linkageRecipe = new BirthGroomIdentitySubsetLinkageRecipe(sourceRepo, resultsRepo, bridge, BirthGroomIdentityLinkageRecipe.LINKAGE_TYPE + "-links", PREFILTER_FIELDS);

            LinkageConfig.numberOfROs = 20;

            new BitBlasterLinkageRunner().run(linkageRecipe, new JensenShannon(2048), 0.49, true, PREFILTER_FIELDS, false, false, true, true);
        } catch (Exception e) {
            System.out.println( "Exception closing bridge" );
        } finally {
            System.out.println( "Run finished" );
        }
    }
}