/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.resolver.cluster;

import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.endToEnd.builders.BirthSiblingBundleBuilder;
import uk.ac.standrews.cs.population_linkage.endToEnd.subsetRecipes.DeathSiblingSubsetLinkageRecipe;

public class SiblingDeathClusterAllTrianglesResolver extends SiblingDeathClusterOpenTriangleResolver {

    public SiblingDeathClusterAllTrianglesResolver(NeoDbCypherBridge bridge, String source_repo_name, DeathSiblingSubsetLinkageRecipe recipe) {
        super( bridge, source_repo_name, recipe );
    }

    public static void main(String[] args) {

        DEATH_SIBLING_TRIANGLE_QUERY = "MATCH (x:Death)-[xy:SIBLING]-(y:Death)-[yz:SIBLING]-(z:Death)-[zx:SIBLING]-(x:Death) return x,y,z,xy,yz";

        String sourceRepo = args[0]; // e.g. synthetic-scotland_13k_1_clean

        try (NeoDbCypherBridge bridge = new NeoDbCypherBridge() ) {

            DeathSiblingSubsetLinkageRecipe linkageRecipe = new DeathSiblingSubsetLinkageRecipe(sourceRepo, "10000", bridge, BirthSiblingBundleBuilder.class.getCanonicalName());
            SiblingDeathClusterAllTrianglesResolver resolver = new SiblingDeathClusterAllTrianglesResolver( bridge,sourceRepo,linkageRecipe );

            printHeaders();

            for( int min_cluster = 9; min_cluster > 2; min_cluster-- ) {
                for (double hdrt = 2; hdrt < 8; hdrt += 1) {
                    for (double ldrt = 10; ldrt < 40; ldrt += 5) {
                        resolver.resolve(min_cluster,ldrt / 100d, hdrt / 10d);
                    }
                }
            }
 //              resolver.resolve();

        } catch (Exception e) {
            System.out.println( "Exception closing bridge" );
        } finally {
            System.out.println( "Run finished" );
            System.exit(0); // Make sure it all shuts down properly.
        }
    }

}
