/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.EndtoEnd.SubsetRecipies;

import uk.ac.standrews.cs.population_linkage.graph.model.Query;
import uk.ac.standrews.cs.population_linkage.graph.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthBrideIdentityLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.population_records.record_types.Marriage;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.storr.impl.exceptions.BucketException;

/**
 * EvidencePair Recipe
 * In all linkage recipies the naming convention is:
 *     the stored type is the first part of the name
 *     the query type is the second part of the name
 * So for example in BirthBrideIdentityLinkageRecipe the stored type (stored in the search structure) is a birth and Marriages are used to query.
 * In all recipes if the query and the stored types are not the same the query type is converted to a stored type using getQueryMappingFields() before querying.
 *
 */
public class BirthBrideIdentitySubsetLinkageRecipe extends BirthBrideIdentityLinkageRecipe {

    private final NeoDbCypherBridge bridge;

    private static final int NUMBER_OF_BIRTHS = 10000;
    private static final int EVERYTHING = Integer.MAX_VALUE;

    private static final int PREFILTER_REQUIRED_FIELDS = 6; // 6 is all of them


    public BirthBrideIdentitySubsetLinkageRecipe(String source_repository_name, String results_repository_name, NeoDbCypherBridge bridge, String links_persistent_name ) {
        super( source_repository_name,results_repository_name,links_persistent_name );
        this.bridge = bridge;
    }

    /**
     * @return
     */
    @Override
    protected Iterable<LXP> getBirthRecords() {
        return filter( PREFILTER_REQUIRED_FIELDS, NUMBER_OF_BIRTHS, super.getBirthRecords() , getLinkageFields() );
    }

    // NOTE Marriage not filtered in this recipe

    @Override
    public void makeLinkPersistent(Link link) {
        try {
            Query.createBirthBrideOwnMarriageReference(
                    bridge,
                    link.getRecord1().getReferend().getString( Birth.STANDARDISED_ID ),
                    link.getRecord2().getReferend().getString( Marriage.STANDARDISED_ID ),
                    String.join( "-",link.getProvenance() ),
                    PREFILTER_REQUIRED_FIELDS,
                    link.getDistance() );
        } catch (BucketException e) {
            throw new RuntimeException(e);
        }
    }

}
