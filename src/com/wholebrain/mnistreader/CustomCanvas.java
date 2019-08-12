package com.wholebrain.mnistreader;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.awt.image.BufferedImage;

@SuppressWarnings("WeakerAccess")
public class CustomCanvas extends Pane {
    private Color[] pallet =new Color[256];
    private Color backGroundColor = Color.WHITE;
    private char currentChar;
    private byte[] image;
    private int numberOfRows, numberOfColumns,
            filterDownThreshold=0, filterUpThreshold=255,
            xMouse, yMouse;
    private double resolution, xPos, yPos;
    private boolean isLabelVisible = true, isFiltered = false,
            showHint=true, showHintCoord=true, showHintValue=true;
    private Font labelFont = new Font(Font.getDefault().getName(),50);
    private Canvas canvas = new Canvas(280,280);
    private POSITION currentLabelPosition = POSITION._TOPLEFT_POSITION;

    /**
     * This enum has the purpose to make the position_radiobuttons easy to tell the canvas
     * which positioning the user wants.
     */
    private enum POSITION {
        _TOPLEFT_POSITION(0,0),
        _TOPRIGHT_POSITION(1,0),
        _BOTTOMLEFT_POSITION(0,1),
        _BOTTOMRIGHT_POSITION(1,1),
        _TOP_POSITION(0.5,0),
        _BOTTOM_POSITION(0.5,1),
        _LEFT_POSITION(0,0.5),
        _RIGHT_POSITION(1,0.5);

        private double vPosition,hPosition;

        POSITION(double hPosition, double vPosition){
            this.vPosition=vPosition;
            this.hPosition=hPosition;
        }
        double getVPosition(){
            return vPosition;
        }

        double getHPosition(){
            return hPosition;
        }
    }

    public CustomCanvas(){
        getChildren().add(canvas);
        setMinSize(280,280);
        canvas.getGraphicsContext2D().setFont(labelFont);
        canvas.getGraphicsContext2D().setTextBaseline(VPos.CENTER);
        canvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);

        // Hint initialization
        Tooltip pxHint = new Tooltip();
        pxHint.setShowDelay(Duration.ZERO);
        pxHint.setHideDelay(Duration.ZERO);

