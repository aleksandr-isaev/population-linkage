/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRecipes;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageConfig;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_records.Normalisation;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.population_records.record_types.Death;
import uk.ac.standrews.cs.population_records.record_types.Marriage;

import java.time.DateTimeException;
import java.time.LocalDate;

public class SiblingMarriageHelper {

    protected static int getBirthYearOfPersonBeingMarried(final LXP record, final boolean spouse_is_bride) {

        final String age_or_birth_date1 = record.getString(spouse_is_bride ? Marriage.BRIDE_AGE_OR_DATE_OF_BIRTH : Marriage.GROOM_AGE_OR_DATE_OF_BIRTH);

        try {
            final int age_at_marriage_recorded = Integer.parseInt(age_or_birth_date1);
            final int marriage_year = Integer.parseInt(record.getString(Marriage.MARRIAGE_YEAR));

            return marriage_year - age_at_marriage_recorded;

        } catch (NumberFormatException e) {

            // Probably date of birth recorded rather than age.
            return Integer.parseInt(Normalisation.extractYear(age_or_birth_date1));
        }
    }

    public static boolean spouseBirthIdentityLinkIsViable(final RecordPair proposedLink, final boolean spouse_is_bride) {

        try {
            int marriage_day = Integer.parseInt(proposedLink.record1.getString(Marriage.MARRIAGE_DAY));
            int marriage_month = Integer.parseInt(proposedLink.record1.getString(Marriage.MARRIAGE_MONTH));
            int marriage_year = Integer.parseInt(proposedLink.record1.getString(Marriage.MARRIAGE_YEAR));

            int birth_day = Integer.parseInt(proposedLink.record2.getString(Birth.BIRTH_DAY));
            int birth_month = Integer.parseInt(proposedLink.record2.getString(Birth.BIRTH_MONTH));
            int birth_year = Integer.parseInt(proposedLink.record2.getString(Birth.BIRTH_YEAR));

            LocalDate birth_date_from_birth_record = LocalDate.of(birth_year, birth_month, birth_day);
            LocalDate marriage_date_from_marriage_record = LocalDate.of(marriage_year, marriage_month, marriage_day);

            int age_at_marriage_calculated = birth_date_from_birth_record.until(marriage_date_from_marriage_record).getYears();

            final String age_or_birth_date = proposedLink.record1.getString(spouse_is_bride ? Marriage.BRIDE_AGE_OR_DATE_OF_BIRTH : Marriage.GROOM_AGE_OR_DATE_OF_BIRTH);
            int age_discrepancy;

            try {

                int age_at_marriage_recorded = Integer.parseInt(age_or_birth_date);
                age_discrepancy = Math.abs(age_at_marriage_calculated - age_at_marriage_recorded);

            } catch (NumberFormatException e) {

                // Probably date of birth recorded rather than age.
                LocalDate birth_date_from_marriage_record = Normalisation.parseDate(age_or_birth_date);
                age_discrepancy = Math.abs(birth_date_from_birth_record.until(birth_date_from_marriage_record).getYears());
            }

            return age_at_marriage_calculated >= LinkageConfig.MIN_AGE_AT_MARRIAGE && age_discrepancy <= LinkageConfig.MAX_ALLOWABLE_MARRIAGE_AGE_DIFFERENCE;

        } catch (NumberFormatException e ) { // in this case a BIRTH_YEAR or MARRIAGE_YEAR or GROOM_AGE_OR_DATE_OF_BIRTH is invalid
            return true;
        } catch (DateTimeException e) { // getting a date we cannot parse like --/--/--
            return true;
        }
    }

    protected static boolean  deathMarriageIdentityLinkIsViable(final RecordPair proposedLink) {

        try {
            int year_of_death = Integer.parseInt(proposedLink.record1.getString(Death.DEATH_YEAR));
            int year_of_marriage = Integer.parseInt(proposedLink.record2.getString(Marriage.MARRIAGE_YEAR));

            return year_of_death >= year_of_marriage; // is death after marriage

        } catch (NumberFormatException e) { // in this case a DEATH_YEAR or MARRIAGE_YEAR is invalid
            return true;
        }
    }

    protected static boolean birthParentIdentityLinkIsViable(final RecordPair proposedLink) {

        try {
            int parent_year_of_birth = Integer.parseInt(proposedLink.record1.getString(Birth.BIRTH_YEAR));
            int child_year_of_birth = Integer.parseInt(proposedLink.record2.getString(Birth.BIRTH_YEAR));

            int parent_age_at_child_birth = child_year_of_birth - parent_year_of_birth;

            return parent_age_at_child_birth >= LinkageConfig.MIN_PARENT_AGE_AT_BIRTH && parent_age_at_child_birth <= LinkageConfig.MAX_PARENT_AGE_AT_BIRTH;

        } catch (NumberFormatException e) {
            return true; // a YOB is missing or in an unexpected format
        }
    }
}
