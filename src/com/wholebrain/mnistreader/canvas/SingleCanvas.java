package com.wholebrain.mnistreader.canvas;

import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

import java.util.concurrent.atomic.AtomicInteger;

public final class SingleCanvas extends CustomCanvas {

    private double xPos, yPos;
    public SingleCanvas(){
        super();
        canvas.setOnScroll(event -> {
            if(event.isShiftDown())
                forceDeltaPosition(event.getDeltaX() > 0 ? -1 : 1);
            else if(event.isAltDown())
                forceDeltaPosition((int)(-event.getDeltaY()*20));
            else
                forceDeltaPosition(-(int) event.getDeltaY());
        });
    }

    @Override
    protected CanvasData getCanvasData() {
        double size = Double.min(canvas.getHeight(), canvas.getWidth());
        setResolution((int)(Double.min(size/(1.0* imageVDefinition), size/(1.0* imageHDefinition))));
        xPos = Math.floor((canvas.getWidth()-getHorizontalDefinition())/2.0);
        yPos = Math.floor((canvas.getHeight()-getVerticalDefinition())/2.0);

        return new CanvasData(new AtomicInteger((int)xPos),
                new AtomicInteger((int)yPos));
    }

    @Override
    protected void notify(ImageBufferProvider listener) {
        // Since the ScrollBar value is not affected by the size of the canvas,
        // the listener is not notified.
    }

    @Override
    public DIRECTION getScrollBarPosition() {
        return DIRECTION._BOTTOM;
    }

    @Override
    protected EventHandler<MouseEvent> getHintEvent() {
        return mouseEvent -> {
            if(!showHint) Tooltip.uninstall(canvas,pxHint);
            else {
                int currentXMouse = (int) Math.floor((mouseEvent.getX() - xPos) / getResolution());
                int currentYMouse = (int) Math.floor((mouseEvent.getY() - yPos) / getResolution());
                if (currentXMouse < 0 || currentXMouse >= imageHDefinition
                        || currentYMouse < 0 || currentYMouse >= imageVDefinition) {
                    Tooltip.uninstall(canvas, pxHint);
                } else if (currentXMouse != xMouse || currentYMouse != yMouse) {
//                    pxHint.show(canvas, mouseEvent.getScreenX() + 10, mouseEvent.getScreenY() + 10);
                    pxHint.setX(mouseEvent.getScreenX()+10);
                    pxHint.setY(mouseEvent.getScreenY()+10);
                    xMouse = currentXMouse;
                    yMouse = currentYMouse;
                    pxHint.setText(getHintText(0));
                    if (!pxHint.isActivated())
                        Tooltip.install(canvas, pxHint);
                }
            }
        };
    }


    @Override
    public int getShownImageCount() {
        return 1;
    }

    @Override
    public int getIndexFor(int position) {
        return position;
    }

    @Override
    public int getScrollValueForIndex(int index) {
        return index;
    }

    @Override
    public int getScrollBarMaxValueFor(int elementCount) {
        return elementCount-1;
    }

    @Override
    public double getScrollBarUnitIncrement() {
        return 1;
    }

    @Override
    protected boolean isResolutionModifiable() {
        return false;
    }

    public void loadImage(byte[] imageBuffer, char currentChar, int currentIndex){
        loadImages(new byte[][]{imageBuffer}, new char[] {currentChar}, new int[]{currentIndex});
    }
}