        canvas.setOnMouseMoved(event -> {
            if(!showHint) Tooltip.uninstall(canvas,pxHint);
            else {
                int currentXMouse = (int) Math.floor((event.getX() - xPos) / resolution);
                int currentYMouse = (int) Math.floor((event.getY() - yPos) / resolution);
                if (currentXMouse < 0 || currentXMouse >= numberOfColumns
                        || currentYMouse < 0 || currentYMouse >= numberOfRows) {
                    Tooltip.uninstall(canvas, pxHint);
                } else if (currentXMouse != xMouse || currentYMouse != yMouse) {
                    pxHint.show(canvas, event.getScreenX() + 10, event.getScreenY() + 10);
                    xMouse = currentXMouse;
                    yMouse = currentYMouse;
                    pxHint.setText(getHintText());
                    if (!pxHint.isActivated())
                        Tooltip.install(canvas, pxHint);
                }
            }

        });
    }

    @Override
    protected void layoutChildren(){
        int w = (int)getWidth();
        int h = (int)getHeight();
        if(w!=canvas.getWidth() || h!=canvas.getHeight()){
            canvas.setWidth(w);
            canvas.setHeight(h);
            repaint();
        }
    }

    public BufferedImage getSnapshot(String imageType){
        WritableImage image = new WritableImage((int)(canvas.getWidth()),(int)(canvas.getHeight()));
        canvas.snapshot(null, image);
        BufferedImage bufferedImage=SwingFXUtils.fromFXImage(image,null);
        switch(imageType){
            case "jpg":
            case "bmp":
            case "wbmp":
                BufferedImage correctedBufferedImage = new BufferedImage((int)canvas.getWidth(), (int)canvas.getHeight(),
                        imageType.equals("wbmp")?BufferedImage.TYPE_BYTE_BINARY:BufferedImage.TYPE_INT_RGB);
                correctedBufferedImage.createGraphics().drawImage(bufferedImage,0,0,java.awt.Color.WHITE,null);
                System.out.println("Image corrected to "+imageType);
                return correctedBufferedImage;
        }
        return bufferedImage;
    }

    /**
     * Loads the image inside the buffer and stores its metadatas.
     * @param imageBuffer Image buffer with each pixel opacity in byte.
     * @param numberOfRows Number of rows of the image.
     * @param numberOfColumns Number of columns of the image.
     * @param currentChar Character represented by the image.
     */
    public void loadImage(byte[] imageBuffer, int numberOfRows, int numberOfColumns,char currentChar){
        this.image = imageBuffer;
        this.numberOfRows=numberOfRows;
        this.numberOfColumns=numberOfColumns;
        this.currentChar=currentChar;
        repaint();
    }

    /**
     * Prepares the color pallet used to paint the images.
     */
    public void initializePallet(Color color){
        pallet = new Color[256];
        if(!isFiltered)
            for (int i = 0; i<256; i++)
                pallet[i]= new Color(color.getRed(), color.getGreen(), color.getBlue(), i/256d);
        else {
            for (int i = 0; i<= filterDownThreshold; i++)
                pallet[i]=backGroundColor;
            for (int i = filterDownThreshold +1; i< filterUpThreshold; i++)
                pallet[i]= new Color(color.getRed(), color.getGreen(), color.getBlue(), i/256d);
            for (int i = filterUpThreshold; i<256; i++)
                pallet[i] = color;
        }

        repaint();
    }

    /**
     * Set the background color of the canvas.
     * @param backGroundColor {@link Color} of background.
     */
    public void setBackGroundColor(Color backGroundColor) {
        this.backGroundColor = backGroundColor;
        initializePallet(pallet[255]);
        repaint();
    }

    /**
     * Sets the boolean flag that informs the canvas if we cant to draw the label.
     * @param selected boolean true if the label is wanted to be drawn.
     */
    public void setLabelVisible(boolean selected) {
        isLabelVisible=selected;
        repaint();
    }

    /**
     * Stores when the label has to be drawn.
     * @param positionString Is actually a {@link POSITION} called by a String thanks to the enum possibilities.
     */
    public void setLabelPosition(String positionString){
        currentLabelPosition=POSITION.valueOf(positionString);
        repaint();
    }

    /**
     * Sets the down threshold for filtering the image :
     * every value equal or below will show a "blank" pixel.
     * @param downValue Limit value to be a blank pixel.
     */
    public void setDownFilter(int downValue) {
        this.filterDownThreshold = downValue;
        updateFiltering();
    }

    /**
     * Sets the up threshold for filtering the image :
     * every value equal or above will show a "full" pixel.
     * @param upValue Limit value to be a fully opaque pixel.
     */
    public void setUpFilter(int upValue) {
        this.filterUpThreshold = upValue;
        updateFiltering();
    }

    /**
     * Makes the {@link Canvas canvas} repaint its content.
     */
    private void repaint(){
        paint(canvas.getGraphicsContext2D());
    }

    /**
     * Asks the {@link Canvas canvas} draw the current image onto a specified {@link GraphicsContext graphics context}.
     * @param graphicsContext to draw into.
     */
    private void paint(GraphicsContext graphicsContext){
        double size = Double.min(canvas.getHeight(), canvas.getWidth());
        resolution = Math.floor(Double.min(size/(1.0*numberOfRows), size/(1.0*numberOfColumns)));
        xPos = Math.floor((canvas.getWidth()-resolution*numberOfColumns)/2.0);
        yPos = Math.floor((canvas.getHeight()-resolution*numberOfRows)/2.0);

        graphicsContext.setFill(backGroundColor);
        graphicsContext.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        for(int y = 0; y<numberOfRows; y++)
            for (int x = 0; x<numberOfColumns; x++){
                graphicsContext.setFill(pallet[image[y*numberOfRows+x]&0xFF]);
                graphicsContext.fillRect(xPos+x*resolution, yPos+y*resolution,resolution,resolution);
            }
        if(isLabelVisible) printLabel(graphicsContext);
    }

    /**
     * Make the canvas draw the label.
     * @param gc The canvas' {@link GraphicsContext}.
     */
    private void printLabel(GraphicsContext gc) {
        gc.setFill(pallet[255]);
        String toPrint = String.valueOf(currentChar);
        double x = (1+4*currentLabelPosition.getHPosition())/6d*canvas.getWidth();
        double y = (1+4*currentLabelPosition.getVPosition())/6d*canvas.getHeight();
        gc.fillText(toPrint,x,y);
    }

    /**
     * Checks if the image needs filtering and reinitialize the pallet in consequence.
     */
    private void updateFiltering(){
        isFiltered = !(filterDownThreshold ==0 && filterUpThreshold ==255);
        initializePallet(pallet[255]);
    }

    /**
     * Setups the behaviour of the {@link Tooltip}.
     * @param showHint Tells if the {@link Tooltip} is enabled.
     * @param showHintCoord Tells if the {@link Tooltip} shows the cursor coordinate on the {@link Canvas canvas}.
     * @param showHintValue Tells if the {@link Tooltip} shows the value of the pixel pointed by the cursor.
     */
    public void sendHintSetup(boolean showHint, boolean showHintCoord, boolean showHintValue) {
        this.showHint=showHint;
        this.showHintCoord=showHintCoord;
        this.showHintValue=showHintValue;
    }

    /**
     * Returns the text that is to show on the {@link Tooltip}.
     * @return {@link String Text}.
     */
    private String getHintText() {
        return (image==null)?
                "No image loaded":
                (showHintCoord? "("+xMouse+";"+yMouse+")":"")+
                        (showHintCoord&&showHintValue?"=":"")+
                        (showHintValue?(image[(yMouse*numberOfColumns+xMouse)]&0xff):"");
    }
}
