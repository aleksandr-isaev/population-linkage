package uk.ac.standrews.cs.population_linkage.al.experiments;

import uk.ac.standrews.cs.population_linkage.data.Utilities;
import uk.ac.standrews.cs.population_linkage.groundTruth.LinkStatus;
import uk.ac.standrews.cs.population_linkage.linkage.ApplicationProperties;
import uk.ac.standrews.cs.population_linkage.model.Link;
import uk.ac.standrews.cs.population_linkage.model.Role;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.storr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.utilities.archive.ErrorHandling;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static uk.ac.standrews.cs.population_linkage.groundTruth.LinkStatus.TRUE_MATCH;

public class UmeaBirthBirthSiblingLinkage extends Linkage {

    Path store_path = ApplicationProperties.getStorePath();

    private final String results_repository_name;
    private final String links_persistent_name;
    private final String gt_persistent_name;
    private final String source_repository_name;
    private final RecordRepository record_repository;

    public UmeaBirthBirthSiblingLinkage(String results_repository_name, String links_persistent_name, String gt_persistent_name, String source_repository_name, RecordRepository record_repository) {
        this.results_repository_name = results_repository_name;
        this.links_persistent_name = links_persistent_name;
        this.gt_persistent_name = gt_persistent_name;
        this.source_repository_name = source_repository_name;
        this.record_repository = record_repository;
    }

    @Override
    public Iterable<LXP> getSourceRecords1() {
        return Utilities.getBirthRecords(record_repository);
    }

    @Override
    public Iterable<LXP> getSourceRecords2() {
        return Utilities.getBirthRecords(record_repository);
    }

    @Override
    public LinkStatus isTrueMatch(LXP record1, LXP record2) {
        return Utilities.isTrueMatchBirthSiblingUmea(record1, record2);
    }

    @Override
    public String getDatasetName() {
        return  "Umea";
    }

    @Override
    public String getLinkageType() {
        return "sibling bundling between babies on birth records";
    }

    @Override
    public String getSourceType1() {
        return "births";
    }

    @Override
    public String getSourceType2() {
        return "births";
    }

    @Override
    public List<Integer> getLinkageFields1() {
        return Utilities.SIBLING_BUNDLING_BIRTH_LINKAGE_FIELDS;
    }

    @Override
    public List<Integer> getLinkageFields2() {
        return Utilities.SIBLING_BUNDLING_BIRTH_LINKAGE_FIELDS;
    }

    @Override
    public Role makeRole1(LXP lxp) throws PersistentObjectException {
        return new Role(lxp.getThisRef(), Birth.ROLE_BABY);
    }

    @Override
    public Role makeRole2(LXP lxp) throws PersistentObjectException {
        return new Role(lxp.getThisRef(), Birth.ROLE_BABY);
    }

    @Override
    public Set<Link> getGroundTruthLinks() {

        final Set<Link> links = new HashSet<>();

        final List<LXP> records = new ArrayList<>();

        for (LXP lxp : record_repository.getBirths()) {
            records.add(lxp);
        }

        final int number_of_records = records.size();

        for (int i = 0; i < number_of_records; i++) {

            for (int j = i + 1; j < number_of_records; j++) {

                LXP record1 = records.get(i);
                LXP record2 = records.get(j);

                try {
                    if (isTrueMatch(record1, record2).equals(TRUE_MATCH)) {
                        links.add(new Link(makeRole1(record1), makeRole2(record2), 1.0f, "ground truth"));
                    }
                } catch (PersistentObjectException e) {
                    ErrorHandling.error( "PersistentObjectException adding getGroundTruthLinks" );
                }
            }
        }

        return links;
    }

    @Override
    public void makeLinksPersistent(Iterable<Link> links) {
        makePersistentUsingStor( store_path, results_repository_name, links_persistent_name, links );  // use makePersistentUsingStor or makePersistentUsingFile
    }

    @Override
    public void makeGroundTruthPersistent(Iterable<Link> links) {
        makePersistentUsingStor( store_path, results_repository_name, gt_persistent_name, links ); // use makePersistentUsingStor or makePersistentUsingFile
    }
}
