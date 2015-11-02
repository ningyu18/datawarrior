/*
 * Copyright 2015 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
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

package mmff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.actelion.research.chem.ExtendedMolecule;

/**
 * The MMFF ForceField class is the top level class used to perform
 * energy calculations/minimisation on a molecule. It accepts an
 * ExtendedMolecule and the string name of the parameter tables to use.
 * Options can also be passed to control which energy terms should be
 * included in the force field equation (default: all 7 terms are
 * included), which non-bonded cutoff should be used (default: 100.0
 * angstrom), which dielectric constant should be used (default: 1.0),
 * and whether a constant or a distance-dependent dielectric model should
 * be used (default: linear).
 *
 * Tables: The Tables object contains a collection of parameter tables.
 * These are added statically to the ForceField class to allow all
 * ForceField instances to select which single instance of parameter
 * Tables to use. For example, there would be one tables instance for
 * MMFF94 and another tables instance for MMFF94s. Then when constructing
 * a ForceField object either MMFF94 or MMFF94s can be selected by passing
 * the string identifier used when loading that table with "loadTable()".
 * Options:
 *  - "nonbonded cutoff": A double, limits the number of Electrostatic and
 *      VanDerWaals energy terms to those involving atoms not further
 *      apart than "nonbonded cutoff" (default: 100.0 angstrom).
 *  - "dielectric constant": A double, the dielectric constant value
 *      (default: 1.0)
 *  - "dielectric model": A string for the dielectric model. "distance"
 *      selects a distance-dependent dielectric model, everything else
*       selects the constant dielectric model (default: "constant")
 *  - "angle bend": A boolean, default True, for whether to include angle
 *      bending energy terms.
 *  - "bond stretch": A boolean, default True, for whether to include bond
 *      stretching energy terms.
 *  - "electrostatic": A boolean, default True, for whether to include the
 *      nonbonded electrostatic energy terms.
 *  - "out of plane": A boolean, default True, for whether to include out
        of plane energy terms.
 *  - "stretch bend": A boolean, default True, for whether to include
        stretch bending energy terms.
 *  - "torsion angle": A boolean, default True, for whether to include
 *      torsional angle energy terms.
 *  - "van der waals": A boolean, default True, for whether to include the
 *      nonbonded van der Waals energy terms.
 */
public final class ForceField {
	public static final String MMFF94 = "MMFF94";
	public static final String MMFF94S = "MMFF94s";

	public static final double FUNCTOL = 1e-4;
    public static final double MOVETOL = 1e-7;
    public static final double EPS = 3e-8;
    public static final double TOLX = 4.0*EPS;
    public static final double MAXSTEP = 100.0;

    //protected final ExtendedMolecule mol;
    protected final Molecule mol;
    public static Map<String, Tables> tables = new HashMap<String, Tables>();
    protected List<EnergyTerm> energies = new ArrayList<EnergyTerm>();

    protected final int dim;
    protected double[] pos;
    protected double[] newpos;
    protected double[] grad;

    protected double total_engy;

    /**
     * Forcefield constructor.
     *  @param m The molecule to construct the forcefield on.
     *  @param tablename The string name for the Tables to be used. There
     *      must be a table with this name that has been loaded with
     *      "loadTable()".
     *  @param options A Map containing the ForceField options and values.
     *      See class description of a list of options.
     */
    public ForceField(ExtendedMolecule m, String tablename,
            Map<String, Object> options) {
        mol = new mmff.Molecule(m);
        Tables table = tables.get(tablename);

        double nonBondedThresh = options.containsKey("nonbonded cutoff")
            ? ((Double)options.get("nonbonded cutoff")).doubleValue()
            : 100.0;

        double dielConst = options.containsKey("dielectric constant")
            ? ((Double)options.get("dielectric constant")).doubleValue() : 1.0;

        boolean dielModel = options.containsKey("dielectric model")
            ? ((String)options.get("dielectric model")).equals("distance")
            : false;

        dim = 3*mol.getAllAtoms();

        grad = new double[dim];
        pos = new double[dim];
        newpos = new double[dim];

        // get the atom positions to be placed in the pos array.
        for (int i=0; i<mol.getAllAtoms(); i++) {
            pos[3*i    ] = mol.getAtomX(i);
            pos[3*i + 1] = mol.getAtomY(i);
            pos[3*i + 2] = mol.getAtomZ(i);
        }

        Separation sep = new Separation(mol);

        if (!options.containsKey("angle bend")
                || (Boolean)options.get("angle bend"))
            energies.addAll(AngleBend.findIn(table, mol));

        if (!options.containsKey("bond stretch")
                || (Boolean)options.get("bond stretch"))
            energies.addAll(BondStretch.findIn(table, mol));

        if (!options.containsKey("electrostatic")
                || (Boolean)options.get("electrostatic"))
            energies.addAll(Electrostatic.findIn(table, mol, sep,
                        nonBondedThresh, dielModel, dielConst));

        if (!options.containsKey("out of plane")
                || (Boolean)options.get("out of plane"))
            energies.addAll(OutOfPlane.findIn(table, mol));

        if (!options.containsKey("stretch bend")
                || (Boolean)options.get("stretch bend"))
            energies.addAll(StretchBend.findIn(table, mol));

        if (!options.containsKey("torsion angle")
                || (Boolean)options.get("torsion angle"))
            energies.addAll(TorsionAngle.findIn(table, mol));

        if (!options.containsKey("van der waals")
                || (Boolean)options.get("van der waals"))
            energies.addAll(VanDerWaals.findIn(table, mol, sep, nonBondedThresh));
    }

