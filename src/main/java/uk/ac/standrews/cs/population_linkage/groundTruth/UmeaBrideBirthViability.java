package uk.ac.standrews.cs.population_linkage.groundTruth;

import uk.ac.standrews.cs.population_linkage.ApplicationProperties;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BrideBirthIdentityLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;

import java.io.IOException;
import java.nio.file.Path;

/* Performs linkage analysis on data from births and marriages.
 * It compares the baby's and parents' names on a birth record with the bride's and her parents' names on a marriage record.
 * The fields used for comparison are listed in getComparisonFields() and getComparisonFields2().
 * This is identity linkage between the baby on a birth record and the bride on a marriage record.
 * The ground truth is listed in isTrueLink.
 **/
public class UmeaBrideBirthViability extends UmeaBrideBirth {

    public UmeaBrideBirthViability(Path store_path, String repo_name, int number_of_records_to_be_checked, int number_of_runs) throws IOException {
        super(store_path, repo_name, number_of_records_to_be_checked, number_of_runs);
    }

    public boolean isViableLink( RecordPair proposedLink) {
        return BrideBirthIdentityLinkageRecipe.isViable(proposedLink);
    }

    public static void main(String[] args) throws Exception {

        Path store_path = ApplicationProperties.getStorePath();
        String repo_name = "umea";
        int NUMBER_OF_RUNS = 1;

        new UmeaBrideBirth(store_path, repo_name, DEFAULT_NUMBER_OF_RECORDS_TO_BE_CHECKED, NUMBER_OF_RUNS).run();
    }
}
