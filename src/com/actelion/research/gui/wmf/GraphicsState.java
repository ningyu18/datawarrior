/*
 * Project: DD_jfx
 * @(#)GraphicsState.java
 *
 * Copyright (c) 1997- 2015
 * Actelion Pharmaceuticals Ltd.
 * Gewerbestrasse 16
 * CH-4123 Allschwil, Switzerland
 *
 * All Rights Reserved.
 *
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.
 *
 * Author: Christian Rufener
 */

package com.actelion.research.gui.wmf;

import java.awt.Point;
import java.awt.Rectangle;

class GraphicsState
{

    GraphicsState()
    {
        origin = new Point(0, 0);
    }

    void decreaseCount()
    {
        count--;
    }

    int getBrush()
    {
        return brushhandle;
    }

    Rectangle getClip()
    {
        return clip;
    }

    int getCount()
    {
        return count;
    }

    int getFont()
    {
        return fonthandle;
    }

    Point getOrigin()
    {
        return origin;
    }

    int getPen()
    {
        return penhandle;
    }

    void increaseCount()
    {
        count++;
    }

    void setBrush(int i)
    {
        brushhandle = i;
    }

    void setClip(Rectangle rectangle)
    {
        clip = rectangle;
    }

    void setFont(int i)
    {
        fonthandle = i;
    }

    void setOrigin(Point point)
    {
        origin.move(point.x, point.y);
    }

    void setPen(int i)
    {
        penhandle = i;
    }

    private int count;
    private int penhandle;
    private int brushhandle;
    private int fonthandle;
    private Rectangle clip;
    private Point origin;
}
