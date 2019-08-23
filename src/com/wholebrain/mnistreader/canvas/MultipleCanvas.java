package com.wholebrain.mnistreader.canvas;

import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

import java.util.concurrent.atomic.AtomicIntegerArray;

public final class MultipleCanvas extends CustomCanvas {
    private int firstLineStartY =0, imagesPerLine =1, imagesPerColumn =1, gap;

    public MultipleCanvas(){
        super();
        canvas.setOnScroll(event -> {
            if (event.isControlDown())
                setResolution(getResolution() + (event.getDeltaY() > 0 ? 1 : -1));
            else if(event.isShiftDown())
                forceDeltaPosition(event.getDeltaX() > 0 ? -1 : 1);
            else if(event.isAltDown())
                forceDeltaPosition((int)(-event.getDeltaY()*20));
            else
                forceDeltaPosition(-(int) event.getDeltaY());
        });
    }

    @Override
    protected EventHandler<MouseEvent> getHintEvent() {
        return mouseEvent -> {
            if (!showHint) Tooltip.uninstall(canvas, pxHint);
            else {
                int currentXMouse = (int)mouseEvent.getX();
                int currentYMouse = (int)mouseEvent.getY();
                int frameHDefinition = getHorizontalDefinition()+gap;

                if(currentXMouse%(frameHDefinition)< gap){
                    Tooltip.uninstall(canvas, pxHint);
                }
                else{
                    int frameVDefinition = getVerticalDefinition()+gap;
                    int yMouse2 = currentYMouse+firstLineStartY;
                    if(yMouse2%(gap+ getVerticalDefinition())<gap)
                        Tooltip.uninstall(canvas, pxHint);
                    else{
                        int column = currentXMouse/frameHDefinition;
                        int line = yMouse2/(frameVDefinition);
                        int xStart = column*frameHDefinition+gap;
                        int yStart = line*frameVDefinition-firstLineStartY+gap;
                        int xCoord = (currentXMouse-xStart)/(getResolution());
                        int yCoord = (currentYMouse-yStart)/(getResolution());
                        if(xMouse!= xCoord || yMouse != yCoord){
                            pxHint.setX(mouseEvent.getScreenX()+10);
                            pxHint.setY(mouseEvent.getScreenY()+10);
                            xMouse=xCoord;
                            yMouse = yCoord;
                            indexBelowMouse = line*imagesPerLine+column;
                            String tipText = getHintText(line*imagesPerLine+column);
                            pxHint.setText(tipText);
                            if(!pxHint.isActivated()&& tipText!=null)
                                Tooltip.install(canvas, pxHint);
                        }


                    }
                }
            }
        };
    }

    @Override
    protected void notify(ImageBufferProvider listener) {
        listener.notifySizeChange();
    }

    @Override
    public DIRECTION getScrollBarPosition() {
        return DIRECTION._RIGHT;
    }

    @Override
    public int getShownImageCount() {
        return Math.max(imagesPerLine * (imagesPerColumn),1);
    }

    @Override
    public int getIndexFor(int position) {
        firstLineStartY = position %(gap+getVerticalDefinition());
//        calculateImageCount();
        return imagesPerLine *(position /(gap+getVerticalDefinition()));
    }

    @Override
    public int getScrollValueForIndex(int index) {
        return (index/ imagesPerLine)*(gap+getVerticalDefinition());
    }

    @Override
    public int getScrollBarMaxValueFor(int elementCount) {
        int numberOfLines = (int)Math.ceil(elementCount*1.0/imagesPerLine);
        int totalSize = numberOfLines*(getHorizontalDefinition())+(numberOfLines+1)*gap;
        return totalSize-(int)canvas.getHeight();
    }

    @Override
    public double getScrollBarUnitIncrement() {
        return getVerticalDefinition() /4;
    }

    protected CanvasData getCanvasData(){
        calculateImageCount();
        AtomicIntegerArray posX = new AtomicIntegerArray(imagesPerLine*imagesPerColumn),
            posY = new AtomicIntegerArray(imagesPerLine*imagesPerColumn);
        for (int i = 0; i<posX.length(); i++){
            posX.set(i,i%imagesPerLine*getHorizontalDefinition()+gap*(i%imagesPerLine+1));
            posY.set(i, gap*(1+i/ imagesPerLine)+getVerticalDefinition() *(i/ imagesPerLine)- firstLineStartY);
        }
        return new CanvasData(posX, posY);
    }


    private void calculateImageCount(){
        imagesPerLine = Math.max((int)(canvas.getWidth()/getHorizontalDefinition()),1);
        gap = (int)((canvas.getWidth()-(imagesPerLine * getHorizontalDefinition()))/(imagesPerLine +1.0));
        if(gap<0) gap = 0;
        if(canvas.getHeight()< getVerticalDefinition()+gap)
            gap=Math.min(gap,((int)canvas.getHeight()- getVerticalDefinition())/2);
        imagesPerColumn= (int)Math.ceil(canvas.getHeight()/(getVerticalDefinition()+gap))+1;
    }
}
