package com.wholebrain.mnistreader;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;


public class Controller implements Initializable {
    @FXML public BorderPane main_layout;
    @FXML public Menu labelposition_menu, filters_menu, showonly_menu, sorters_menu, means_menu;
    @FXML public MenuItem open_menu, close_menu, showall_chars_menuitem,
            mean_set_menuitem,mean_char_menuitem,save_snapshot_menuitem,fast_snapshot_menuitem;
    @FXML public CheckMenuItem show_labels_checkbox, hint_show_menuitem, hint_coordinates_menuitem, hint_value_menuitem;
    @FXML public RadioMenuItem _TOPLEFT_POSITION_radiomenu, _TOPRIGHT_POSITION_radiomenu,
            _BOTTOMLEFT_POSITION_radiomenu, _BOTTOMRIGHT_POSITION_radiomenu, _TOP_POSITION_radiomenu,
            _BOTTOM_POSITION_radiomenu, _LEFT_POSITION_radiomenu, _RIGHT_POSITION_radiomenu;
    @FXML public Label index_label;
    @FXML public TextField jumpto_textfield;
    @FXML public ScrollBar index_scrollbar;
    @FXML public ColorPicker empty_color_picker, full_color_picker;
    @FXML public Slider empty_threshold_slider, full_threshold_slider;

    // Programmatically added GUI elements.
    private CustomCanvas canvas = new CustomCanvas();
    private List<CheckMenuItem> charFilters= new ArrayList<>();
    private List<MenuItem> showOnlyFilters = new ArrayList<>();
    private Stage primaryStage;

    // Dataset related
    private DatasetReader reader = new DatasetReader();
    private int currentImageIndex = 0;

    // Filters
    private Set<Character> filteredChars = new HashSet<>();
    private List<Integer> filteredImageIndexes = new ArrayList<>();

    // Sorter
    private Comparator<Integer> currentSorter;
    private SorterList sorters = new SorterList();

