/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.chem;

import java.util.Arrays;

public class CanonizerBaseValue implements Comparable<CanonizerBaseValue> {
    public long[] mValue;
    private int mAtom;
    private int mIndex;
    private int mAvailableBits;

    /**
     * @param size depends on the maximum number of non-H neighbors
     *             and whether bond query features are present
     */
    public CanonizerBaseValue(int size) {
        mValue = new long[size];
        }

    public void init(int atom) {
        mAtom = atom;
        mIndex = 0;
        mAvailableBits = 63;
        Arrays.fill(mValue, 0);
        }

    public void add(long data) {
        mValue[mIndex] += data;
        }

    public void add(int bits, long data) {
        if (mAvailableBits == 0) {
            mIndex++;
            mAvailableBits = 63;
            }
        if (mAvailableBits == 63) {
            mValue[mIndex] |= data;
            mAvailableBits -= bits;
            }
        else {
            if (mAvailableBits >= bits) {
                mValue[mIndex] <<= bits;
                mValue[mIndex] |= data;
                mAvailableBits -= bits;
                }
            else {
                mValue[mIndex] <<= mAvailableBits;
                mValue[mIndex] |= (data >> (bits - mAvailableBits));
                bits -= mAvailableBits;
                mIndex++;
                mAvailableBits = 63 - bits;
                mValue[mIndex] |= (data & ((1 << bits) - 1));
                }
            }
        }

    public int getAtom() {
        return mAtom;
        }

    public int compareTo(CanonizerBaseValue b) {
        assert(mIndex == b.mIndex);
        for (int i=0; i<mIndex; i++)
            if (mValue[i] != b.mValue[i])
                return (mValue[i] < b.mValue[i]) ? -1 : 1;
        return (mValue[mIndex] == b.mValue[mIndex]) ? 0
             : (mValue[mIndex] < b.mValue[mIndex]) ? -1 : 1;
        }
    }
