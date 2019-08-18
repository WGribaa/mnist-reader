package com.wholebrain.mnistreader.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public final class MultipleCanvas extends CustomCanvas {
    private int line1StartY =0, xMouse, yMouse, xNumber=1, yNumber=1, gap;
    private double resolution = 1, currentYPos = 0;

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
//        calculateImageCount();
        return Math.max(xNumber*yNumber,1);
    }

    @Override
    public int getFirstShownIndex(int indexTry, int numberOfImages) {
        int lastToFillCanvas =numberOfImages-numberOfImages%getShownImageCount() -1;
        return (indexTry>lastToFillCanvas?
                lastToFillCanvas+1:
                indexTry-indexTry%Math.max(xNumber,1));
    }

    protected void paint(GraphicsContext graphicsContext){
        graphicsContext.setFill(backGroundColor);
        graphicsContext.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        if(imageBuffers==null || imageBuffers.length ==0)
            return;
        calculateImageCount();
        // first row of images
        for (int i = 0; i<xNumber && i<imageBuffers.length; i++)
            drawImage(graphicsContext, i, line1StartY,
                    (int)(i*numberOfColumns*resolution+gap*(i+1)),gap);
        for (int i = xNumber; i<yNumber*xNumber && i < imageBuffers.length; i++)
            drawImage(graphicsContext, i, 0,
                    (int)(i%xNumber*(numberOfColumns*resolution))+gap*(i%xNumber+1),
                    (int)(gap*(1+i/xNumber)+resolution*numberOfRows*(i/xNumber)));
    }

    private void drawImage(GraphicsContext gc, int index, int startY, int xPosOnContext, int yPosOnContext){
        for(int y = startY; y<numberOfRows; y++)
            for (int x = 0; x<numberOfColumns; x++){
                gc.setFill(pallet[imageBuffers[index][y*numberOfRows+x]&0xFF]);
                gc.fillRect(xPosOnContext+x*resolution, yPosOnContext+y*resolution,resolution,resolution);
            }
    }

    private void calculateImageCount(){
        xNumber = Math.max((int)(canvas.getWidth()/(resolution * numberOfColumns)),1);
        gap = (int)((canvas.getWidth()-(xNumber*numberOfColumns*resolution))/(xNumber+1.0));
        int remainY = (int)(canvas.getHeight()-(numberOfRows- line1StartY)*resolution)-gap;
        yNumber = Math.max((int)(remainY/(numberOfRows*resolution+gap))+1,1);
        System.out.println("*********** MULTIPLECANVAS.PAINT()*********\n\tResolution = "+resolution
                +"\tImage definition = "+numberOfColumns+"*"+numberOfRows
                +"("+(int)canvas.getWidth()+"*"+(int)canvas.getHeight()+")\n\txNumber = "+xNumber
                +"\n\tyNumber = "+yNumber+"\n\tGAP = "+gap);
    }
}
