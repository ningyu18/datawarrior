/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of ActelionMMFF94.
 * 
 * ActelionMMFF94 is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * ActelionMMFF94 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with ActelionMMFF94.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Paolo Tosco,Daniel Bergmann
 */

package com.actelion.research.forcefield.mmff.table;

import com.actelion.research.forcefield.mmff.Csv;
import com.actelion.research.forcefield.mmff.Tables;

/**
 * Equivalence table, corresponds to MMFFDEF.PAR parameters table provided
 * in the MMFF literature. This table supplies the atom type equivalences
 * used in the matching of parameters to force-field interactions. This
 * table does not implement Searchable and so can not be used with the
 * binary search.
 */
public final class Def {
    public final int[][] table;

    public Def(Tables t, String csvpath) {
        table = Csv.readIntsFile(csvpath);
    }
}
