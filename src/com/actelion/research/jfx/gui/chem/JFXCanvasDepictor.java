/* * Project: DD_jfx * @(#)JFXCanvasDepictor.java * * Copyright (c) 1997- 2015 * Actelion Pharmaceuticals Ltd. * Gewerbestrasse 16 * CH-4123 Allschwil, Switzerland * * All Rights Reserved. * * This software is the proprietary information of Actelion Pharmaceuticals, Ltd. * Use is subject to license terms. * * Author: Christian Rufener */package com.actelion.research.jfx.gui.chem;import com.actelion.research.chem.AbstractDepictor;import com.actelion.research.chem.StereoMolecule;import com.actelion.research.jfx.gui.GraphicsContextImpl;import javafx.geometry.Bounds;import javafx.geometry.VPos;import javafx.scene.canvas.GraphicsContext;import javafx.scene.paint.Color;import javafx.scene.shape.ArcType;import javafx.scene.shape.StrokeLineCap;import javafx.scene.shape.StrokeLineJoin;import javafx.scene.text.Font;import javafx.scene.text.Text;import javafx.scene.text.TextAlignment;/** * Project: * User: rufenec * Date: 10/12/11 * Time: 6:00 PM */public class JFXCanvasDepictor extends AbstractDepictor{    private GraphicsContext ctx = null;    private Font currentFont = Font.font("Helvetica", 8);    ;    private double lineWidth = 1.0f;    public JFXCanvasDepictor(StereoMolecule mol, int mode)    {        super(mol != null ? mol : new StereoMolecule());        super.setDisplayMode(mode);    }    public JFXCanvasDepictor(StereoMolecule mol)    {        this(mol, 0);    }    protected void init() {        super.init();    }    @Override    public void paint(Object g)    {        if (g instanceof GraphicsContext) {            ctx = (GraphicsContext) g;        } else if (g instanceof GraphicsContextImpl) {            ctx = ((GraphicsContextImpl) g).getContext();        } else {            throw new IllegalArgumentException("Need to pass a Canvas object " + g);        }        super.paint(g);    }    @Override    protected void drawBlackLine(DepictorLine theLine)    {        ctx.save();        ctx.setLineWidth(lineWidth);        ctx.setLineCap(StrokeLineCap.ROUND);        ctx.setLineJoin(StrokeLineJoin.MITER);//        ctx.setStroke(colormap.color);        ctx.beginPath();        ctx.moveTo(theLine.x1, theLine.y1);        ctx.lineTo(theLine.x2,theLine.y2);        ctx.stroke();//        ctx.strokeLine(theLine.x1, theLine.y1, theLine.x2, theLine.y2);        ctx.restore();    }    @Override    protected void drawDottedLine(DepictorLine theLine)    {        ctx.save();//        ctx.setStroke(colormap.color);        ctx.setLineCap(StrokeLineCap.ROUND);        ctx.beginPath();        ctx.moveTo(theLine.x1,theLine.y1);        ctx.lineTo(theLine.x2,theLine.y2);        ctx.stroke();//        ctx.strokeLine(theLine.x1, theLine.y1, theLine.x2, theLine.y2);        ctx.restore();    }    @Override    protected void drawPolygon(double[] px, double[] py, int count)    {        ctx.save();//        ctx.setStroke(colormap.color);//        ctx.setFill(colormap.color);        double[] x = new double[count];        double[] y = new double[count];        for (int i = 0; i < count; i++) {            x[i] = px[i];            y[i] = py[i];        }        ctx.fillPolygon(x, y, count);//        ctx.strokePolygon(x, y, count);        ctx.restore();    }    @Override    protected void drawString(String theString, double x, double y)    {        ctx.save();        ctx.setFont(currentFont);        ctx.setTextAlign(TextAlignment.CENTER);        ctx.setTextBaseline(VPos.CENTER);//        ctx.setFill(colormap.color);        ctx.fillText(theString, x, y);        ctx.restore();    }    private Bounds getBounds(String s)    {        Text t = new Text(s);        t.setFont(currentFont);        return t.getLayoutBounds();    }    @Override    protected void fillCircle(double x, double y, double r)    {        ctx.save();//        if (colormap != null) {//            ctx.setFill(colormap.color);//        }        ctx.fillArc(            (double) x, (double) y,            (double) r, (double) r, (double) 0,            (double) 360, ArcType.ROUND);        ctx.restore();    }    @Override    protected double getStringWidth(String theString)    {//        Trace.trace(theString);        return (double) getBounds(theString).getWidth();    }    @Override    protected int getTextSize()    {        return (int) currentFont.getSize();    }    @Override    protected void setTextSize(int theSize)    {        currentFont = Font.font("Helvetica", theSize);    }    @Override    protected void setLineWidth(double lineWidth)    {        this.lineWidth = lineWidth;    }    @Override    protected double getLineWidth()    {        return lineWidth;    }    @Override    protected void setColor(java.awt.Color theColor)    {        if (ctx != null) {            Color color = new Color(theColor.getRed() / 255.0, theColor.getGreen() / 255.0, theColor.getBlue() / 255.0, 1.0);            ctx.setStroke(color);            ctx.setFill(color);        }         //colormap = getColor(theColor);    }}