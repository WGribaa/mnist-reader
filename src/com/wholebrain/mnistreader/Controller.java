package com.wholebrain.mnistreader;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;


public class Controller implements Initializable {
    public MenuItem open_menu, close_menu;
    public Canvas canvas;
    public Label index_label;
    public ScrollBar index_scrollbar;
    public ColorPicker empty_color_picker, full_color_picker;
    public TextField jumpto_textfield;

    private Stage primaryStage;
    private File currentFile;
    private int magicNumber, numberOfImages, numberOfRows, numberOfColumns, currentImageIndex = 0;
    private Color[] pallet = new Color[256];
    private boolean needsTransformation = false;

    /**
     * Closes the application.
     */
    @FXML
    public void on_close(){
        primaryStage.close();
    }

    /**
     * Open a dialog to load a dataset.
     */
    @FXML
    public void on_open() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All compatible files","*.idx3-ubyte","*-idx3-ubyte"),
                new FileChooser.ExtensionFilter("Mnist DataSet files (*.idx3-ubyte)","*.idx3-ubyte"),
                new FileChooser.ExtensionFilter("Emnist DataSet files ","*-idx3-ubyte"));
        fileChooser.setInitialDirectory(currentFile != null ?currentFile.getParentFile(): null);
        File file= fileChooser.showOpenDialog(null);
        if(file !=null) {
            currentFile = file;
            primaryStage.setTitle("Datasets Images Reader" + " : " + currentFile.getName());
            getMetaInfos();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        index_scrollbar.setMax(0);
        index_scrollbar.valueProperty().addListener((observable, oldValue, newValue) ->
            updateIndex(newValue.intValue())
        );

        EventHandler<ActionEvent> colorChangedEvent = event -> {
            initializePalet();
            paint();
        };

        empty_color_picker.setValue(Color.WHITE);
        empty_color_picker.setOnAction(colorChangedEvent);
        empty_color_picker.getCustomColors().add(Color.WHITE);

        full_color_picker.setValue(Color.BLACK);
        full_color_picker.setOnAction(colorChangedEvent);
        full_color_picker.getCustomColors().add(Color.BLACK);

        initializePalet();

        jumpto_textfield.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getText();
            if(!text.matches("\\d*"))
                return null;
            return change;
        }));
        jumpto_textfield.textProperty().addListener((observable, oldValue, newValue) -> {
            int newInt=0;
            try {
                newInt = Integer.parseInt(newValue);
            } catch (NumberFormatException e) {
                jumpto_textfield.setText(oldValue);
//                throw new NumberFormatException("The input number \""+newValue+"\" is out of Integer range.");
            }
            if (newInt < 0)
                newInt = 0;
            else if (newInt >= numberOfImages)
                newInt = numberOfImages - 1;
            updateIndex(newInt);
        });

    }

    /**
     * Stores a link to the {@link Stage primary stage} to be able to close it
     * or to display a model {@link Dialog dialogs} over it.
     * @param primaryStage Main {@link Stage window}.
     */
    void setStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Update the displayed infos about the current image index.
     */
    private void updateIndex(int newIndex){
        if(currentFile==null || numberOfImages==0) return;
        index_label.setText(String.valueOf(currentImageIndex));
        currentImageIndex = newIndex;
        jumpto_textfield.setText(String.valueOf(newIndex));
        index_scrollbar.setValue(newIndex);
        // Corrects the orientation of the imageBuffer if it comes from an EMNIST dataset.
        needsTransformation = currentFile.getName().contains("-idx3-ubyte");
        paint();
    }

    /**
     * Gets and stores the meta infos about the current DataSet.
     */
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
        index_scrollbar.setMax(numberOfImages-1);
        index_scrollbar.setBlockIncrement(numberOfImages/20);
        updateIndex(0);
    }

    /**
     * Displays a colored representation of the current image on the {@link Canvas canvas}.
     */
    private void paint(){
        BufferedInputStream bis = jumpToIndex(currentImageIndex);
        if (bis == null) return;

        byte[] imageBuffer = new byte[numberOfRows*numberOfColumns];
        try{
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

        if (needsTransformation) correctOrientation(imageBuffer);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double resolution = Double.min(canvas.getHeight()/numberOfRows, canvas.getWidth()/numberOfColumns);
        gc.setFill(empty_color_picker.getValue());
        gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        for(int y = 0; y<numberOfRows; y++)
            for (int x = 0; x<numberOfColumns; x++){
                gc.setFill(pallet[imageBuffer[y*numberOfRows+x]&0xFF]);
                gc.fillRect(x*resolution, y*resolution,resolution, resolution);
            }
    }

    /**
     * Creates a {@link BufferedInputStream stream} and puts its positioning index
     * to the specified index.
     * @param index Target index.
     * @return {@link BufferedInputStream} with the correct positioning index.
     */
    private BufferedInputStream jumpToIndex(int index){
        if (index >= numberOfImages)
            return null;
        BufferedInputStream bis = null;

        try{
            bis = new BufferedInputStream(new FileInputStream(currentFile));
            bis.skip(16+index*numberOfRows*numberOfColumns);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bis;
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
     * Shows the Meta Infos inside the console.
     */
    private void printMetaInfos(){
        System.out.println("Magic Number = "+magicNumber);
        System.out.println("Number Of Images = "+numberOfImages);
        System.out.println("Number Of Rows = "+numberOfRows);
        System.out.println("Number Of Columns = "+numberOfColumns);
    }

    /**
     * Prepares the color pallet used to paint the images.
     */
    private void initializePalet(){
        Color fullColor = full_color_picker.getValue();
        pallet = new Color[256];
        for (int i = 0; i<256; i++)
            pallet[i]= new Color(fullColor.getRed(), fullColor.getGreen(), fullColor.getBlue(), i/256d);
    }

    /**
     * The EMNIST dataset needs 2 transformations in order to be drawn as identifiable character :
     * 1 - A horizontal mirroring (or a vertical axial symmetry)
     * 2 - A 90° counter-clockwise rotation.
     * The transformation resulting is an axial symmetry along a descendant diagonal.
     * Modifies directly the content of the array passed as parameter.
     * @param imageBuffer Array of bytes of the image.
     * @
     */
    private void correctOrientation(byte[] imageBuffer){
        byte buffer;
        for (int y=0;y<numberOfRows;y++)
            for(int x = y+1; x<numberOfColumns; x++){
                buffer = imageBuffer[y*numberOfColumns+x];
                imageBuffer[y*numberOfColumns+x] = imageBuffer[x*numberOfColumns+y];
                imageBuffer[x*numberOfColumns+y] = buffer;
            }
    }
}
