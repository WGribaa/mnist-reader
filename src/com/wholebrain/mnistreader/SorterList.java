package com.wholebrain.mnistreader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A List of {@link Comparator comparators} that has both mapping and indexing capabilities.
 */
public final class SorterList{
    private final List<String> strings = new ArrayList<>();
    private final List<Comparator<Integer>> comparators= new ArrayList<>();

    public SorterList(){}

    /**
     * Adds a comaparor to the list.
     * @param string {@link String}
     * @param comparator {@link Comparator<Integer>}
     */
    public void put(final String string, final Comparator<Integer> comparator){
        strings.add(string);
        comparators.add(comparator);
    }

    /**
     * Get a comparator from his index.
     * @param index of the {@link Comparator<Integer> comparator.}
     * @return {@link Comparator<Integer>}
     */
    public Comparator<Integer> getComparator(final int index){
        if(index >= strings.size())
            return null;
        return comparators.get(index);
    }

    /**
     * Get a comparator from his {@link String}.
     * @param string {@link String} of the {@link Comparator<Integer> comparator.}
     * @return {@link Comparator<Integer>}
     */
    public Comparator<Integer> getComparator(final String string){
        if(strings.contains(string))
            return comparators.get(strings.indexOf(string));
        return null;
    }

    /**
     * Gets a {@link Comparator<Integer> comparator}'s {@link String string} from his index.
     * @param index index of the {@link Comparator<Integer> comparator}.
     * @return {@link String}
     */
    public String getString(final int index){
        if(index >= strings.size())
            return null;
        return strings.get(index);
    }

    /**
     * Gets a {@link Comparator<Integer> comparator}'s {@link String string} from his instance.
     * @param comparator Instance of the {@link Comparator<Integer> comparator}.
     * @return {@link String}
     */
    public String getString(final Comparator<Integer> comparator){
        if(comparators.contains(comparator))
            return strings.get(comparators.indexOf(comparator));
        return null;
    }

    /**
     /**
     * Gets a {@link Comparator<Integer> comparator}'s index from his {@link String string}.
     * @param {@link String} of the {@link Comparator<Integer> comparator}.
     * @return index.
     */
    public int getIndex(final String string){
        return strings.indexOf(string);
    }
    /**
     * Gets a {@link Comparator<Integer> comparator}'s index from his instance.
     * @param comparator Instance of the {@link Comparator<Integer> comparator}.
     * @return index.
     */
    public int getIndex(final Comparator comparator){
        return comparators.indexOf(comparator);
    }

    /**
     * Return the number of stored {@link Comparator<Integer> comparators}.
     * @return size.
     */
    public int size(){
        return strings.size();
    }
}
