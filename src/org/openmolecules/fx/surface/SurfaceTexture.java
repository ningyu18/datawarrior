package org.openmolecules.fx.surface;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.util.SortedList;
import javafx.scene.image.Image;
import javafx.scene.shape.TriangleMesh;
import org.sunflow.math.Point3;

/**
 * Created by thomas on 05.04.16.
 */
public abstract class SurfaceTexture {
	protected TriangleMesh mMesh;
	protected Conformer mConformerFX,mConformerSunFlow;
	protected StereoMolecule mMol;
	protected Image mImage;
	protected SortedList<AtomWithXCoord> mSortedAtomsFX, mSortedAtomsSunFlow;
	private AtomWithXCoord mAtomWithXCoord;  // non thread save buffer
	private float mMaxVDWR;

	public SurfaceTexture(TriangleMesh mesh, Conformer conformer) {
		mMesh = mesh;
		mConformerFX = conformer;
		mMol = conformer.getMolecule();
		mMol.ensureHelperArrays(Molecule.cHelperNeighbours);

		mMaxVDWR = 0f;
		mAtomWithXCoord = new AtomWithXCoord(0, 0);
		mSortedAtomsFX = new SortedList<>();
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			float x = (float) mConformerFX.getX(atom);
			mSortedAtomsFX.add(new AtomWithXCoord(atom, x));
			float vdwr = VDWRadii.VDW_RADIUS[mMol.getAtomicNo(atom)];
			if (mMaxVDWR < vdwr)
				mMaxVDWR = vdwr;
		}
	}

	/** Reduce the influence of small weights, which also reduces somewhat the artefacts,
	 *  that arise from neighbor triangles with different sets of three atomicNos.
	 *  @param distance of point from surface atom radius (VDWR+mSurfaceSurplus); negative, if inside
	 */
	protected float calculateWeight(float distance) {
		return (float)Math.exp(-6.0 * Math.max(0, distance));
	}

	/**
	 * Creates a sorted atom list based on the given conformer's coordinates.
	 * This must be called once before calling getSurfaceColor().
	 * @param conformer
	 */
	public void initializeSurfaceColor(Conformer conformer) {
		mConformerSunFlow = conformer;
		mSortedAtomsSunFlow = new SortedList<>();
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			float x = (float)conformer.getX(atom);
			mSortedAtomsSunFlow.add(new AtomWithXCoord(atom, x));
		}
	}

	/**
	 * This calculates the surface color at a given point in space for ray-tracing.
	 * initializeSurfaceColor() must have been called once before calling this method.
	 * The implementing class must use mConformer2's coordinates, which is the one
	 * passed to initializeSurfaceColor().
	 * dependin
	 * @param p
	 * @return
	 */
	public abstract org.sunflow.image.Color getSurfaceColor(Point3 p);

	/**
	 * @param x
	 * @param sortedAtoms
	 * @return returns index into atom list sorted by x - VDW-radius
	 */
	public int getLowIndex(float x, SortedList<AtomWithXCoord> sortedAtoms) {
		// for sunflow we cannot use the the mAtomWithXCoord buffer, because it uses multiple threads
		AtomWithXCoord awxc = (sortedAtoms == mSortedAtomsFX) ?
				mAtomWithXCoord.setX(x - mMaxVDWR) : new AtomWithXCoord(0,x - mMaxVDWR);
		return sortedAtoms.getIndexAboveEqual(awxc);
	}

	/**
	 * @param x
	 * @param sortedAtoms
	 * @return returns index into atom list sorted by x - VDW-radius
	 */
	public int getHighIndex(float x, SortedList<AtomWithXCoord> sortedAtoms) {
		// for sunflow we cannot use the the mAtomWithXCoord buffer, because it uses multiple threads
		AtomWithXCoord awxc = (sortedAtoms == mSortedAtomsFX) ?
				mAtomWithXCoord.setX(x + mMaxVDWR) : new AtomWithXCoord(0,x + mMaxVDWR);
		return sortedAtoms.getIndexAboveEqual(awxc);
	}

	/**
	 * Converts list index of by x sorted atom list int atom index
	 * @param index
	 * @param sortedAtoms
	 * @return
	 */
	public int toAtom(int index, SortedList<AtomWithXCoord> sortedAtoms) {
		return index >= mMol.getAllAtoms() ? mMol.getAllAtoms() : sortedAtoms.get(index).atom;
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param limit maximum distance to calculate
	 * @param atom
	 * @param conformer
	 * @return if distance between given point and atom if it is smaller than limit; otherwise Float.MAX_VALUE
	 */
	public float distanceToAtom(float x, float y, float z, float limit, int atom, Conformer conformer) {
		Coordinates c = conformer.getCoordinates(atom);
		float dx = Math.abs((float)c.x - x);
		if (dx > limit)
			return Float.MAX_VALUE;
		float dy = Math.abs((float)c.y - y);
		if (dy > limit)
			return Float.MAX_VALUE;
		float dz = Math.abs((float)c.z - z);
		if (dz > limit)
			return Float.MAX_VALUE;

		float d = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);

		return (d < limit) ? d : Float.MAX_VALUE;
	}

	abstract public void applyToSurface();
	public Image getImage() {
		return mImage;
	}

	class AtomWithXCoord implements Comparable<AtomWithXCoord> {
		int atom;
		float x;

		public AtomWithXCoord(int atom, float x) {
			this.atom = atom;
			this.x = x;
		}

		public AtomWithXCoord setX(float x) {
			this.x = x;
			return this;
		}

		@Override
		public int compareTo(AtomWithXCoord o) {
			return x < o.x ? -1 : x > o.x ? 1 : atom < o.atom ? -1 : atom > o.atom ? 1 : 0;
		}
	}
}
