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
package uk.ac.standrews.cs.population_linkage.groundTruthML;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.population_linkage.compositeMeasures.LXPMeasure;
import uk.ac.standrews.cs.population_linkage.supportClasses.Utilities;
import uk.ac.standrews.cs.population_records.RecordRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * This class performs linkage analysis on data pulled from a single data sources, for example births.
 */
public abstract class SingleSourceWeightedLinkageAnalysis extends WeightedThresholdAnalysis {

    protected SingleSourceWeightedLinkageAnalysis(final String repo_name, final String linkage_results_filename, final String distance_results_filename, final int number_of_records_to_be_checked, final int number_of_runs, final boolean allow_multiple_links, double threshold) throws IOException {

        super(repo_name, linkage_results_filename, distance_results_filename, number_of_records_to_be_checked, number_of_runs, allow_multiple_links, threshold);
    }

    @Override
    public void setupRecords() {

        if (verbose) System.out.println("Reading records from repository: " + repo_name);

        final RecordRepository record_repository = new RecordRepository(repo_name);
        final Iterable<LXP> records = getSourceRecords(record_repository);

        if (verbose) System.out.println("Randomising record order");

        source_records = Utilities.permute(records, new Random(SEED));
        number_of_records = number_of_records_to_be_checked == CHECK_ALL_RECORDS ? source_records.size() : number_of_records_to_be_checked;
    }

    @Override
    public void processRecord(final int record_index, final LXPMeasure measure) {

        processRecord(record_index, number_of_records, source_records, source_records, measure, true);
    }

    @Override
    public void printMetaData() {

        linkage_results_metadata_writer.println("Output file created: " + LocalDateTime.now());
        linkage_results_metadata_writer.println("Checking quality of linkage using various string similarity measures and thresholds");
        linkage_results_metadata_writer.println("Dataset: " + getDatasetName());
        linkage_results_metadata_writer.println("EvidencePair type: " + getLinkageType());
        linkage_results_metadata_writer.println("Records: " + getSourceType());
        linkage_results_metadata_writer.flush();

        distance_results_metadata_writer.println("Output file created: " + LocalDateTime.now());
        distance_results_metadata_writer.println("Checking distributions of record pair distances using various string similarity measures and thresholds");
        distance_results_metadata_writer.println("Dataset: " + getDatasetName());
        distance_results_metadata_writer.println("EvidencePair type: " + getLinkageType());
        distance_results_metadata_writer.println("Records: " + getSourceType());
        distance_results_metadata_writer.flush();
    }
}
