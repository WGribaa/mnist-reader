package com.wholebrain.mnistreader.canvas;

import javafx.animation.AnimationTimer;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("WeakerAccess")
public abstract class CustomCanvas extends Pane {
    private int filterDownThreshold=0, filterUpThreshold = 255, resolution = 3;
    private boolean isFiltered = false;
    private ImageBufferProvider provider;
    private static int MAX_RESOLUTION=50, MIN_RESOLUTION = 2;
    private static long refreshTimeNs =
            1000000000L/GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getRefreshRate();


    protected Canvas canvas = new Canvas(280,280);
    protected POSITION currentLabelPosition = POSITION._TOPLEFT_POSITION;
    protected Color backGroundColor = Color.WHITE;
    protected Color[] pallet = new Color[256];
    protected Tooltip pxHint= new Tooltip();
    protected boolean isLabelVisible = true, showHint = true, showHintCoord = true, showHintValue = true, showHintIndex=false;
    protected int imageVDefinition =1, imageHDefinition =1, xMouse, yMouse, indexBelowMouse=0;
    private byte[][] imageBuffers;
    protected String[] currentChars;
    protected int[] currentIndices;

    private final CanvasRedrawTask<CanvasData> redrawTask = new CanvasRedrawTask<>(canvas) {
        @Override
        protected void redraw(GraphicsContext context, CanvasData data) {
            context.setFill(backGroundColor);
            context.fillRect(0,0,canvas.getWidth(), canvas.getHeight());
            for (int i = 0; i< data.getPosX().length() && i<imageBuffers.length; i++){
                for(int y = 0; y< imageVDefinition; y++)
                    for (int x = 0; x< imageHDefinition; x++){
                        context.setFill(pallet[imageBuffers[i][y* imageVDefinition +x]&0xFF]);
                        context.fillRect(data.getPosX().get(i)+x*resolution, data.getPosY().get(i)+y*resolution,resolution,resolution);
                    }
                if(isLabelVisible){
                    context.setFill(pallet[255]);
                    String toPrint = String.valueOf(currentChars.length>0?currentChars[i]:' ');
                    context.fillText(toPrint,
                            data.getPosX().get(i)+(1+4*currentLabelPosition.getHPosition())/6d*imageHDefinition*resolution,
                            data.getPosY().get(i)+(1+4*currentLabelPosition.getVPosition())/6d*imageVDefinition*resolution);
                }
            }
        }
    };

    public void setImageProvider(ImageBufferProvider listener) {
        this.provider = listener;
    }


    /**
     * An enum to tell the scene where to put the {@link ScrollBar}.
     */
    public enum DIRECTION{
        _BOTTOM,
        _RIGHT,
        _LEFT
    }
    /**
     * This enum has the purpose to make the position_radiobuttons easy to tell the canvas
     * which positioning the user wants.
     */
    @SuppressWarnings("unused")
    protected enum POSITION {
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

    protected abstract CanvasData getCanvasData();
    protected abstract EventHandler<MouseEvent> getHintEvent();
    protected abstract void notify(ImageBufferProvider listener);
    public abstract DIRECTION getScrollBarPosition();
    public abstract int getShownImageCount();
    public abstract int getIndexFor(int position);
    public abstract int getScrollValueForIndex(int index);
    public abstract int getScrollBarMaxValueFor(int elementCount);
    public abstract double getScrollBarUnitIncrement();

    public CustomCanvas(){
        getChildren().add(canvas);
        setMinSize(280,280);
        canvas.getGraphicsContext2D().setTextBaseline(VPos.CENTER);
        canvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        pxHint.setHideDelay(Duration.ZERO);
        pxHint.setShowDelay(Duration.ZERO);
        pxHint.setShowDuration(Duration.INDEFINITE);
        canvas.setOnMouseMoved(getHintEvent());
    }

    public final void layoutChildren(){
        int w = (int)getWidth();
        int h = (int)getHeight();
        if(w!=canvas.getWidth() || h!=canvas.getHeight()){
            canvas.setWidth(w);
            canvas.setHeight(h);
            refresh();
            notify(provider);
        }
    }

