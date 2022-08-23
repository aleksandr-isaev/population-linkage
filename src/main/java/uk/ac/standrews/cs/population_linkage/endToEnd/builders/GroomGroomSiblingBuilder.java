/*
 * Copyright 2022 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 *
 * This file is part of the module population-linkage.
 *
 * population-linkage is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * population-linkage is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with population-linkage. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.population_linkage.endToEnd.builders;

import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.population_linkage.graph.Query;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.GroomGroomSiblingLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.LinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRunners.BitBlasterLinkageRunner;
import uk.ac.standrews.cs.population_linkage.linkageRunners.MakePersistent;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageQuality;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageResult;
import uk.ac.standrews.cs.population_records.record_types.Marriage;

/**
 * This class attempts to perform marriage-marriage sibling linkage.
 */
public class GroomGroomSiblingBuilder implements MakePersistent {

    public static void main(String[] args) throws Exception {

        String sourceRepo = args[0]; // e.g. synthetic-scotland_13k_1_clean
        String number_of_records = args[1]; // e.g. EVERYTHING or 10000 etc.

        try(BitBlasterLinkageRunner runner = new BitBlasterLinkageRunner();
            GroomGroomSiblingLinkageRecipe linkageRecipe = new GroomGroomSiblingLinkageRecipe(sourceRepo, number_of_records, GroomGroomSiblingBuilder.class.getName()) ) {

            int linkage_fields = linkageRecipe.ALL_LINKAGE_FIELDS;
            int half_fields = linkage_fields - (linkage_fields / 2) + 1;

            while (linkage_fields >= half_fields) {
                linkageRecipe.setNumberLinkageFieldsRequired(linkage_fields);
                LinkageResult lr = runner.run(linkageRecipe, new GroomGroomSiblingBuilder(), false, true);
                LinkageQuality quality = lr.getLinkageQuality();
                quality.print(System.out);
                linkage_fields--;
            }
        }
    }

    public void makePersistent(LinkageRecipe recipe, Link link) {
        try {
            final String std_id1 = link.getRecord1().getReferend(Marriage.class).getString(Marriage.STANDARDISED_ID);
            final String std_id2 = link.getRecord2().getReferend(Marriage.class).getString(Marriage.STANDARDISED_ID);

            if( ! Query.MMGroomGroomSiblingReferenceExists(recipe.getBridge(), std_id1, std_id2, recipe.getLinksPersistentName())) {
                Query.createMMGroomGroomSiblingReference(
                        recipe.getBridge(),
                        std_id1,
                        std_id2,
                        recipe.getLinksPersistentName(),
                        recipe.getNumberOfLinkageFieldsRequired(),
                        link.getDistance());
            }
        } catch (BucketException | RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