    // Image
    private File lastImageFolder;
    private int lastExtension;

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
        fileChooser.setInitialDirectory(reader.getCurrentFile() != null ?reader.getCurrentFile().getParentFile(): null);
        loadFile(fileChooser.showOpenDialog(primaryStage));
    }

    @FXML
    public void mnist_goto() {
        gotoHtmlLink("http://yann.lecun.com/exdb/mnist/");
    }

    @FXML
    public void emnist_goto() {
        gotoHtmlLink("https://www.nist.gov/node/1298471/emnist-dataset");
    }

    @FXML
    public void send_position_tocanvas(ActionEvent actionEvent) {
        String positionString = actionEvent.getSource().toString();
        positionString= positionString.substring(positionString.indexOf('=')+1,positionString.lastIndexOf("_radiomenu"));
        canvas.setLabelPosition(positionString);
    }

    @FXML
    public void on_showall_labels() {
        for(CheckMenuItem m : charFilters)
            m.setSelected(true);
        filteredChars.addAll(reader.getCharSet());
        updateCharFiltering();
    }

    @FXML
    public void on_mean_dataset() {
        launchMeanImage(reader.getAllImageBuffers(),"of whole dataset");
    }

    @FXML
    public void on_mean_set() {
        StringBuilder charsInSet = new StringBuilder(filteredChars.size()<=1?"":"s").append(" [");
        for (char c : filteredChars)
            charsInSet.append(c).append(",");
        charsInSet.replace(charsInSet.lastIndexOf(","),charsInSet.lastIndexOf(",")+1,"]");
        launchMeanImage(reader.getAllImageBuffersForChars(filteredChars), "for character"+charsInSet.toString());
    }

    @FXML
    public void on_mean_char() {
        ArrayList<Character> currentCharList = new ArrayList<>();
        char currentChar = reader.getCharForIndex(currentImageIndex);
        currentCharList.add(currentChar);
        launchMeanImage(reader.getAllImageBuffersForChars(currentCharList),"for character ["+currentChar+"]", currentChar);
    }

    @FXML
    public void on_save_snapshot(){
        FileChooser fileChooser = new FileChooser();
        String[] formats = ImageIO.getWriterFileSuffixes();
        String[] formatNames = new String[formats.length];
        ObservableList<FileChooser.ExtensionFilter> chooserFilter = fileChooser.getExtensionFilters();
        for (int i = 0; i< formats.length; i++){
            formatNames[i] = formats[i].toUpperCase()+" image file";
            formats[i] = "*."+formats[i];
            chooserFilter.add(new FileChooser.ExtensionFilter(formatNames[i],formats[i]));
        }
        fileChooser.setInitialDirectory(lastImageFolder == null ? reader.getCurrentFile().getParentFile():lastImageFolder);
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(lastExtension));
        String initialFileName =reader.getCurrentFile().getName().substring(0,reader.getCurrentFile().getName().lastIndexOf("idx")-1)
                .concat("#").concat(Integer.toString(currentImageIndex));
        if(reader.hasLabelsProperty().get())
            initialFileName = initialFileName.concat("[").concat(Character.toString(reader.getLabel(currentImageIndex))).concat("]");
        fileChooser.setInitialFileName(initialFileName);
        File file = fileChooser.showSaveDialog(primaryStage);
        if(file!=null){
            lastExtension=fileChooser.getExtensionFilters().indexOf(fileChooser.getSelectedExtensionFilter());
            lastImageFolder=file.getParentFile();
            String imageType = file.getName().substring(file.getName().lastIndexOf(".")+1);
            try {
                ImageIO.write(canvas.getSnapshot(imageType),imageType,file);
                toast(canvas, "Snapshot saved as "+file.getName(),3000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void on_fast_snapshot() {
        String[] formats = ImageIO.getWriterFileSuffixes();
        String fileName =reader.getCurrentFile().getName().substring(0,reader.getCurrentFile().getName().lastIndexOf("idx")-1)
                .concat("#").concat(Integer.toString(currentImageIndex));
        String imageType = formats[lastExtension];
        if(reader.hasLabelsProperty().get())
            fileName = fileName.concat("[").concat(Character.toString(reader.getLabel(currentImageIndex))).concat("]");
        File file = new File(lastImageFolder==null?reader.getCurrentFile().getParent():lastImageFolder.getPath(),
                fileName.concat(".").concat(imageType));
        int firstFreeIndex=1;
        while(file.exists()){
            file = new File(file.getParent(),fileName.concat("(")
                    .concat(Integer.toString(firstFreeIndex)).concat(").").concat(imageType));
            firstFreeIndex++;
        }
        try {
            ImageIO.write(canvas.getSnapshot(imageType),imageType,file);
            toast(canvas, "Snapshot saved as "+file.getName(),3000);
        } catch (IOException e) {
            e.printStackTrace();
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        index_scrollbar.setMax(0);
        index_scrollbar.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == oldValue.intValue()) return;
            updateIndex(newValue.intValue());
        });

        empty_color_picker.setValue(Color.WHITE);
        empty_color_picker.setOnAction(e->canvas.setBackGroundColor(empty_color_picker.getValue()));
        empty_color_picker.getCustomColors().add(Color.WHITE);

        full_color_picker.setValue(Color.BLACK);
        full_color_picker.setOnAction(e-> canvas.initializePallet(full_color_picker.getValue()));
        full_color_picker.getCustomColors().add(Color.BLACK);

        canvas.initializePallet(full_color_picker.getValue());

        jumpto_textfield.setTextFormatter(new TextFormatter<>(change -> {
            if(!change.getText().matches("\\d*"))
                return null;
            return change;
        }));
        jumpto_textfield.textProperty().addListener((observable, oldValue, newValue) -> {

            if (filteredImageIndexes.isEmpty())
                return;
            if (newValue == null || newValue.isEmpty())
                newValue = "0";

            int newInt, maxValue=Collections.max(filteredImageIndexes);
            try {
                newInt = Math.min(Integer.parseInt(newValue), maxValue);

            } catch (NumberFormatException e) {
                jumpto_textfield.setText(oldValue);
                newInt = maxValue;
                //throw new NumberFormatException("The input number \""+newValue+"\" is out of Integer range.");
            }
            if (!filteredImageIndexes.contains(newInt))
                newInt = findClosestInt(newInt,  filteredImageIndexes);

            updateIndex(filteredImageIndexes.indexOf(newInt));

        });

        ChangeListener<Boolean> sendVisibleProperty =((observable, oldValue, newValue) ->
                canvas.setLabelVisible(show_labels_checkbox.isSelected()&&!show_labels_checkbox.isDisable()));
        show_labels_checkbox.selectedProperty().addListener(sendVisibleProperty);
        show_labels_checkbox.disableProperty().addListener(sendVisibleProperty);

        initializeBindings();
        initializeSorters();
        initializeHints();

    }

    /**
     * Initialize all needed bindings between node properties.
     */
    private void initializeBindings(){
        labelposition_menu.disableProperty().bind(show_labels_checkbox.disableProperty()
                .or(show_labels_checkbox.selectedProperty().not()));

        filters_menu.disableProperty().bind(show_labels_checkbox.disableProperty());
        showonly_menu.disableProperty().bind(show_labels_checkbox.disableProperty());

        full_threshold_slider.minProperty().bind(empty_threshold_slider.valueProperty());
        empty_threshold_slider.maxProperty().bind(full_threshold_slider.valueProperty());
        empty_threshold_slider.valueProperty().addListener((observable, oldValue, newValue) -> canvas.setDownFilter(newValue.intValue()));
        full_threshold_slider.valueProperty().addListener((observable, oldValue, newValue) -> canvas.setUpFilter(newValue.intValue()));

        means_menu.disableProperty().bind(reader.hasOpenFile().not());
        fast_snapshot_menuitem.disableProperty().bind(reader.hasOpenFile().not());
        save_snapshot_menuitem.disableProperty().bind(reader.hasOpenFile().not());

        show_labels_checkbox.disableProperty().bind(reader.hasLabelsProperty().not());
        showall_chars_menuitem.disableProperty().bind(reader.hasLabelsProperty().not());
        filters_menu.disableProperty().bind(reader.hasLabelsProperty().not());
        showonly_menu.disableProperty().bind(reader.hasLabelsProperty().not());
        mean_char_menuitem.disableProperty().bind(reader.hasLabelsProperty().not());
        mean_set_menuitem.disableProperty().bind(reader.hasLabelsProperty().not());
    }

    /**
     * Initialize the available {@link Comparator<Integer>} which will sort the displayed characters.
     */
    private void initializeSorters() {
        Comparator<Integer> sorterDefault = Comparator.comparingInt(i->i);
        Comparator<Integer> sorterInvertedDefault = (i1,i2)->i2-i1;
        Comparator<Integer> sorter0z = Comparator.comparingInt(i -> reader.getCharForIndex(i));
        Comparator<Integer> sorterz0 = (i1, i2) -> reader.getCharForIndex(i2)-reader.getCharForIndex(i1);
        Comparator<Integer> sorterAZaz09 = Comparator.comparingInt(i -> (reader.getCharForIndex(i) - (byte)'A' + Byte.MAX_VALUE) % Byte.MAX_VALUE);
        Comparator<Integer> sorter90zaZA = (i1, i2) -> (reader.getCharForIndex(i2)-(byte)'A'+Byte.MAX_VALUE)%Byte.MAX_VALUE - (reader.getCharForIndex(i1)-(byte)'A'+Byte.MAX_VALUE)%Byte.MAX_VALUE;
        Comparator<Integer> sorterazAZ09az = Comparator.comparingInt(i ->
                -((reader.getCharForIndex(i) - (byte) 'A' + Byte.MAX_VALUE) % Byte.MAX_VALUE - 31 - Byte.MAX_VALUE) % Byte.MAX_VALUE);
        Comparator<Integer> sorterza90ZA = (i1, i2) ->
                - ((reader.getCharForIndex(i2)-(byte)'A'+Byte.MAX_VALUE)%Byte.MAX_VALUE-31-Byte.MAX_VALUE)% Byte.MAX_VALUE
                        +((reader.getCharForIndex(i1)-(byte)'A'+Byte.MAX_VALUE)%Byte.MAX_VALUE-31-Byte.MAX_VALUE)%Byte.MAX_VALUE;

        sorters.put("Default order", sorterDefault);
        sorters.put("Inverted order", sorterInvertedDefault);
        sorters.put("0 -> 9 -> A -> Z -> a -> z",sorter0z);
        sorters.put("0 <- 9 <- A <- Z <- a <- z", sorterz0);
        sorters.put("A -> Z -> a -> z -> 0 -> 9",sorterAZaz09);
        sorters.put("A <- Z <- a <- z <- 0 <- 9",sorter90zaZA);
        sorters.put("A -> Z -> 0 -> 9 -> a -> z",sorterazAZ09az);
        sorters.put("A <- Z <- 0 <- 9 <- a <- z",sorterza90ZA);

        EventHandler<ActionEvent> sortEvent = event -> {
            currentSorter = sorters.getComparator(((RadioMenuItem) event.getSource()).getText());
            sort();
            updateIndex(filteredImageIndexes.indexOf(currentImageIndex));
        };

        ToggleGroup sorterGroup = new ToggleGroup();
        for(int i = 0; i<sorters.size(); i++){
            RadioMenuItem newMenuItem = new RadioMenuItem();
            newMenuItem.setText(sorters.getString(i));
            sorterGroup.getToggles().add(newMenuItem);
            if (i==0)newMenuItem.setSelected(true);
            else if (i>1)
                newMenuItem.setDisable(true);
            newMenuItem.setOnAction(sortEvent);
            sorters_menu.getItems().add(newMenuItem);
            if(i>1)
                newMenuItem.disableProperty().bind(reader.hasLabelsProperty().not());
        }
        sorters_menu.getItems().add(2,new SeparatorMenuItem());
        currentSorter=sorters.getComparator(0);

    }

    /**
     * Initializes the behaviour of the {@link javafx.scene.control.Tooltip].
     */
    private void initializeHints() {
        Platform.runLater(()->canvas.mouseTransparentProperty().bind(primaryStage.focusedProperty().not()));
        EventHandler<ActionEvent> handler = event -> {
            boolean isCoordShown = hint_coordinates_menuitem.isSelected(),
                    isValueShown = hint_value_menuitem.isSelected(),
                    isHintShown = hint_show_menuitem.isSelected()&&(isCoordShown || isValueShown);

            canvas.sendHintSetup(isHintShown,isCoordShown,isValueShown);
        };

        hint_show_menuitem.setOnAction(handler);
        hint_coordinates_menuitem.setOnAction(handler);
        hint_value_menuitem.setOnAction(handler);
    }

    /**
     * Stores a link to the {@link Stage primary stage} to be able to close it.
     * @param primaryStage Main {@link Stage window}.
     */
    void setStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        main_layout.setCenter(canvas);
    }

    /**
     * Send the file to the DatasetReader, then change every related variables and properties.
     * @param file to read.
     */
    private void loadFile(File file) {
        if(file == null) return;
        reInitializeMenus();
        reader.setCurrentFile(file);
        primaryStage.setTitle("Datasets Images Reader : " + file.getName());


        if (!reader.hasLabelsProperty().get()) {
            for (int i = 0; i<reader.getNumberOfImages();i++)
                filteredImageIndexes.add(i);
            setupScrollBar();
            updateIndex(0);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Datasets Images Reader");
            alert.setHeaderText("The labels file \""+DatasetReader.getLabelsFileName(file)+"\" could not be found.");
            alert.setContentText("The labels won't be shown. To show the labels, please put the labels file next to its associated images file.");
            alert.showAndWait();
        }else {
            filteredChars.addAll(reader.getCharSet());
            loadFilters();
            updateCharFiltering();
            setupScrollBar();
        }

    }

    /**
     * Sets the menus to the initial settings.
     */
    private void reInitializeMenus(){
        currentImageIndex=0;
        filteredChars.clear();
        filteredImageIndexes.clear();
        canvas.setLabelVisible(false);
    }

    /**
     * Change basic size properties of the {@link java.awt.Scrollbar scroll bar}
     * to map the count of displayed images.
     */
    private void setupScrollBar(){
        index_scrollbar.setMin(0);
        index_scrollbar.setMax(filteredImageIndexes.size()-1);
        index_scrollbar.setBlockIncrement(filteredImageIndexes.size()/20);
    }

    /**
     * Update the displayed infos about the current image index.
     */
    private void updateIndex(int newFilteredIndex) {
        if (!reader.hasOpenFile().get()) return;
        if (filteredImageIndexes.size() == 0) {
            canvas.loadImage(getNullImage(), 112, 112, '?');
            index_label.setText("No image.");
            jumpto_textfield.setText(null);
        } else {
            currentImageIndex = filteredImageIndexes.get(newFilteredIndex);
            index_label.setText(String.valueOf(currentImageIndex));
            index_scrollbar.setValue(newFilteredIndex);
            paint();
        }
    }

    /**
     * Displays a colored representation of the current image on the {@link CustomCanvas canvas}.
     */
    private void paint(){
        byte[] imageBuffer = reader.getImageBuffer(currentImageIndex);


        if (reader.isNeedsTransformation()) correctOrientation(reader, imageBuffer);

        canvas.loadImage(imageBuffer, reader.getRowCount(),
                reader.getColumnCount(),
                reader.getLabel(currentImageIndex));
    }

    /**
     * Adds a {@link CheckMenuItem} for every char present in dataset.
     */
    private void loadFilters(){
        filters_menu.getItems().clear();
        showonly_menu.getItems().clear();

        showOnlyFilters.clear();
        charFilters.clear();
        if(reader.getNumberOfImages()== 0)
            return;

        EventHandler<ActionEvent> filterEvent = event -> {
            char c = ((CheckMenuItem)event.getSource()).getText().charAt(0);
            if(((CheckMenuItem)event.getSource()).isSelected()) {
                addCharToFilter(c);
            }
            else
                removeCharToFilter(c);
        };
        EventHandler<ActionEvent> showOnlyEvent = event -> {
            char c = ((MenuItem)event.getSource()).getText().charAt(0);
            filteredChars.clear();
            for(CheckMenuItem filter : charFilters)
                filter.setSelected(false);
            //noinspection RedundantCast
            charFilters.get(showOnlyFilters.indexOf((MenuItem)event.getSource())).setSelected(true);
            addCharToFilter(c);
        };

        for (char c : reader.getCharSet()) {
            filteredChars.add(c);

            CheckMenuItem newCharFilter = new CheckMenuItem(String.valueOf(c));
            newCharFilter.setSelected(true);
            newCharFilter.setOnAction(filterEvent);
            charFilters.add(newCharFilter);

            MenuItem newShowOnlyItem = new MenuItem(String.valueOf(c));
            newShowOnlyItem.setOnAction(showOnlyEvent);
            showOnlyFilters.add(newShowOnlyItem);
        }
        charFilters.sort(Comparator.comparingInt(o -> o.getText().charAt(0)));
        showOnlyFilters.sort(Comparator.comparingInt(o -> o.getText().charAt(0)));

        filters_menu.getItems().addAll(charFilters);
        showonly_menu.getItems().addAll(showOnlyFilters);
    }

    /**
     * Adds a character to the filtering process.
     * @param c char to add.
     */
    private void addCharToFilter(char c){
        if(!filteredChars.contains(c)){
            filteredChars.add(c);
            updateCharFiltering();
        }
    }

    /**
     * Removes a character to the filtering process.
     * @param c char to add.
     */
    private void removeCharToFilter(char c) {
        if(filteredChars.contains(c)){
            filteredChars.remove(c);
            updateCharFiltering();
        }
    }

    /**
     * Updates the variable filteredImageIndexes to correspond
     * to the indexes of the filtered characters.
     */
    private void updateCharFiltering(){
        filteredImageIndexes.clear();
        for(char c : filteredChars)
            filteredImageIndexes.addAll(reader.getIndicesForChar(c));
        sort();
        setupScrollBar();
        if(filteredImageIndexes.size()==0)
            updateIndex(0);
        else updateIndex(filteredImageIndexes.contains(currentImageIndex) ?
                filteredImageIndexes.indexOf(currentImageIndex):
                filteredImageIndexes.indexOf(findClosestInt(currentImageIndex,filteredImageIndexes)));
    }

    /**
     * Sort displayed characters in the chosen order.
     */
    private void sort(){
        if(filteredImageIndexes.size()==0) return;
        filteredImageIndexes.sort(currentSorter);
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
    private static void correctOrientation(DatasetReader reader, byte[] imageBuffer){
        byte buffer;
        int rowCount = reader.getRowCount(),
                columnCount = reader.getColumnCount();
        for (int y=0;y<columnCount;y++)
            for(int x = y+1; x<rowCount; x++){
                buffer = imageBuffer[y*columnCount+x];
                imageBuffer[y*columnCount+x] = imageBuffer[x*columnCount+y];
                imageBuffer[x*columnCount+y] = buffer;
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
     * Simply a byte by byte generated image for the fun !
     * @return imageBuffer readable by the {@link CustomCanvas custom canvas}.
     */
    private byte[] getNullImage(){
        byte[] imgData = new byte[12544];
        pxRect(imgData, 5,62,20,96);
        pxRect(imgData, 22,62,37,96);
        pxSimpleTriangle(imgData, 5,62,36,true);
        pxRect(imgData, 39,28,54,62);
        pxRect(imgData, 56,28,71,62);
        pxSimpleTriangle(imgData,39,62,70,false);
        pxRect(imgData, 73,10,88,96);
        pxRect(imgData, 90,10,105,96);
        return imgData;
    }

    /**
     * Creates and shows a window that show the pixel-wise mean image of a set of images.
     * @param image Images to mean, as an array of image buffers (which are arrays of byte).
     * @param title Title to show on the window. The title will automatically add " in {imageFileName}".
     */
    private void launchMeanImage(byte[][] image, String title) {
        launchMeanImage(image, title, '?');
    }

    /**
     * Creates and shows a window that shows the pixel-wise mean image of a set of images.
     * @param image Images to mean, as an array of image buffers (which are arrays of byte).
     * @param title Title to show on the window. The title will automatically add " in {imageFileName}".
     * @param currentChar Character of the label to be shown.
     */
    private void launchMeanImage(byte[][] image, String title, char currentChar){
        CustomCanvas meanCanvas = new CustomCanvas();
        meanCanvas.setLabelVisible(false);
        meanCanvas.initializePallet(full_color_picker.getValue());
        meanCanvas.setBackGroundColor(empty_color_picker.getValue());
        meanCanvas.sendHintSetup(true, true, true);

        if(reader.isNeedsTransformation()) {
            //loading bar dialog for transformations
            new ProgressDialog("Transforming image ", image.length,
                    new Task() {
                        @Override
                        protected Void call() {
                            for (int i = 0; i< image.length; i++) {
                                correctOrientation(reader,image[i]);
                                updateProgress(i,image.length);
                            }
                            return null;
                        }
                    });
        }

        byte[] meanImageBuffer = new byte[reader.getPixelCount()];
        new ProgressDialog("Calculating mean for image ", image.length,
                new Task() {
                    @Override
                    protected Void call(){
                        long[] meanImage = new long[reader.getPixelCount()];
                        for (int i =0;i<image.length;i++) {
                            for (int j = 0; j < image[i].length; j++)
                                meanImage[j] += ((long) image[i][j] & 0xff);
                            updateProgress(i,image.length-1);
                        }
                        for(int i = 0; i<meanImage.length; i++) {
                            meanImageBuffer[i] = (byte)(Math.round(meanImage[i]/(image.length*1.0)));
                        }
                        return null;
                    }
                });

        meanCanvas.loadImage(meanImageBuffer, reader.getRowCount(), reader.getColumnCount(),currentChar);

        BorderPane borderPane = new BorderPane(meanCanvas);
        Scene meanScene = new Scene(borderPane);
        Stage meanStage = new Stage();
        meanCanvas.mouseTransparentProperty().bind(meanStage.focusedProperty().not());
        meanStage.setScene(meanScene);
        meanStage.setTitle("Mean image "+title+" in "+reader.getCurrentFile().getName());
        meanCanvas.setPrefSize(280,280);
        meanStage.setX(Math.min(primaryStage.getX()+primaryStage.getWidth(), Toolkit.getDefaultToolkit().getScreenSize().width-280));
        meanStage.setY(primaryStage.getY());
        meanStage.show();
    }

    // Utils methods

    /**
     * Finds the closest int in a {@link List<Integer> list of integer}
     * from a int.
     * Example : List = {0 ; 10 ; 20 ; 30 ; 40}. findClosestInt(21,List) will return 30.
     * @param value int to consider
     * @param sortedList {@link }List<Integer>} sorted with int values from smallest to biggest.
     * @return the closest int in the lis.
     */
    private static int findClosestInt(final int value, final List<Integer> sortedList) {
        int lo = 0, hi = sortedList.size() - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (sortedList.get(mid) < value) lo = mid + 1;
            else if (sortedList.get(mid) > value) hi = mid;
            else return mid;
        }
        return lo > hi || sortedList.get(lo) < value ? sortedList.get(lo-1) : sortedList.get(lo);
    }

    /**
     * Colors a rectangle in the imageBuffer.
     * @param imageBuffer as an array of byte.
     * @param startX X coordinate of the top left corner of the rectangle.
     * @param startY Y coordinate of the top left corner of the rectangle.
     * @param endX X coordinate of the bottom right corner of the rectangle.
     * @param endY Y coordinate of the bottom right corner of the rectangle.
     */
    private static void pxRect(byte[] imageBuffer, int startX, int startY, int endX, int endY){
        for (int i = startY; i<endY; i++){
            for (int j = startX; j<endX; j++){
                imageBuffer[i*112+j]=(byte)255;
            }
        }
    }

    /**
     * Colors a horizontal based equilateral triangle int the imageBuffer.
     * @param imageBuffer as an array of byte.
     * @param startX X coordinate of the left corner of the triangle.
     * @param startY Y coordinate of the left corner of the triangle.
     * @param endX X coordinate of the right corner of the triangle.
     * @param up Direction of the third corner of the triangle : true = up ; false = down.
     */
    private static void pxSimpleTriangle(byte[] imageBuffer, int startX, @SuppressWarnings("SameParameterValue") int startY, int endX, boolean up){
        int height = (endX-startX)/2;
        int i = 0;
        while(i<=height){
            for (int j =0; j<=i; j++) {
                imageBuffer[112 * startY + (up?-j:j) * 112 + startX + i] = (byte) 255;
                imageBuffer[112 * startY + (up?-j:j) * 112 + endX-i]=(byte)255;
            }
            i++;
        }

    }

    /**
     * Shows a {@link Popup} at the bottom center of the specified {@link Region region}, inside a simple {@link Label}.
     * @param region Region to put the Popup over.
     * @param message Text to show.
     * @param msPeriod Time in milliseconds to show the {@link Popup}.
     */
    private static void toast(Region region, String message, @SuppressWarnings("SameParameterValue") int msPeriod){
        final Popup popup = new Popup();
        Label popupText = new Label(message);
        popupText.setStyle("-fx-font-weight: bold");
        popup.getContent().add(popupText);
        Window window = region.getScene().getWindow();
        popup.setOnShown(e -> {
            popup.setX(window.getX() + window.getWidth() / 2 - popup.getWidth() / 2);
            popup.setY(window.getY() + region.getScene().getY()+ region.getLayoutY() + region.getHeight() - popup.getHeight()-10);
            new Thread(()->{
                try {
                    Thread.sleep(msPeriod);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                Platform.runLater(popup::hide);
            }).start();
        });
        popup.show(window);
    }
}
