/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.EndtoEnd.Recipies;

import uk.ac.standrews.cs.population_linkage.graph.model.Query;
import uk.ac.standrews.cs.population_linkage.graph.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.DeathSiblingLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_records.record_types.Death;
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
public class DeathSiblingSubsetLinkageRecipe extends DeathSiblingLinkageRecipe {

    private static final int NUMBER_OF_DEATHS = 10000;
    private static final int EVERYTHING = Integer.MAX_VALUE;
    private final NeoDbCypherBridge bridge;

    public DeathSiblingSubsetLinkageRecipe(String source_repository_name, String results_repository_name, NeoDbCypherBridge bridge, String links_persistent_name, int prefilterRequiredFields) {
        super( source_repository_name,results_repository_name,links_persistent_name,prefilterRequiredFields );
        this.bridge = bridge;
    }

    /**
     * @return the death records to be used in this recipe
     */
    @Override
    protected Iterable<LXP> getDeathRecords() {
        return filter(prefilterRequiredFields, NUMBER_OF_DEATHS, super.getDeathRecords(), getLinkageFields());
    }

    @Override
    public void makeLinkPersistent(Link link) {
        try {
            Query.createDDSiblingReference(
                    bridge,
                    link.getRecord1().getReferend().getString( Death.STANDARDISED_ID ),
                    link.getRecord2().getReferend().getString( Death.STANDARDISED_ID ),
                    String.join( "-",link.getProvenance() ),
                    prefilterRequiredFields,
                    link.getDistance() );
        } catch (BucketException e) {
            throw new RuntimeException(e);
        }
    }
}
