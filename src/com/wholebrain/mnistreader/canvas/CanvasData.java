package com.wholebrain.mnistreader.canvas;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

class CanvasData{
    private AtomicIntegerArray posX, posY;

    CanvasData(AtomicIntegerArray posX, AtomicIntegerArray posY){
        this.posX = posX;
        this.posY = posY;
    }
    CanvasData (AtomicInteger posX, AtomicInteger posY){
        this.posX= new AtomicIntegerArray(1);
        this.posX.set(0, posX.get());
        this.posY = new AtomicIntegerArray(1);
        this.posY.set(0, posY.get());
    }
    AtomicIntegerArray getPosX(){
        return posX;
    }
    AtomicIntegerArray getPosY(){
        return posY;
    }
}
