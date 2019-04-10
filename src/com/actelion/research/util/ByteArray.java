package com.actelion.research.util;

import java.io.*;

/*
 * @(#) ByteArray.java
 *
 * Copyright 1997-2001 Actelion Ltd., Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.
 *
 * @author rufenec
 */
public class ByteArray
{
    private byte data[];

    ByteArray(byte data[])
    {
        this.data = data;
    }

    byte[] byteValue()
    {
        return data;
    }

    int compareTo(ByteArray o)
    {
        int len = data.length;
        int lenb = o.data.length;
        for (int i = 0;; i++) {
            int a = 0, b = 0;
            if (i < len) {
                a = ((int) data[i]) & 0xff;
            } else if (i >= lenb) {
                return 0;
            }
            if (i < lenb) {
                b = ((int) o.data[i]) & 0xff;
            }
            if (a > b) {
                return 1;
            }
            if (b > a) {
                return -1;
            }
        }
    }

    int compareTo(ByteArray o,int maxlen)
    {
        int len = Math.min(data.length,maxlen);
        int lenb = Math.min(o.data.length,maxlen);
        if (maxlen > data.length || maxlen > o.data.length)
            throw new RuntimeException("Cannot compare bytearray!");
        
        for (int i = 0; i < maxlen; i++) {
            int a = 0, b = 0;
            a = ((int) data[i]) & 0xff;
            b = ((int) o.data[i]) & 0xff;
            if (a > b) {
                return 1;
            }
            if (b > a) {
                return -1;
            }
        }
        return 0;
    }
    
    public static int compareArrays(byte[] rhs,  byte[] lhs)
    {
        ByteArray r = new ByteArray(rhs);
        ByteArray l = new ByteArray(lhs);
        return r.compareTo(l);
    }
    public static int compareArrays(byte[] rhs,  byte[] lhs, int maxlen)
    {
        ByteArray r = new ByteArray(rhs);
        ByteArray l = new ByteArray(lhs);
        return r.compareTo(l,maxlen);
    }
}