    /**
     * Returns a snapshot of the currently draw content of the canvas.
     * @param imageType File type of the wanted image scnapshot.
     * @return {@link BufferedImage}.
     */
    public final BufferedImage getSnapshot(String imageType){
        WritableImage image = new WritableImage((int)(canvas.getWidth()),(int)(canvas.getHeight()));
        canvas.snapshot(null, image);
        BufferedImage bufferedImage= SwingFXUtils.fromFXImage(image,null);
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

    public final void setImageDefinition(int hResolution, int vResolution){
        this.imageHDefinition = hResolution;
        this.imageVDefinition = vResolution;
        canvas.getGraphicsContext2D().setFont(new Font(Font.getDefault().getName(),
                Math.max(hResolution,vResolution)*resolution/3.5));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        MAX_RESOLUTION = Math.max(screenSize.width/hResolution+1, screenSize.height / vResolution+1);
    }
    /**
     * Loads the image inside the buffer and stores its metadatas.
     * @param imageBuffers Image buffers with each pixel opacity in byte.
     * @param currentChars Characters represented by the images.
     */
    public final void loadImages(byte[][] imageBuffers,char[] currentChars, int[] currentIndices){
        this.imageBuffers = imageBuffers;
        this.currentChars=new String[currentChars.length];
        for (int i = 0; i<currentChars.length; i++)
            this.currentChars[i] = String.valueOf(currentChars[i]);
        this.currentIndices = currentIndices;
        repaint();
    }

    /**
     * Prepares the color pallet used to paint the images.
     */
    public final void initializePallet(Color color){
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
        if(imageBuffers!= null && imageBuffers.length >0) repaint();
    }

    /**
     * Set the background color of the canvas.
     * @param backGroundColor {@link Color} of background.
     */
    public final void setBackGroundColor(Color backGroundColor) {
        this.backGroundColor = backGroundColor;
        initializePallet(pallet[255]);
        repaint();
    }

    /**
     * Sets the boolean flag that informs the canvas if we want to draw the labels.
     * @param selected boolean true if the labels are wanted to be drawn.
     */
    public final void setLabelVisible(boolean selected) {
        isLabelVisible=selected;
        repaint();
    }

    /**
     * Stores when the label has to be drawn.
     * @param positionString Is actually a {@link POSITION} called by a String thanks to the enum possibilities.
     */
    public final void setLabelPosition(String positionString){
        currentLabelPosition= POSITION.valueOf(positionString);
        repaint();
    }

    /**
     * Sets the down threshold for filtering the image :
     * every value equal or below will show a "blank" pixel.
     * @param downValue Limit value to be a blank pixel.
     */
    public final void setDownFilter(int downValue) {
        this.filterDownThreshold = downValue;
        updateFiltering();
    }

    /**
     * Sets the up threshold for filtering the image :
     * every value equal or above will show a "full" pixel.
     * @param upValue Limit value to be a fully opaque pixel.
     */
    public final void setUpFilter(int upValue) {
        this.filterUpThreshold = upValue;
        updateFiltering();
    }

    /**
     * Setups the behaviour of the {@link Tooltip}.
     * @param showHint Tells if the {@link Tooltip} is enabled.
     * @param showHintCoord Tells if the {@link Tooltip} shows the cursor coordinate on the {@link Canvas canvas}.
     * @param showHintValue Tells if the {@link Tooltip} shows the value of the pixel pointed by the cursor.
     */
    public final void updateHintSetup(boolean showHint, boolean showHintIndex, boolean showHintCoord, boolean showHintValue) {
        this.showHint=showHint;
        this.showHintCoord=showHintCoord;
        this.showHintValue=showHintValue;
        this.showHintIndex = showHintIndex;
    }

    public final void setResolution(int newResolution){
        if (this.resolution == newResolution || newResolution <MIN_RESOLUTION || newResolution > MAX_RESOLUTION)
            return;
        /*if((int)canvas.getWidth()<newResolution*imageHDefinition ||
                (int)canvas.getHeight()<newResolution*imageVDefinition)
            newResolution = Math.min((int)(canvas.getWidth() / imageHDefinition) ,
                    (int)(canvas.getHeight() / imageVDefinition) );*/
        this.resolution=newResolution;
        canvas.getGraphicsContext2D().setFont(new Font(Font.getDefault().getName(),
                Math.max(imageVDefinition, imageHDefinition)*resolution/3.5));
        repaint();
        if(provider != null) provider.notifySizeChange();
    }

    /**
     * Returns the text that is to show on the {@link Tooltip}.
     * @return {@link String Text}.
     */
    protected final String getHintText(int index){
        if(indexBelowMouse>=imageBuffers.length){
            Tooltip.uninstall(canvas,pxHint);
            return null;
        }
        StringBuilder text = new StringBuilder();
        if(showHintIndex) text.append("[").append(currentIndices[indexBelowMouse]).append("] ");
        if(showHintCoord) text.append("(").append(xMouse).append(";").append(yMouse).append(")");
        if(showHintCoord&&showHintValue) text.append("=");
        if(showHintValue)text.append((imageBuffers[index][(yMouse* imageHDefinition +xMouse)]&0xff));

        return text.toString();
    }

    /**
     * Makes the {@link Canvas canvas} repaint itself.
     */
    protected final void repaint(){
        if(imageBuffers== null) return;
        CanvasData data = getCanvasData();
        redrawTask.requestRedraw(data);
    }

    /**
     * Refreshes the drawing information {@link Canvas canvas} calcultates length to draw.
     */
    protected final void refresh(){
        if(imageBuffers== null) return;
        getCanvasData();
    }

    protected final int getHorizontalDefinition(){
        return resolution * imageHDefinition;
    }
    protected final int getVerticalDefinition(){
        return resolution * imageVDefinition;
    }
    protected final int getResolution(){
        return resolution;
    }

    protected final void forceDeltaPosition(int delta){
        if(provider != null && delta !=0) provider.forceDeltaPosition(delta);
    }

    /**
     * Checks if the image needs filtering and reinitialize the pallet in consequence.
     */
    private void updateFiltering(){
        isFiltered = !(filterDownThreshold ==0 && filterUpThreshold ==255);
        initializePallet(pallet[255]);
    }

    private abstract static class CanvasRedrawTask<T> extends AnimationTimer {
        private final AtomicReference<T> data= new AtomicReference<>(null);
        private final Canvas canvas;
        private  long lastRefresh=0L;

        public CanvasRedrawTask(Canvas canvas){
            this.canvas = canvas;
        }
        public void requestRedraw(T dataToDraw){
            data.set(dataToDraw);
            start();
        }

        public void handle(long now){
//            if(now-lastRefresh>= refreshTimeNs) {
                T dataToDraw = data.getAndSet(null);
                if (dataToDraw != null)
                    redraw(canvas.getGraphicsContext2D(), dataToDraw);
                lastRefresh=now;
//            }
        }
        protected abstract void redraw(GraphicsContext context, T data);
    }
}
