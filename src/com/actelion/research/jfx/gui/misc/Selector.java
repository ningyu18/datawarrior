/*
 * Project: DD_jfx
 * @(#)Selector.java
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

package com.actelion.research.jfx.gui.misc;

/**
 * Created with IntelliJ IDEA.
 * User: rufenec
 * Date: 8/6/13
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Selector<T>
{
    public boolean match(T t);
}