    /**
     * Forcefield constructor. Overloaded to pass the default (empty)
     * options to a ForceField.
     *  @param mol The molecule to construct the forcefield on.
     *  @param tablename The string name for the Tables to be used. There
     *      must be a table with this name that has been loaded with
     *      "loadTable()".
     */
    public ForceField(ExtendedMolecule mol, String tablename) {
        this(mol, tablename, new HashMap<String, Object>());
    }

    /**
     * Returns the total number of atoms in this force field.
     *  @return Total number of atoms.
     */
    public int size() {
        return mol.getAllAtoms();
    }

    /**
     * Return the variance across all atoms in a molecule
     * for the specified coordinate.
     *  @param c the coordinate index (0, X; 1, Y; 2, Z).
     *  @return variance for the specified coordinate.
     */
    public double coordVariance(int c) {
        double m = 0.0;
        double s = 0.0;
        int k = 1;
        for (int i=0; i<mol.getAllAtoms(); i++) {
            double v;
            switch (c) {
                case 0:
                    v = mol.getAtomX(i);
                    break;
                case 1:
                    v = mol.getAtomY(i);
                    break;
                default:
                    v = mol.getAtomZ(i);
                    break;
            }
            double tm = m;
            m += (v - tm) / (double)k;
            s += (v - tm) * (v - m);
            k++;
        }
        return (k > 1 ? s / (double)(k - 1) : 0.0);
    }

    /**
     * Minimise the current molecule.
     *  @param maxIts The maximum number of iterations to run for.
     *  @param gradTol The gradient tolerance.
     *  @param funcTol The energy tolerance.
     *  @return Return code, 0 on success.
     */
    public int minimise(int maxIts, double gradTol, double funcTol) {
        // get the atom positions to be placed in the pos array.
        int res = 1;
        int at = 0;
        int minVarCoord = -1;
        final int MAX_ATTEMPTS = 10;
        double [] delta = new double[3];
        double [] ctd = new double[3];
        while ((res > 0) && (at < MAX_ATTEMPTS)) {
            if (at > 0 && minVarCoord == -1) {
                double minVar = 0.0;
                for (int c=0; c<3; c++) {
                    double v = coordVariance(c);
                    if (minVarCoord == -1 || v < minVar) {
                        minVarCoord = c;
                        minVar = v;
                    }
                }
            }
            for (int i=0; i<mol.getAllAtoms(); i++) {
                if (at > 0)
                    delta[minVarCoord] = Math.random() - 0.5;
                pos[3*i    ] = mol.getAtomX(i) + delta[0];
                pos[3*i + 1] = mol.getAtomY(i) + delta[1];
                pos[3*i + 2] = mol.getAtomZ(i) + delta[2];
            }
            res = run_minimiser(maxIts, gradTol, funcTol);
            at++;
        }
        if (res == 0)
            for (int i=0; i<mol.getAllAtoms(); i++) {
                mol.setAtomX(i, pos[3*i  ]);
                mol.setAtomY(i, pos[3*i+1]);
                mol.setAtomZ(i, pos[3*i+2]);
            }
        return res;
    }

    /**
     * Minimise the current molecule using the default parameter value for the
     * energy tolerance.
     *  @param maxIts The maximum number of iterations to run for.
     *  @param gradTol The gradient tolerance.
     *  @return Return code, 0 on success.
     */
    public int minimise(int maxIts, double gradTol) {
        return minimise(maxIts, gradTol, 1e-6);
    }

    /**
     * Minimise the current molecule using the default parameter values for
     * the energy tolerance and gradient tolerance.
     *  @param maxIts The maximum number of iterations to run for.
     *  @return Return code, 0 on success.
     */
    public int minimise(int maxIts) {
        return minimise(maxIts, 1e-4, 1e-6);
    }

