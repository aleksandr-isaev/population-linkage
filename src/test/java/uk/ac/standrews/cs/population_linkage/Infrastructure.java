package uk.ac.standrews.cs.population_linkage;

import org.junit.Test;
import uk.ac.standrews.cs.population_linkage.importers.DataSetImporter;
import uk.ac.standrews.cs.population_linkage.importers.RecordRepository;
import uk.ac.standrews.cs.population_linkage.importers.kilmarnock.KilmarnockDataSetImporter;
import uk.ac.standrews.cs.population_linkage.record_types.Birth;
import uk.ac.standrews.cs.population_linkage.record_types.Death;
import uk.ac.standrews.cs.population_linkage.record_types.Marriage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class Infrastructure {

    @Test
    public void loadAndRead() throws Exception {

        Path store_path = Files.createTempDirectory("");
        String repo_name = "kilmarnock_repository";
        Path birth_records_path = Paths.get("/uk/ac/standrews/cs/population_linkage/data/kilmarnock/kilmarnock_births.csv");
        Path death_records_path = Paths.get("/uk/ac/standrews/cs/population_linkage/data/kilmarnock/kilmarnock_deaths.csv");
        Path marriage_records_path = Paths.get("/uk/ac/standrews/cs/population_linkage/data/kilmarnock/kilmarnock_marriages.csv");

        DataSetImporter importer = new KilmarnockDataSetImporter(store_path, repo_name, true, birth_records_path, death_records_path, marriage_records_path);

        int births_loaded = importer.importBirthRecords();
        int deaths_loaded = importer.importDeathRecords();
        int marriages_loaded = importer.importMarriageRecords();

        RecordRepository record_repository = new RecordRepository(store_path, repo_name);

        int births_read = 0;
        for (long object_id : record_repository.births.getOids()) {

            Birth birth = record_repository.births.getObjectById(object_id);
            assertNotNull(birth);
            births_read++;
        }

        int deaths_read = 0;
        for (long object_id : record_repository.deaths.getOids()) {

            Death death = record_repository.deaths.getObjectById(object_id);
            assertNotNull(death);
            deaths_read++;
        }

        int marriages_read = 0;
        for (long object_id : record_repository.marriages.getOids()) {

            Marriage marriage = record_repository.marriages.getObjectById(object_id);
            assertNotNull(marriage);
            marriages_read++;
        }

        assertEquals(births_loaded, births_read);
        assertEquals(deaths_loaded, deaths_read);
        assertEquals(marriages_loaded, marriages_read);
    }
}
