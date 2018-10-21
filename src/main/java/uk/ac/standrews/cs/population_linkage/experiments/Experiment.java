package uk.ac.standrews.cs.population_linkage.experiments;

import uk.ac.standrews.cs.population_linkage.model.LinkageQuality;
import uk.ac.standrews.cs.population_linkage.model.Linker;
import uk.ac.standrews.cs.population_linkage.model.Links;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.storr.impl.LXP;

import java.util.List;

public abstract class Experiment {

    public void run() throws Exception {

        RecordRepository record_repository = getRecordRepository();

        printHeader();

        long t1 = System.currentTimeMillis();

        List<LXP> birth_sub_records = getRecords(record_repository);

        long t2 = System.currentTimeMillis();
        System.out.println((t2 - t1) / 1000 + "s to extract linkage records");

        Linker sibling_bundler = getLinker();

        Links sibling_links = sibling_bundler.link(birth_sub_records);

        long t3 = System.currentTimeMillis();
        System.out.println((t3 - t2) / 1000 + "s to link records");

        Links ground_truth_links = getGroundTruthLinks(record_repository);

        long t4 = System.currentTimeMillis();
        System.out.println((t4 - t3) / 1000 + "s to get ground truth links");

        LinkageQuality linkage_quality = evaluateLinkage(sibling_links, ground_truth_links);

        long t5 = System.currentTimeMillis();
        System.out.println((t5 - t4) / 1000 + "s to evaluate linkage");

        linkage_quality.print(System.out);
    }

    protected abstract RecordRepository getRecordRepository() throws Exception;
    protected abstract void printHeader();
    protected abstract List<LXP> getRecords(RecordRepository record_repository);
    protected abstract Linker getLinker() throws Exception;
    protected abstract Links getGroundTruthLinks(RecordRepository record_repository);
    protected abstract LinkageQuality evaluateLinkage(Links calculated_links, Links ground_truth_links);
}