    /**
     * Minimise the current molecule using default parameter values for
     * the number of iterations, energy tolerance and gradient tolerance.
     *  @return Return code, 0 on success.
     */
    public int minimise() {
        return minimise(200, 1e-4, 1e-6);
    }

    /**
     * Runs the MMFF minimiser.
     */
    protected int run_minimiser(int maxIts,
               double gradTol,
               double funcTol) {
        double sum,maxStep,fp;

        grad = new double[dim];
        double[] dGrad = new double[dim];
        double[] hessDGrad = new double[dim];
        double[] newPos = new double[dim];
        double[] xi = new double[dim];
        double[] invHessian = new double[dim*dim];

        for (int i=0; i<dim; i++)
            newPos[i] = pos[i];

        // evaluate the function and gradient in our current position:
        fp = getTotalEnergy(pos);
        updateGradient();

        sum = 0.0;
        //memset(invHessian,0,dim*dim*sizeof(double));
        for (int i=0; i<dim; i++) {
            // initialize the inverse hessian to be identity
            invHessian[i*dim+i] = 1.0;
            // the first line dir is -grad:
            xi[i] = -grad[i];
            sum += pos[i]*pos[i];
        }

        // pick a max step size:
        maxStep = MAXSTEP * Math.max(Math.sqrt(sum), dim);


        for (int iter=1; iter<=maxIts; iter++) {
            // do the line search:
            int status = linearSearch(pos,fp,xi,newPos,maxStep);

            if (status < 0)
                return 2;

            // save the function value for the next search:
            fp = total_engy;

            // set the direction of this line and save the gradient:
            double test=0.0;
            for (int i=0; i<dim; i++) {
                xi[i] = newPos[i] - pos[i];
                pos[i] = newPos[i];
                double temp = Math.abs(xi[i])/Math.max(Math.abs(pos[i]),1.0);
                if (temp > test)
                    test = temp;
                dGrad[i] = grad[i];
            }

            if (test < TOLX) {
                return 0;
            }

            // update the gradient:
            double gradScale = updateGradient();

            // is the gradient converged?
            test = 0.0;
            double term = Math.max(total_engy*gradScale, 1.0);
            for (int i=0; i<dim; i++) {
                double tmp = Math.abs(grad[i])*Math.max(Math.abs(pos[i]), 1.0);
                test = Math.max(test, tmp);
                dGrad[i] = grad[i] - dGrad[i];
            }

            test /= term;

            if (test < gradTol) {
                return 0;
            }

            // compute hessian*dGrad:
            double fac = 0, fae = 0, sumDGrad = 0, sumXi = 0;
            for(int i=0; i<dim; i++) {
                int itab = i*dim;
                hessDGrad[i] = 0.0;

                for (int j=0; j<dim; j++)
                    hessDGrad[i] += invHessian[itab+j] * dGrad[j];

                fac += dGrad[i] * xi[i];
                fae += dGrad[i] * hessDGrad[i];
                sumDGrad += dGrad[i] * dGrad[i];
                sumXi += xi[i] * xi[i];
            }

            if (fac > Math.sqrt(EPS*sumDGrad*sumXi)) {
                fac = 1.0/fac;
                double fad = 1.0/fae;
                for (int i=0; i<dim; i++)
                    dGrad[i] = fac*xi[i] - fad*hessDGrad[i];

                for (int i=0; i<dim; i++) {
                    int itab = i*dim;
                    for (int j=i; j<dim; j++) {
                        invHessian[itab+j] += fac*xi[i]*xi[j]
                                - fad*hessDGrad[i]*hessDGrad[j]
                                + fae*dGrad[i]*dGrad[j];
                        invHessian[j*dim+i] = invHessian[itab+j];
                    }
                }
            }

            // generate the next direction to move:
            for (int i=0; i<dim; i++) {
                int itab = i*dim;
                xi[i] = 0.0;
                for (int j=0; j<dim; j++)
                    xi[i] -= invHessian[itab+j]*grad[j];
            }
        }
        return 1;
    }

