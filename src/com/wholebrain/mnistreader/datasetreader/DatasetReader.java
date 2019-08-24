package com.wholebrain.mnistreader.datasetreader;

import com.wholebrain.mnistreader.ProgressDialog;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DatasetReader{
    private final SimpleObjectProperty<File > currentFile = new SimpleObjectProperty<>();
    private final BooleanProperty hasOpenFile = new SimpleBooleanProperty(),
            hasLabels = new SimpleBooleanProperty();
    private int magicNumber, numberOfImages, numberOfRows, numberOfColumns;
    private boolean needsTransformation;
    private char[] labelsChars;
    private Map<Integer, Character> byteToCharMapping = new HashMap<>();
    private Map<Character, List<Integer>> charToImageIndexMapping = new LinkedHashMap<>();
    private ReadingMethod readingMethod = ReadingMethod.DiskMethod;
    private MethodReader methodReader = new DiskReader();

    /**
     * An enum to tell which method to use when accessing the ImageBuffers.
     */
    public enum ReadingMethod{
        DiskMethod,
        RamMethod
    }

    public DatasetReader(){
        currentFile.addListener(e->hasOpenFile.setValue(currentFile.get()!=null));
    }

    /**
     * Sets the reading method.
     * @param method DiskMethod = access to file via {@link BufferedInputStream}.
     *               RamMethod = direct acces to the memory.
     */
    public void setMethod(ReadingMethod method){
        readingMethod = method;
        switch(method){
            case DiskMethod:
                methodReader = new DiskReader();
                break;
            case RamMethod:
                methodReader = new RamReader();
                break;
        }
    }

    // All explicit getters.

    public final int getRowCount(){
        return numberOfRows;
    }
    public final int getColumnCount(){
        return numberOfColumns;
    }
    public final int getPixelCount(){
        return numberOfRows*numberOfColumns;
    }
    private boolean isNeedsTransformation(){
        return needsTransformation;
    }
    public final char getLabel(int index){
        return labelsChars!=null?labelsChars[index]:'?';
    }
    public final char[] getLabels(int[] indices){
        char[] labels = new char[indices.length];
        for (int i = 0; i< indices.length; i++)
            labels[i] = getLabel(indices[i]);
        return labels;
    }
    public final char getCharForIndex(int index){
        return labelsChars[index];
    }
    public final int getNumberOfImages() {
        return numberOfImages;
    }
    public final Set<Character> getCharSet(){
        return charToImageIndexMapping.keySet();
    }
    public final BooleanProperty hasLabelsProperty(){
        return hasLabels;
    }
    public final File getCurrentFile(){return currentFile.get();}
    public final BooleanProperty hasOpenFile(){
        return hasOpenFile;
    }
    public final byte[][] getImageBuffers(int[] shownIndices) {
        return methodReader.getImageBuffers(shownIndices);
    }

    public final byte[][] getAllImageBuffersForChars(List<Character> listOfChars) {
        return methodReader.getAllImageBuffersForChars(listOfChars);
    }

    public final byte[][] getAllImageBuffers() {
        return methodReader.getAllImageBuffers();
    }
    public final ReadingMethod getReadingMethod(){
        return readingMethod;
    }

    public final void setCurrentFile(File file){
        if(file!=null && (currentFile.get()==null || !file.getPath().equals(currentFile.get().getPath()))){
            currentFile.set(file);
            getMetaInfos();
        }
    }

    /**
     * Returns all the indexes corresponding with the provided char.
     * @param c char to extract the indexes.
     * @return List of indexes.
     */
    public final List<Integer> getIndicesForChar(char c) {
        return charToImageIndexMapping.get(c);
    }

    /**
     * Gets and stores the meta infos about the current DataSet.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void getMetaInfos(){
        BufferedInputStream bis = null;
        byte[] buffer = new byte[4];

        try{
            bis = new BufferedInputStream(new FileInputStream(currentFile.get()));
            bis.read(buffer);
            magicNumber = bytesToInt(buffer);
            bis.read(buffer);
            numberOfImages = bytesToInt(buffer);
            bis.read(buffer);
            numberOfRows = bytesToInt(buffer);
            bis.read(buffer);
            numberOfColumns = bytesToInt(buffer);
            printMetaInfos();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(bis !=null)
                try{
                    bis.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
        }

        needsTransformation = currentFile.get().getName().contains("emnist");

        loadLabels();
    }

    /**
     * Reads the labels file associated with the current image file and save it in a byte array.
     * If the number of images is different from the number of labels, the byte array will remain null.
     */
    private void loadLabels() {
        labelsChars = null;
        hasLabels.setValue(false);
        charToImageIndexMapping.clear();
        byteToCharMapping.clear();

        File labelFile = new File(getLabelsFileName(currentFile.get()));
        if (!labelFile.exists())
            return;

        BufferedInputStream bis = null;
        byte[] labelsBytes = null;
        try {
            bis = new BufferedInputStream((new FileInputStream(labelFile)));
            bis.readNBytes(4);
            byte[] buffer = new byte[4];
            //noinspection ResultOfMethodCallIgnored
            bis.read(buffer);
            if (bytesToInt(buffer) != numberOfImages)
                return;
            labelsBytes = bis.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null)
                    bis.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (labelsBytes == null || labelsBytes.length != numberOfImages)
            labelsChars = null;
        else {
            loadByteToCharMapping();
            labelsChars = new char[labelsBytes.length];
            hasLabels.set(true);
            for (int i = 0; i < labelsBytes.length; i++) {
                labelsChars[i] = byteToCharMapping.get((int) labelsBytes[i]);
                if (charToImageIndexMapping.containsKey(labelsChars[i]))
                    charToImageIndexMapping.get(labelsChars[i]).add(i);
                else {
                    List<Integer> newList = new ArrayList<>();
                    newList.add(i);
                    charToImageIndexMapping.put(labelsChars[i], newList);
                }
            }
        }
    }

    /**
     * Maps the instructions about correspondancy between byte with characters from the dataset label file.
     */
    private void loadByteToCharMapping(){
        byteToCharMapping.clear();
        if(currentFile.get().getPath().lastIndexOf("emnist")==-1) {
            System.out.println("No loading because not an Emnist dataset.");
            defaultMapping();
            return;
        }
        String mappingType = currentFile.get().getName().split("-")[1];
        File mappingFile = new File(currentFile.get().getParent()+File.separator+"emnist-"+mappingType+"-mapping.txt");
        if (!mappingFile.exists()) {
            System.out.println("No loading because the file \""+ mappingFile.getPath()+"\" doesn't exist.");
            defaultMapping();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(mappingFile))){
            String line = br.readLine();
            String[] splittedLine;
            while(line!=null){
                splittedLine = line.split(" ");
                byteToCharMapping.put(Integer.valueOf(splittedLine[0]),(char)Integer.parseInt(splittedLine[1]));
                line=br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Mapping file \""+mappingFile.getPath()+"\" has been loaded.");
        System.out.println(byteToCharMapping);
    }

    /**
     * Default byte-character mapping.
     */
    private void defaultMapping(){
        for (int i = 0; i<10;i++){
            byteToCharMapping.put(i,(char)(i+48));
        }
        System.out.println("Default mapping has been loaded.");
        System.out.println(byteToCharMapping);
    }

    /**
     * Shows the Meta Infos inside the console.
     */
    private void printMetaInfos(){
        System.out.println("Magic Number = "+magicNumber);
        System.out.println("Number Of Images = "+numberOfImages);
        System.out.println("Number Of Rows = "+numberOfRows);
        System.out.println("Number Of Columns = "+numberOfColumns);
    }

    /**
     * Translate an array of 4 bytes into a integer.
     * @param bytes Array of 4 bytes.
     * @return int.
     */
    private int bytesToInt(byte[] bytes){
        return bytes[0]<<24 | (bytes[1]&0xFF)<<16 | (bytes[2]&0xFF) <<8 | (bytes[3]&0xFF);
    }

    /**
     * The EMNIST dataset needs 2 transformations in order to be drawn as identifiable character :
     * 1 - A horizontal mirroring (or a vertical axial symmetry)
     * 2 - A 90° counter-clockwise rotation.
     * The transformation resulting is an axial symmetry along a descendant diagonal.
     * Modifies directly the content of the array passed as parameter.
     * @param imageBuffer Array of bytes of the image.
     */
    private void correctOrientation(byte[] imageBuffer){
        byte buffer;
        for (int y=0;y<numberOfColumns;y++) {
            for (int x = y + 1; x < numberOfRows; x++) {
                buffer= imageBuffer[x * numberOfColumns + y];
                imageBuffer[x * numberOfColumns + y] = imageBuffer[y * numberOfColumns + x];
                imageBuffer[y * numberOfColumns + x] = buffer;
            }
        }
    }

    // Utils methods

    private void skip(BufferedInputStream bufferedInputStream, int bytesToSkip){
        try {
            while(bytesToSkip>0)
            bytesToSkip-=bufferedInputStream.skip(bytesToSkip);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getLabelsFileName(File file){
        StringBuilder labelSb = new StringBuilder(file.getPath());
        labelSb.replace(labelSb.lastIndexOf("idx3"), labelSb.lastIndexOf("idx3") + 4, "idx1")
                .replace(labelSb.lastIndexOf("images"), labelSb.lastIndexOf("images") + 6, "labels");
        return labelSb.toString();
    }

    // Inner classes

    private class RamReader implements MethodReader {
        private byte[][] imageBuffers;

        private RamReader(){
            imageBuffers = new byte[getNumberOfImages()][getPixelCount()];
            new ProgressDialog("Loading file to memory...", imageBuffers.length, new Task() {
                @Override
                protected Object call() {
                    BufferedInputStream bis = null;
                    try {
                        bis = new BufferedInputStream(new FileInputStream(getCurrentFile()));
                        skip(bis, 16);
                        for (int i = 0; i < imageBuffers.length; i++) {
                            byte[] imageBuffer = new byte[getPixelCount()];
                            //noinspection ResultOfMethodCallIgnored
                            bis.read(imageBuffer);
                            imageBuffers[i] = imageBuffer;
                            updateProgress(i, imageBuffers.length - 1);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (bis != null)
                                bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            });
            if (isNeedsTransformation()) {
                new ProgressDialog("Transforming orientation...", imageBuffers.length, new Task() {
                    @Override
                    protected Object call() {
                        for (int i = 0; i < imageBuffers.length; i++) {
                            correctOrientation(imageBuffers[i]);
                            updateProgress(i, imageBuffers.length - 1);
                        }
                        return null;
                    }
                });
            }
        }

        public byte[] getImageBuffer(int index){
            return imageBuffers[index].clone();
        }

        @Override
        public byte[][] getImageBuffers(int[] indices) {
            byte[][] selectedImageBuffers = new byte[indices.length][getPixelCount()];
            for (int i = 0; i< indices.length; i++)
                selectedImageBuffers[i] = imageBuffers[indices[i]];
            return selectedImageBuffers.clone();
        }

        @Override
        public byte[][] getAllImageBuffers() {
            return imageBuffers.clone();
        }

        @Override
        public byte[][] getAllImageBuffersForChars(Collection<Character> characterSet) {
            List<Integer> indices = new ArrayList<>();
            for (char c : characterSet)
                indices.addAll(getIndicesForChar(c));

            indices.sort(Comparator.comparing(integer -> integer));
            byte[][]selectedImageBuffers = new byte[indices.size()][getPixelCount()];
            for (int i =0; i<indices.size();i++){
                selectedImageBuffers[i] = imageBuffers[indices.get(i)];
            }
            return selectedImageBuffers.clone();
        }

    }

    private class DiskReader implements MethodReader{

        /**
         * Returns the imageBuffer at a specific index.
         * @param index Index of the image in the current Dataset.
         * @return byte[]
         */
        public byte[] getImageBuffer(int index){
            BufferedInputStream bis = getStreamAtImageIndex(index);
            if (bis == null) return null;

            byte[] imageBuffer = new byte[getPixelCount()];
            try{
                //noinspection ResultOfMethodCallIgnored
                bis.read(imageBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }finally{
                try{
                    bis.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            if(isNeedsTransformation())
                correctOrientation(imageBuffer);
            return imageBuffer;
        }

        /**
         * Returns the imageBuffers of specific indices.
         * @param indices indices of the wanted imageBuffers.
         * @return Array of imageBuffers as byte[].
         */
        public byte[][] getImageBuffers(int[] indices){
            byte[][] imageBuffers = new byte[indices.length][];
            int pixelCount = getPixelCount();
            for(int i = 0; i<indices.length; i++) {
                BufferedInputStream bis = getStreamAtImageIndex(indices[i]);
                if (bis == null) return null;
                byte[] imageBuffer = new byte[pixelCount];
                try {
                    //noinspection ResultOfMethodCallIgnored
                    bis.read(imageBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                imageBuffers[i] = imageBuffer;
            }
            if(isNeedsTransformation())
                applyCorrection(imageBuffers);
            return imageBuffers;
        }

        /**
         * Returns all the ImageBuffers inside the current Dataset.
         * @return Array of ImageBuffer as byte[].
         */
        public byte[][] getAllImageBuffers() {
            byte[][] allImages = new byte[getNumberOfImages()][getPixelCount()];
            BufferedInputStream bis = getStreamAtImageIndex(0);
            if (bis == null) return null;


            try {
                for (int i = 0; i < getNumberOfImages(); i++) {
                    byte[] imageBuffer = new byte[getPixelCount()];
                    //noinspection ResultOfMethodCallIgnored
                    bis.read(imageBuffer);
                    allImages[i] = imageBuffer;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            if(isNeedsTransformation())
                applyCorrection(allImages);
            return allImages;
        }

        /**
         * Returns all the ImageBuffers corresponding to a {@link Collection<char> set of char}.
         * @param characterSet {@link Collection} of the wanted characters.
         * @return Array of ImageBuffer as byte[].
         */
        public byte[][] getAllImageBuffersForChars(Collection<Character> characterSet) {
            List<Integer> indices = new ArrayList<>();
            for (char c : characterSet)
                indices.addAll(getIndicesForChar(c));

            indices.sort(Comparator.comparing(integer -> integer));
            byte[][] imageBuffers = new byte[indices.size()][getPixelCount()];

            BufferedInputStream bis = getStreamAtImageIndex(indices.get(0));
            if (bis == null) return null;
            try {
                for (int i = 0; i <indices.size(); i++) {
                    byte[] imageBuffer = new byte[getPixelCount()];
                    //noinspection ResultOfMethodCallIgnored
                    bis.read(imageBuffer);
                    imageBuffers[i] = imageBuffer;
                    if(i<indices.size()-1 && indices.get(i+1)>indices.get(i)+1) {
                        int bytesToSkip = (indices.get(i + 1) - indices.get(i) - 1) * getPixelCount();
                        skip(bis, bytesToSkip);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(isNeedsTransformation())
                applyCorrection(imageBuffers);
            return imageBuffers;

        }

        /**
         * Creates a {@link BufferedInputStream stream} and puts its positioning index
         * to the specified index.
         * @param imageIndex Target index.
         * @return {@link BufferedInputStream} with the correct positioning index.
         */
        private BufferedInputStream getStreamAtImageIndex(int imageIndex){
            if (imageIndex >= getNumberOfImages())
                return null;
            BufferedInputStream bis = null;

            try{
                bis = new BufferedInputStream(new FileInputStream(getCurrentFile()));
                int bytesToSkip = 16+imageIndex*getPixelCount();
                while(bytesToSkip>0)
                    bytesToSkip-= bis.skip(bytesToSkip);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bis;
        }

        private void applyCorrection(byte[][] imageBuffers){
            for (byte[] imageBuffer : imageBuffers)
                correctOrientation(imageBuffer);
        }
    }
}
