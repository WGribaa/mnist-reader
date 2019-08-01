package com.wholebrain.mnistreader;

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


public class Controller implements Initializable {
    public MenuItem open_menu, close_menu;
    public Canvas canvas;
    public Label index_label;
    public ScrollBar index_scrollbar;
    public ColorPicker empty_color_picker, full_color_picker;

    private Stage primaryStage;
    private File currentFile;
    private int magicNumber, numberOfImages, numberOfRows, numberOfColumns, currentImageIndex = 0;
    private Color[] pallet = new Color[256];

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
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("DataSets files (*.idx3-ubyte)","*.idx3-ubyte"));
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
        index_scrollbar.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentImageIndex=newValue.intValue();
            updateIndex();
            paint();
        });

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
    private void updateIndex(){
        index_label.setText(String.valueOf(currentImageIndex));
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
        currentImageIndex = 0;
        index_scrollbar.setMax(numberOfImages-1);
        index_scrollbar.setBlockIncrement(numberOfImages/10);
        paint();
    }

    /**
     * Displays a colored representation of the current image on the {@link Canvas canvas}.
     */
    private void paint(){
        BufferedInputStream bis = jumpToIndex(currentImageIndex);
        if (bis == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double resolution = Double.min(canvas.getHeight()/numberOfRows, canvas.getWidth()/numberOfColumns);
        gc.setFill(empty_color_picker.getValue());
        gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        try{
            for(int y = 0; y<numberOfRows; y++)
                for (int x = 0; x<numberOfColumns; x++){
                    gc.setFill(pallet[bis.read()]);
                    gc.fillRect(x*resolution, y*resolution,resolution, resolution);
                }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try{
                bis.close();
            }catch (IOException e){
                e.printStackTrace();
            }
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

}