    /**
     *
     */
    private int linearSearch(double[] oldPt,
            double oldVal,
            double[] dir,
            double[] newPt,
            double maxStep) {
        final int MAX_ITER_LINEAR_SEARCH = 1000;
        int ret = -1;
        double [] tmpPt = new double[dim];
        double sum = 0.0, slope = 0.0, test = 0.0, lambda = 0.0;
        double lambda2 = 0.0, lambdaMin = 0.0, tmpLambda = 0.0, val2 = 0.0;

        // get the length of the direction vector:
        sum = 0.0;
        for (int i=0; i<dim; i++)
            sum += dir[i]*dir[i];
        sum = Math.sqrt(sum);

        // rescale if we're trying to move too far:
        if (sum > maxStep)
            for (int i=0; i<dim; i++)
                dir[i] *= maxStep/sum;

        // make sure our direction has at least some component along
        // -grad
        slope = 0.0;
        for (int i=0; i<dim; i++)
            slope += dir[i]*grad[i];

        if (slope >= 0.0)
            return ret;

        test = 0.0;
        for (int i=0; i<dim; i++) {
            double temp = Math.abs(dir[i])/Math.max(Math.abs(oldPt[i]),1.0);
            if (temp > test)
                test=temp;
        }

        lambdaMin = MOVETOL/test;
        lambda = 1.0;
        int it = 0;
        while (it < MAX_ITER_LINEAR_SEARCH) {
            if (lambda < lambdaMin) {
                // the position change is too small.
                ret = 1;
                break;
            }

            for(int i=0; i<dim; i++)
                newPt[i]=oldPt[i]+lambda*dir[i];
            total_engy = getTotalEnergy(newPt);

            // we're converged on the function:
            if (total_engy-oldVal <= FUNCTOL*lambda*slope)
                return 0;

            // if we made it this far, we need to backtrack:
            // it's the first step:
            if (it == 0)
                tmpLambda = -slope / (2.0*(total_engy - oldVal - slope));
            else {
                double rhs1 = total_engy - oldVal - lambda*slope;
                double rhs2 = val2 - oldVal - lambda2*slope;
                double a = (rhs1/(lambda*lambda) - rhs2/(lambda2*lambda2))
                        /(lambda-lambda2);
                double b = (-lambda2*rhs1/(lambda*lambda)
                        + lambda*rhs2/(lambda2*lambda2))/(lambda-lambda2);
                if (a == 0.0)
                    tmpLambda = -slope/(2.0*b);
                else {
                    double disc = b*b-3*a*slope;
                    if (disc < 0.0)
                        tmpLambda = 0.5*lambda;
                    else if (b <= 0.0)
                        tmpLambda = (-b + Math.sqrt(disc))/(3.0*a);
                    else
                        tmpLambda = -slope/(b + Math.sqrt(disc));
                }

                if (tmpLambda > 0.5*lambda)
                    tmpLambda = 0.5*lambda;
            }

            lambda2 = lambda;
            val2 = total_engy;
            lambda = Math.max(tmpLambda, 0.1*lambda);
            ++it;
        }
        // nothing was done
        for(int i=0; i<dim; i++)
            newPt[i]=oldPt[i];
        return ret;
    }

    /**
     * Accumulates all the energy gradients.
     *  @return The gradient scale factor.
     */
    protected double updateGradient() {
        grad = new double[dim];

        for (EnergyTerm engy : energies)
            engy.getGradient(pos, grad);

        double maxGrad = -1e8;
        double gradScale = 0.1;
        for (int i=0; i<dim; i++) {
            grad[i] *= gradScale;
            if (grad[i] > maxGrad)
                maxGrad = grad[i];
        }

        if (maxGrad > 10.0) {
            while (maxGrad*gradScale > 10.0)
                gradScale *= 0.5;

            for (int i=0; i<dim;i++)
                grad[i] *= gradScale;
        }

        return gradScale;
    }

    /**
     * Gets the total energy of the molecule as the sum of the energy
     * terms.
     *  @param pos The positions array representing the atoms positions in
     *      space.
     *  @return The total force field energy.
     */
    public double getTotalEnergy(double[] pos) {
        double total = 0.0;
        for (EnergyTerm term : energies)
            total += term.getEnergy(pos);

        return total;
    }

    /**
     * Gets the total energy of the molecule as the sum of the energy
     * terms. This function passes the force fields `pos` array to
     * getTotalEnergy().
     *  @return The total force field energy.
     */
    public double getTotalEnergy() {
        return getTotalEnergy(this.pos);
    }

    public static void initialize(String tableSet) {
    	if (MMFF94.equals(tableSet))
	        ForceField.loadTable(MMFF94, Tables.newMMFF94());
    	if (MMFF94.equals(tableSet))
	        ForceField.loadTable(MMFF94S, Tables.newMMFF94s());
    }

    /**
     * Loads and registers a tables object with the ForceField class so it
     * can be used by new ForceField instances.
     *  @param name The string name used to identifiy the tables object.
     *  @param table The tables object.
     */
    public static synchronized void loadTable(String name, Tables table) {
        if (!tables.containsKey(name))
            tables.put(name, table);
    }

    /**
     * Returns a table given a table name.
     *  @param name The string name of a table.
     *  @return The tables object.
     */
    public static Tables table(String name) {
        return tables.get(name);
    }
}
