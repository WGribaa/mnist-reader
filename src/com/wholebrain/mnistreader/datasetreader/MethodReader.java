package com.wholebrain.mnistreader.datasetreader;

import java.util.Collection;

public interface MethodReader {
    /**
     * Returns the imageBuffer at a specific index.
     * @param index Index of the image in the current Dataset.
     * @return byte[]
     */
    byte[] getImageBuffer(int index);
    /**
     * Returns the imageBuffers of specific indices.
     * @param indices indices of the wanted imageBuffers.
     * @return Array of imageBuffers as byte[].
     */
    byte[][] getImageBuffers(int[] indices);
    /**
     * Returns all the ImageBuffers inside the current Dataset.
     * @return Array of ImageBuffer as byte[].
     */
    byte[][] getAllImageBuffers();
    /**
     * Returns all the ImageBuffers corresponding to a {@link Collection <char> set of char}.
     * @param characterSet {@link Collection} of the wanted characters.
     * @return Array of ImageBuffer as byte[].
     */
    byte[][] getAllImageBuffersForChars(Collection<Character> characterSet);
}
