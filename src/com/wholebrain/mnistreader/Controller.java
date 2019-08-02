package com.wholebrain.mnistreader;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    public MenuItem open_menu, close_menu;
    public Canvas canvas;
    public Label index_label, char_label;
    public ScrollBar index_scrollbar;
    public ColorPicker empty_color_picker, full_color_picker;
    public TextField jumpto_textfield;
    public CheckMenuItem labels_checkbox;
    public Menu labelposition_menu;
    public RadioMenuItem topleft_position_radiomenu,topright_position_radiomenu,
            bottomleft_position_radiomenu,bottomright_position_radiomenu,top_position_radiomenu,
            bottom_position_radiomenu,left_position_radiomenu,right_position_radiomenu;

    private Stage primaryStage;
    private File currentFile;
    private int magicNumber, numberOfImages, numberOfRows, numberOfColumns, currentImageIndex = 0;
    private Color[] pallet = new Color[256];
    private boolean needsTransformation = false;
    private char[] labelsChars;
    private Map<Integer, Character> mapping;

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

    @FXML
    public void mnist_goto() {
        gotoHtmlLink("http://yann.lecun.com/exdb/mnist/");
    }

    @FXML
    public void emnist_goto() {
        gotoHtmlLink("https://www.nist.gov/node/1298471/emnist-dataset");
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

        char_label.visibleProperty().bind(labels_checkbox.selectedProperty());

        initializeMenus();

    }

    private void initializeMenus(){
        labelposition_menu.disableProperty().bind(labels_checkbox.disableProperty());

        topleft_position_radiomenu.setOnAction(event -> {
            char_label.setLayoutX(0);
            char_label.setLayoutY(0);
        });
        topright_position_radiomenu.setOnAction(event -> {
            char_label.setLayoutX(canvas.getWidth()-char_label.getWidth());
            char_label.setLayoutY(0);
        });
        bottomleft_position_radiomenu.setOnAction(event -> {
            char_label.setLayoutX(0);
            char_label.setLayoutY(canvas.getHeight()-char_label.getHeight());
        });
        bottomright_position_radiomenu.setOnAction(event -> {
            char_label.setLayoutX(canvas.getWidth()-char_label.getWidth());
            char_label.setLayoutY(canvas.getHeight()-char_label.getHeight());
        });
        top_position_radiomenu.setOnAction(event -> {
            char_label.setLayoutX((canvas.getWidth()-char_label.getWidth())/2);
            char_label.setLayoutY(0);
        });
        bottom_position_radiomenu.setOnAction(event -> {
            char_label.setLayoutX((canvas.getWidth()-char_label.getWidth())/2);
            char_label.setLayoutY(canvas.getHeight()-char_label.getHeight());
        });
        left_position_radiomenu.setOnAction(event -> {
            char_label.setLayoutX(0);
            char_label.setLayoutY((canvas.getHeight()-char_label.getHeight())/2);
        });
        right_position_radiomenu.setOnAction(event -> {
            char_label.setLayoutX(canvas.getWidth()-char_label.getWidth());
            char_label.setLayoutY((canvas.getHeight()-char_label.getHeight())/2);
        });

    }

    /**
     * Stores a link to the {@link Stage primary stage} to be able to close it.
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
        needsTransformation = currentFile.getName().contains("emnist");
        paint();
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
        loadLabels();
        index_scrollbar.setMax(numberOfImages-1);
        index_scrollbar.setBlockIncrement(numberOfImages/20);
        updateIndex(0);
    }

    /**
     * Reads the labels file associated with the current image file and save it in a byte array.
     * If the number of images is different from the number of labels, the byte array will remain null.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void loadLabels() {
        labelsChars = null;

        StringBuilder labelSb = new StringBuilder(currentFile.getPath());
        labelSb.replace(labelSb.lastIndexOf("idx3"),labelSb.lastIndexOf("idx3")+4,"idx1")
                .replace(labelSb.lastIndexOf("images"), labelSb.lastIndexOf("images")+6, "labels");
        File labelFile = new File(labelSb.toString());
        if (!labelFile.exists())
            return;

        BufferedInputStream bis = null;
        byte[] labelsBytes = null;
        try{
            bis = new BufferedInputStream((new FileInputStream(labelFile)));
            bis.skip(4);
            byte[] buffer = new byte[4];
            bis.read(buffer);
            if(bytesToInt(buffer) != numberOfImages)
                return;
            labelsBytes = bis.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (bis != null)
                    bis.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (labelsBytes==null || labelsBytes.length != numberOfImages)
            labelsChars = null;
        else {
            loadMapping();
            labelsChars = new char[labelsBytes.length];
            for(int i =0; i< labelsBytes.length; i++)
                labelsChars[i]= mapping.get((int) labelsBytes[i]);

            labels_checkbox.setDisable(false);
            labels_checkbox.setSelected(true);
        }
    }

    private void loadMapping(){
        mapping = new HashMap<>();
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
                mapping.put(Integer.valueOf(splittedLine[0]),(char)Integer.parseInt(splittedLine[1]));
                line=br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Mapping file \""+mappingFile.getPath()+"\" has been loaded.");
        System.out.println(mapping);

    }

    private void defaultMapping(){
        for (int i = 0; i<10;i++){
            mapping.put(i,(char)(i+48));
        }
        System.out.println("Default mapping has been loaded.");
        System.out.println(mapping);
    }


    /**
     * Shows the label associated with the current image.
     */
    private void updateLabel(){
        if(labelsChars ==null) {
            char_label.setText(null);
        } else{
//            System.out.println(labelsChars[currentImageIndex]);
            char_label.setText(String.valueOf(labelsChars[currentImageIndex]));
        }
    }

    /**
     * Displays a colored representation of the current image on the {@link Canvas canvas}.
     */
    private void paint(){
        BufferedInputStream bis = jumpToIndex(currentImageIndex);
        if (bis == null) return;

        byte[] imageBuffer = new byte[numberOfRows*numberOfColumns];
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
        updateLabel();
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
            //noinspection ResultOfMethodCallIgnored
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

    /**
     * Open the user's main web browsing application (if available) and browses to the link.
     * @param link Link to open in the Web Browser.
     */
    private void gotoHtmlLink(String link){
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(link));
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Shows some infos about us aka I, me and myself at the moment.
     * @throws IOException Common Exception occurring during the FXML loading step.
     */
    @FXML
    public void about_us() throws IOException {
        Stage infoStage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("infopanel/infopanel.fxml"));
        Parent root = loader.load();
        infoStage.opacityProperty().set(0.9);
        infoStage.setTitle("About us...");
        infoStage.setScene(new Scene(root));
        infoStage.initModality(Modality.APPLICATION_MODAL);
        infoStage.setResizable(false);
        infoStage.centerOnScreen();
        infoStage.show();

    }
}
