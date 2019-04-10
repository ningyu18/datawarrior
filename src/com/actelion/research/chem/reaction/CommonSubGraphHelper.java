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
 * @author Christian Rufener
 */

package com.actelion.research.chem.reaction;

import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.mcs.MCS;

import java.util.ArrayList;
import java.util.Arrays;

public class CommonSubGraphHelper
{

    static class Result
    {
        private StereoMolecule molecule;
        private int reactant;
        private int product;

        public Result(StereoMolecule m, int rI, int pI)
        {
            setMolecule(m);
            setReactant(rI);
            setProduct(pI);
        }

        public String toString()
        {
            return "MCS Result: R=" + getReactant() + " P=" + getProduct() + " #Atoms=" + getMolecule().getAllAtoms();
        }

        public StereoMolecule getMolecule()
        {
            return molecule;
        }

        public void setMolecule(StereoMolecule molecule)
        {
            this.molecule = molecule;
        }

        public int getReactant()
        {
            return reactant;
        }

        public void setReactant(int reactant)
        {
            this.reactant = reactant;
        }

        public int getProduct()
        {
            return product;
        }

        public void setProduct(int product)
        {
            this.product = product;
        }
    }

//    public static String findMCS(String rxnctab)
//    {
//        RXNFileParser p = new RXNFileParser();
//        Reaction reaction = new Reaction();
//        try {
//            p.parse(reaction,rxnctab);
//            MCS mcs = new MCS(MCS.PAR_KEEP_AROMATIC_RINGS);
//            StereoMolecule a = reaction.getReactant(0);
//            StereoMolecule b = reaction.getProduct(0);
//            mcs.set(a,b);
//            StereoMolecule m = mcs.getMCS();
//            MolfileV3Creator creator = new MolfileV3Creator(m);
//            return creator.getMolfile();
//        } catch (Exception e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//        return null;
//    }
//
//    public static String findMCS(Reaction reaction)
//    {
//        RXNFileParser p = new RXNFileParser();
//        try {
//            MCS mcs = new MCS(MCS.PAR_KEEP_AROMATIC_RINGS);
//            StereoMolecule a = reaction.getReactant(0);
//            StereoMolecule b = reaction.getProduct(0);
//            mcs.set(a,b);
//            StereoMolecule m = mcs.getMCS();
//            MolfileV3Creator creator = new MolfileV3Creator(m);
//            return creator.getMolfile();
//        } catch (Exception e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//        return null;
//    }

    public static Result getMCS(Reaction reaction, boolean[] exclude,SSSearcher sss)
    {
//        MCS mcs = new MCS(MCS.PAR_KEEP_RINGS,sss);
        MCS mcs = new MCS(MCS.PAR_CLEAVE_RINGS,sss);
        return getCommonSubGraph(mcs, reaction, exclude);
    }

    private static Result getCommonSubGraph(MCS mcs, Reaction reaction, boolean[] exclude)
    {
        Result ret = null;
        try {
            int rn = reaction.getReactants();
            int pn = reaction.getProducts();
            StereoMolecule res = null;
            int resReactantIndex = -1;
            int resProcductIndex = -1;
            int max = 0;

            // Look for the biggest overlap
            for (int r = 0; r < rn; r++) {
                StereoMolecule reactant = reaction.getReactant(r);
                for (int p = 0; p < pn; p++) {
                    StereoMolecule product = reaction.getProduct(p);
                    mcs.set(product, reactant, exclude);
                    StereoMolecule m = mcs.getMCS();
                    if (m != null) {
                        int na = m.getAllAtoms();
                        if (max < na) {
                            res = m;
                            resReactantIndex = r;
                            resProcductIndex = p;
                            max = na;
//                            System.out.printf("Reactant %d -> Product %d max = %d\n",r,p,max);
                        }
                    }
                }
            }

//            for (int r = rn-1; r >= 0 ; r--) {
//                StereoMolecule reactant = reaction.getReactant(r);
//                for (int p = 0; p < pn ; p++) {
//                    StereoMolecule product = reaction.getProduct(p);
//                     mcs.set(product, reactant, null);
//                    StereoMolecule m = mcs.getMCS();
//                    if (m != null) {
//                        int na = m.getAllAtoms();
//                        if (max < na) {
//                            res = m;
//                            resReactantIndex = r;
//                            resProcductIndex = p;
//                            max = na;
//                        }
//                    }
//                }
//            }

            if (res != null) {
                //return new MCSResult(res,resReactantIndex,resProcductIndex);
                ret = new Result(res, resReactantIndex, resProcductIndex);
            }

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return ret;
    }

//    public static Result[] getMCS2(Reaction rxn)
//    {
//        Reaction reaction = new Reaction(rxn);
//        MCS mcs = new MCS(MCS.PAR_KEEP_AROMATIC_RINGS);
//        Result res = getCommonSubGraph(mcs, reaction);
//        remove(reaction, res.getMolecule(), res.getReactant(), res.getProduct());
//        Result res2 = getCommonSubGraph(mcs, reaction);
//
//        return new Result[]{
//            res,
//            res2
//        };
//    }

    private static void remove(Reaction rxn, StereoMolecule fragment, int rIndex, int pIndex)
    {
        SSSearcher searcher = new SSSearcher();

        StereoMolecule reactant = rxn.getReactant(rIndex);
        StereoMolecule product = rxn.getProduct(pIndex);
        StereoMolecule candidates[] = {
            reactant,
            product
        };
        for (StereoMolecule candidate : candidates) {
            searcher.setMol(fragment, candidate);
            searcher.findFragmentInMolecule(SSSearcher.cCountModeOverlapping, SSSearcher.cDefaultMatchMode);
            ArrayList<int[]> matchList = searcher.getMatchList();
            if (matchList != null) {
                int count = matchList.get(0).length;
//                for (int[] matching : matchList) {
//                    for (int k = 0; k < matching.length; k++) {
//                        count++;
//                    }
//                }
                int remove[] = new int[count];
                int idx = 0;
//                for (int[] matching : matchList) {
                int[] matching = matchList.get(0);
                {
                    for (int k = 0; k < matching.length; k++) {
                        remove[idx++] = matching[k];
                    }
                }
                Arrays.sort(remove);
                for (int i = remove.length - 1; i >= 0; i--) {
                    candidate.deleteAtom(remove[i]);

                }
            }
        }
        rxn.normalize();
    }


}
