package com.wholebrain.mnistreader.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public final class MultipleCanvas extends CustomCanvas {
    private int line1StartY =0, xMouse, yMouse, imagesPerLine =1, imagesPerColumn =1, gap;
    private double currentYPos = 0;

    @Override
    protected void paintLabels(GraphicsContext graphicsContext) {

    }

    @Override
    protected void initializeHint(Canvas canvas) {

    }

    @Override
    public DIRECTION getScrollBarPosition() {
        return DIRECTION._RIGHT;
    }

    @Override
    public int getShownImageCount() {
        return Math.max(imagesPerLine * imagesPerColumn,1);
    }

    @Override
    public int getFirstShownIndex(int indexTry, int numberOfImages) {
        int lastToFillCanvas =numberOfImages-numberOfImages%getShownImageCount() -1;
        return (indexTry>lastToFillCanvas?
                lastToFillCanvas+1:
                indexTry-indexTry%Math.max(imagesPerLine,1));
    }

    @Override
    public int getIndexFor(int scrollValue) {
        line1StartY = scrollValue %(gap+resolution* imageVResolution);
        System.out.println("GET INDEX FOR : "+scrollValue+"\n\tLine starts at "+line1StartY+"\n\t"
                +(int)(imagesPerLine *(scrollValue/(gap+resolution* imageVResolution)))+
                "\n\tImages shown = "+ imagesPerLine +"*"+ imagesPerColumn);
        return imagesPerLine *(scrollValue/(gap+resolution* imageVResolution));
    }

    @Override
    public int getScrollValueForIndex(int index) {
        System.out.println("\n\n\tScroll Value for index "+ index +" = "+
                (line1StartY+ (index/ imagesPerLine)*(gap+resolution* imageVResolution))+
                "\n\t with line 1 starting at "+line1StartY);
        return line1StartY+ (index/ imagesPerLine)*(gap+resolution* imageVResolution);
    }


    @Override
    public int getScrollBarMaxValueFor(int elementCount) {
        return -(int)((Math.floor(-elementCount/(imagesPerLine *1.0))+ imagesPerColumn)*(gap+ imageVResolution *resolution));
    }

    @Override
    public double getScrollBarUnitIncrement() {
        return resolution*imageVResolution/3;
//        return 1;
    }

    protected void paint(GraphicsContext graphicsContext){
        graphicsContext.setFill(backGroundColor);
        graphicsContext.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        if(imageBuffers==null || imageBuffers.length ==0)
            return;
        calculateImageCount();
        // first row of images
        /*for (int i = 0; i<xNumber && i<imageBuffers.length; i++)
            drawImage(graphicsContext, i, line1StartY,
                    (int)(i* imageHResolution *resolution+gap*(i+1)),gap);
        for (int i = xNumber; i<yNumber*xNumber && i < imageBuffers.length; i++)
            drawImage(graphicsContext, i, 0,
                    (int)(i%xNumber*(imageHResolution *resolution))+gap*(i%xNumber+1),
                    (int)(gap*(1+i/xNumber)+resolution* imageVResolution *(i/xNumber)));*/
        //todo avoid to draw above the first line
        for (int i = 0; i< imagesPerLine * imagesPerColumn && i<imageBuffers.length; i++)
            drawImage(graphicsContext, i,
                    i% imagesPerLine *imageHResolution*resolution+gap*(i% imagesPerLine +1),
                    gap*(1+i/ imagesPerLine)+resolution*imageVResolution*(i/ imagesPerLine)-line1StartY);
    }

    private void drawImage(GraphicsContext gc, int index, int xPosOnContext, int yPosOnContext){
        for(int y = 0; y< imageVResolution; y++)
            for (int x = 0; x< imageHResolution; x++){
                gc.setFill(pallet[imageBuffers[index][y* imageVResolution +x]&0xFF]);
                gc.fillRect(xPosOnContext+x*resolution, yPosOnContext+y*resolution,resolution,resolution);
            }
    }

    private void calculateImageCount(){
        imagesPerLine = Math.max((int)(canvas.getWidth()/(resolution * imageHResolution)),1);
        gap = (int)((canvas.getWidth()-(imagesPerLine * imageHResolution *resolution))/(imagesPerLine +1.0));
        int remainY = (int)(canvas.getHeight()-(imageVResolution - line1StartY)*resolution)-gap;
        imagesPerColumn = Math.max((int)(remainY/(imageVResolution *resolution+gap))+1,1);
        /*System.out.println("*********** MULTIPLECANVAS.PAINT()*********\n\tResolution = "+resolution
                +"\tImage definition = "+numberOfColumns+"*"+numberOfRows
                +"("+(int)canvas.getWidth()+"*"+(int)canvas.getHeight()+")\n\txNumber = "+xNumber
                +"\n\tyNumber = "+yNumber+"\n\tGAP = "+gap);*/
    }
}
