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
package uk.ac.standrews.cs.population_linkage.aleks.resolvers;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Death;

import java.time.LocalDate;
import java.util.*;

public class OpenTriangleClusterDD extends OpenTriangleCluster {
    private IBucket deaths;

    public OpenTriangleClusterDD(long x, List<List<Long>> triangleChain, String recordRepo) {
        super(x, triangleChain);
        RecordRepository record_repository = new RecordRepository(recordRepo);
        deaths = record_repository.getBucket("death_records");
    }

    @Override
    public void getYearStatistics() throws BucketException {
        for (List<Long> chain : triangleChain){
            LXP[] tempKids = {(LXP) deaths.getObjectById(x), (LXP) deaths.getObjectById(chain.get(0)), (LXP) deaths.getObjectById(chain.get(1))};
            for (int i = 0; i < tempKids.length; i++) {
                if (!children.contains(tempKids[i])) {
                    int year = 1850;
                    int month = 1;
                    int day = 1;

                    try{
                        year = Integer.parseInt((tempKids[i].getString(Death.DATE_OF_BIRTH)).substring(6));
                        month = Integer.parseInt((tempKids[i].getString(Death.DATE_OF_BIRTH)).substring(3, 5));
                        day = Integer.parseInt((tempKids[i].getString(Death.DATE_OF_BIRTH)).substring(0, 2));

                        birthDays.put(tempKids[i].getString(Death.STANDARDISED_ID), LocalDate.of(year, month, day));
                    }catch(Exception e){
                        if(!children.isEmpty()){
                            year = (int) Math.round(yearTotal/children.size());
                        }
                    }

                    yearTotal += year;

                    children.add(tempKids[i]);
                }
            }
        }

        //https://deveshsharmablogs.wordpress.com/2013/07/16/find-most-common-element-in-a-list-in-java/
        int maxValue = -1;
        for(Map.Entry<String, Integer> entry: birthplaceMap.entrySet()) {
            if(entry.getValue() > maxValue) {
                mostCommonBirthplace = entry.getKey();
                maxValue = entry.getValue();
            }
        }

        yearAvg = yearTotal / children.size();

        List<LocalDate> sortedBirthDays = new ArrayList<>(birthDays.values());
        Collections.sort(sortedBirthDays);

        if(!sortedBirthDays.isEmpty()){
            ageRange = sortedBirthDays.get(sortedBirthDays.size() - 1).getYear() - sortedBirthDays.get(0).getYear();

            if ((sortedBirthDays.size() % 2) == 0) {
                yearMedian = ((sortedBirthDays.get(sortedBirthDays.size() / 2)).getYear() + (sortedBirthDays.get(sortedBirthDays.size() / 2 - 1)).getYear()) / 2;
            }else {
                yearMedian = sortedBirthDays.get(sortedBirthDays.size() / 2).getYear();
            }
        }else{
            ageRange = 0;
            yearMedian = (int) yearAvg;
        }
    }
}
