/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.EndtoEnd.SubsetRecipies;

import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.population_linkage.graph.model.Query;
import uk.ac.standrews.cs.population_linkage.graph.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthParentsMarriageLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.population_records.record_types.Marriage;
import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;

/**
 * EvidencePair Recipe
 * In all linkage recipies the naming convention is:
 *     the stored type is the first part of the name
 *     the query type is the second part of the name
 * So for example in BirthBrideIdentityLinkageRecipe the stored type (stored in the search structure) is a birth and Marriages are used to query.
 * In all recipes if the query and the stored types are not the same the query type is converted to a stored type using getQueryMappingFields() before querying.
 *
 */

public class BirthParentsMarriageSubsetLinkageRecipe extends BirthParentsMarriageLinkageRecipe {

    public static final int ALL_LINKAGE_FIELDS = 8;

    private static final int NUMBER_OF_BIRTHS = 10000;
    private static final int EVERYTHING = Integer.MAX_VALUE;
    private final NeoDbCypherBridge bridge;

    public int linkage_fields = ALL_LINKAGE_FIELDS;


    public BirthParentsMarriageSubsetLinkageRecipe(String source_repository_name, String results_repository_name, NeoDbCypherBridge bridge, String links_persistent_name ) {
        super( source_repository_name,results_repository_name,links_persistent_name );
        this.bridge = bridge;
    }

    public void setNumberLinkageFieldsRequired( int number ) {
        linkage_fields = number;
    }


    /**
     * @return
     */
    @Override
    protected Iterable<LXP> getBirthRecords() {
        return filter(linkage_fields, NUMBER_OF_BIRTHS, super.getBirthRecords(), getLinkageFields());
    }

    // NOTE that Marriage fields are not filtered in this recipe.

    @Override
    public void makeLinkPersistent(Link link) {
        try {
            Query.createBMFatherReference(
                    bridge,
                    link.getRecord1().getReferend().getString( Birth.STANDARDISED_ID ),
                    link.getRecord2().getReferend().getString( Marriage.STANDARDISED_ID ),
                    links_persistent_name,
                    linkage_fields,
                    link.getDistance() );
            Query.createBMMotherReference(bridge,
                    link.getRecord1().getReferend().getString( Birth.STANDARDISED_ID ),
                    link.getRecord2().getReferend().getString( Marriage.STANDARDISED_ID ),
                    links_persistent_name,
                    linkage_fields,
                    link.getDistance() );
        } catch (BucketException | RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
