package com.actelion.research.chem.conf;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.util.Angle;

public class TorsionDescriptor {
	private static final float TORSION_EQUIVALENCE_TOLERANCE = (float)Math.PI / 12f;

	private float[] mTorsion;

	/**
	 * Calculates an array of all rotatable bonds that can be used
	 * multiple times as parameter to calculateDescriptor().
	 * If the molecule contains marked atoms, these are not considered
	 * part of the molecule, when detecting rotatable bonds. Any non-aromatic
	 * single bond with at least one non-H, non-marked neighbor to either side
	 * is considered a rotatable bond, if none of the bond atoms is marked.
	 * @param mol the molecule behind multiple conformers
	 */
	public static int[] getRotatableBonds(StereoMolecule mol) {
		int count = 0;
		for (int bond=0; bond<mol.getBonds(); bond++)
			if (qualifiesAsDescriptorBond(mol, bond))
				count++;
		int[] rotatableBond = new int[count];
		count = 0;
		for (int bond=0; bond<mol.getBonds(); bond++)
			if (qualifiesAsDescriptorBond(mol, bond))
				rotatableBond[count++] = bond;
		return rotatableBond;
		}

	private static boolean qualifiesAsDescriptorBond(StereoMolecule mol, int bond) {
		if (mol.getBondOrder(bond) != 1 || mol.isAromaticBond(bond))
			return false;

		for (int i=0; i<2; i++) {
			int bondAtom = mol.getBondAtom(i, bond);
			if (mol.isMarkedAtom(bondAtom))
				return false;
			int connAtoms = mol.getConnAtoms(bondAtom);
			if (connAtoms == 1)
				return false;
			int qualifiedConnAtoms = 0;
			for (int j=0; j<connAtoms; j++) {
				int connAtom = mol.getConnAtom(bondAtom, j);
				if (!mol.isMarkedAtom(connAtom))
					qualifiedConnAtoms++;
				}
			if (qualifiedConnAtoms < 2)
				return false;
			}

		return true;
		}

	/**
	 * Creates a TorsionDescriptor the coordinates of the passed molecule.
	 * The torsion descriptor is not canonical, unless the passed molecule is canonical.
	 * Rotatable bonds need to carry at least one external non-hydrogen neighbor on each side.
	 * @param mol
	 * @param rotatableBond those bonds considered to be rotatable
	 */
	public TorsionDescriptor(StereoMolecule mol, int[] rotatableBond) {
		mTorsion = new float[rotatableBond.length];
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		int[] atom = new int[4];
		for (int i=0; i<rotatableBond.length; i++) {
			atom[1] = mol.getBondAtom(0, rotatableBond[i]);
			atom[2] = mol.getBondAtom(1, rotatableBond[i]);
			atom[0] = mol.getConnAtom(atom[1], 0);
			atom[3] = mol.getConnAtom(atom[2], 0);
			mTorsion[i] = (float)TorsionDB.calculateTorsion(mol, atom);
			}
		}

	/**
	 * Creates a TorsionDescriptor from conformer's coordinates
	 * drawing atom connectivity from the conformer's molecule.
	 * The torsion descriptor is not canonical, unless the passed molecule is canonical.
	 * Rotatable bonds need to carry at least one external non-hydrogen neighbor on each side.
	 * @param conformer
	 */
	public TorsionDescriptor(Conformer conformer, int[] rotatableBond) {
		mTorsion = new float[rotatableBond.length];
		StereoMolecule mol = conformer.getMolecule();
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		int[] atom = new int[4];
		for (int i=0; i<rotatableBond.length; i++) {
			for (int j=0; j<2; j++) {
				atom[j+1] = mol.getBondAtom(j, rotatableBond[i]);
				for (int k=0; k<mol.getConnAtoms(atom[j+1]); k++) {
					int refAtom = mol.getConnAtom(atom[j+1], k);
					if (refAtom != mol.getBondAtom(1-j, rotatableBond[i])) {
						atom[3*j] = refAtom;
						break;
						}
					}
				}
			mTorsion[i] = (float)TorsionDB.calculateTorsion(conformer, atom);
			}
		}

	/**
	 * Returns true, if none of the torsion angles are more different
	 * than TORSION_EQUIVALENCE_TOLERANCE;
	 * @param td
	 * @return
	 */
	public boolean equals(TorsionDescriptor td) {
		for (int i=0; i<mTorsion.length; i++) {
			float dif = Math.abs(mTorsion[i] - td.mTorsion[i]);
			if (dif > TORSION_EQUIVALENCE_TOLERANCE
			 && dif < 2*(float)Math.PI - TORSION_EQUIVALENCE_TOLERANCE)
				return false;
			}
		return true;
		}

	/**
	 * Calculates the weight for all rotatable bonds from the position in the molecule.
	 * Central bonds have weights close to 1.0, while almost terminal bonds have weights
	 * close to 0.0. Ring bonds get a weight of 0.33 independent of their location.
	 * @param mol
	 * @param rotatableBond
	 * @return
	 */
	public static float[] getRotatableBondWeights(StereoMolecule mol, int[] rotatableBond) {
		float[] bondWeight = new float[rotatableBond.length];
		for (int i=0; i<bondWeight.length; i++) {
			if (mol.isRingBond(rotatableBond[i])) {
				bondWeight[i] = 0.33f;
				}
			else {
				int atom1 = mol.getBondAtom(0, rotatableBond[i]);
				int atom2 = mol.getBondAtom(1, rotatableBond[i]);
				if (mol.getConnAtoms(atom1) == 1 || mol.getConnAtoms(atom2) == 1) {
					bondWeight[i] = (float)Math.sqrt(2f / mol.getAtoms());
					}
				else {
					int atomCount = mol.getSubstituentSize(atom1, atom2) - 1;
					bondWeight[i] = (float)Math.sqrt(2.0 * Math.min(atomCount, mol.getAtoms() - atomCount) / mol.getAtoms());
					}
				}
			}
		return bondWeight;
		}

	/**
	 * Calculates a similarity value between td and this considering
	 * individual torsion values, the importance of the rotatable bond,
	 * and the ratio of rotatable/non-rotatable bonds.
	 * @param td
	 * @return
	 */
	public float getDissimilarity(TorsionDescriptor td, float[] bondWeight) {
		assert(mTorsion.length == td.mTorsion.length);

		if (mTorsion.length == 0)
			return 0.0f;

		float meanAngleDiff = 0f;

		float weightSum = 0f;
		for (int i=0; i<mTorsion.length; i++) {
			meanAngleDiff += bondWeight[i] * Math.abs(Angle.difference(mTorsion[i], td.mTorsion[i]));
			weightSum += bondWeight[i];
			}
		return meanAngleDiff / ((float)Math.PI * weightSum);
		}
	}
