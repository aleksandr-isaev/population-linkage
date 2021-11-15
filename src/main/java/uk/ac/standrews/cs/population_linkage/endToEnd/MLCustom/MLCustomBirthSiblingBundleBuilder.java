/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.endToEnd.MLCustom;

import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.LinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRunners.MakePersistent;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageQuality;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageResult;

/**
 * This class attempts to perform birth-birth sibling linkage.
 */
public class MLCustomBirthSiblingBundleBuilder implements MakePersistent {

    public static void main(String[] args) throws Exception {

        String sourceRepo = args[0]; // e.g. synthetic-scotland_13k_1_clean
        String resultsRepo = args[1]; // e.g. synth_results

        try(NeoDbCypherBridge bridge = new NeoDbCypherBridge() ) {

            MLCustomBirthSiblingLinkageRecipe linkageRecipe = new MLCustomBirthSiblingLinkageRecipe(sourceRepo, resultsRepo, bridge, MLCustomBirthSiblingBundleBuilder.class.getCanonicalName());

            CustomBitBlasterLinkageRunner runner = new CustomBitBlasterLinkageRunner();

            int linkage_fields = linkageRecipe.ALL_LINKAGE_FIELDS;
            int half_fields = linkage_fields - (linkage_fields / 2 ) + 1;

            while( linkage_fields >= half_fields ) {
                linkageRecipe.setNumberLinkageFieldsRequired(linkage_fields);
                LinkageResult lr = runner.run(linkageRecipe, new MLCustomBirthSiblingBundleBuilder(), false, false, true, false);
                LinkageQuality quality = lr.getLinkageQuality();
                quality.print(System.out);

                linkage_fields--;
            }
        } catch (Exception e) {
            System.out.println( "Runtime exception:" );
            e.printStackTrace();
        } finally {
            System.out.println("Run finished");
            System.exit(0); // make sure process dies.
        }
    }

    @Override
    public void makePersistent(LinkageRecipe linkage_recipe, Link link) {
        throw new RuntimeException( "makePersistent unimplemented" );
    }
}
