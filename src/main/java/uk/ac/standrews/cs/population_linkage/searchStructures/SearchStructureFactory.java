/*
 * Copyright 2022 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.searchStructures;

import java.util.List;

public interface SearchStructureFactory<T> {

    String getSearchStructureType();

    SearchStructure<T> newSearchStructure(Iterable<T> stored_set, List<T> reference_objects);

    SearchStructure<T> newSearchStructure(Iterable<T> stored_set);
}
