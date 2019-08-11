package com.wholebrain.mnistreader;

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

@SuppressWarnings("WeakerAccess")
public class DatasetReader {

    private File currentFile;
    private int magicNumber, numberOfImages, numberOfRows, numberOfColumns;
    private boolean needsTransformation;
    private char[] labelsChars;
    private Map<Integer, Character> byteToCharMapping = new HashMap<>();
    private Map<Character, List<Integer>> charToImageIndexMapping = new LinkedHashMap<>();

    public DatasetReader(){}

    public File getCurrentFile(){
        return currentFile;
    }

    public void setCurrentFile(File file){
        if(currentFile==null || !file.getPath().equals(currentFile.getPath())){
            currentFile=file;
            getMetaInfos();
        }
    }

    // All explicit getters.
    public int getRowCount(){
        return numberOfRows;
    }
    public int getColumnCount(){
        return numberOfColumns;
    }
    public int getPixelCount(){
        return numberOfRows*numberOfColumns;
    }
    public boolean isNeedsTransformation(){
        return needsTransformation;
    }
    public char getLabel(int index){
        return labelsChars!=null?labelsChars[index]:'?';
    }
    public char getCharForIndex(int index){
        return labelsChars[index];
    }
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
        return imageBuffer;
    }
    public int getNumberOfImages() {
        return numberOfImages;
    }
    public Set<Character> getCharSet(){
        return charToImageIndexMapping.keySet();
    }
    public boolean hasLabels(){
        return labelsChars!=null;
    }
    /**
     * Returns all the indexes corresponding with the provided char.
     * @param c char to extract the indexes.
     * @return List of indexes.
     */
    public List<Integer> getIndicesForChar(char c) {
        return charToImageIndexMapping.get(c);
    }

    /**
     * Returns all the ImageBuffers inside the current Dataset.
     * @return Array of ImageBuffer as byte[].
     */
    public byte[][] getAllImageBuffers() {
        byte[][] allImages = new byte[numberOfImages][getPixelCount()];
        BufferedInputStream bis = getStreamAtImageIndex(0);
        if (bis == null) return null;


        try {
            for (int i = 0; i < numberOfImages; i++) {
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
                    while(bytesToSkip>0)
                        bytesToSkip -= bis.skip(bytesToSkip);
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

        return imageBuffers;

    }

    /**
     * Creates a {@link BufferedInputStream stream} and puts its positioning index
     * to the specified index.
     * @param imageIndex Target index.
     * @return {@link BufferedInputStream} with the correct positioning index.
     */
    private BufferedInputStream getStreamAtImageIndex(int imageIndex){
        if (imageIndex >= numberOfImages)
            return null;
        BufferedInputStream bis = null;

        try{
            bis = new BufferedInputStream(new FileInputStream(currentFile));
            int bytesToSkip = 16+imageIndex*numberOfRows*numberOfColumns;
            while(bytesToSkip>0)
                bytesToSkip-= bis.skip(bytesToSkip);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bis;
    }

    /**
     * Gets and stores the meta infos about the current DataSet.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void getMetaInfos(){
        BufferedInputStream bis = null;
        byte[] buffer = new byte[4];

        try{
            bis = new BufferedInputStream(new FileInputStream(currentFile));
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

        needsTransformation = currentFile.getName().contains("emnist");

        loadLabels();
    }

    /**
     * Reads the labels file associated with the current image file and save it in a byte array.
     * If the number of images is different from the number of labels, the byte array will remain null.
     */
    private void loadLabels() {
        labelsChars = null;
        charToImageIndexMapping.clear();
        byteToCharMapping.clear();

        File labelFile = new File(getLabelsFileName(currentFile));
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
        if(currentFile.getPath().lastIndexOf("emnist")==-1) {
            System.out.println("No loading because not an Emnist dataset.");
            defaultMapping();
            return;
        }
        String mappingType = currentFile.getName().split("-")[1];
        File mappingFile = new File(currentFile.getParent()+File.separator+"emnist-"+mappingType+"-mapping.txt");
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


    // Utils methods


    public static String getLabelsFileName(File file){
        StringBuilder labelSb = new StringBuilder(file.getPath());
        labelSb.replace(labelSb.lastIndexOf("idx3"), labelSb.lastIndexOf("idx3") + 4, "idx1")
                .replace(labelSb.lastIndexOf("images"), labelSb.lastIndexOf("images") + 6, "labels");
        return labelSb.toString();
    }
}
