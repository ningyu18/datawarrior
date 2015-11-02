package org.openmolecules.render;
import java.awt.Color;
import java.util.Arrays;

import org.sunflow.SunflowAPI;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

import com.actelion.research.calc.SingularValueDecomposition;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.VDWRadii;

public class MoleculeRenderer extends SunflowAPIAPI {
	public static final float DEFAULT_CAMERA_DISTANCE = 12.5f;
	public static final float DEFAULT_CAMERA_X = 0f;
	public static final float DEFAULT_CAMERA_Z = 0.001f;	// exactly 0f rotates the camera for some reason...
	public static final float DEFAULT_FIELD_OF_VIEW = 40f;

	private long	mRandomSeed;
	private float	mCameraDistance,mCameraX, mCameraZ,mFieldOfView;

	public final static int[] ARGbsCpk = {
	    0xFFFF1493, 0xFFFFFFFF, 0xFFD9FFFF, 0xFFCC80FF, 0xFFC2FF00, 0xFFFFB5B5, //  ?, H,He,Li, Be,B
	    0xFF909090, 0xFF3050F8, 0xFFFF0D0D, 0xFF90E050, 0xFFB3E3F5, 0xFFAB5CF2, //  C, N, O, F,Ne,Na
	    0xFF8AFF00, 0xFFBFA6A6, 0xFFF0C8A0, 0xFFFF8000, 0xFFFFFF30, 0xFF1FF01F, // Mg,Al,Si, P, S,Cl
	    0xFF80D1E3, 0xFF8F40D4, 0xFF3DFF00, 0xFFE6E6E6, 0xFFBFC2C7, 0xFFA6A6AB, // Ar, K,Ca,Sc,Ti, V
	    0xFF8A99C7, 0xFF9C7AC7, 0xFFE06633, 0xFFF090A0, 0xFF50D050, 0xFFC88033, // Cr,Mn,Fe,Co,Ni,Cu
	    0xFF7D80B0, 0xFFC28F8F, 0xFF668F8F, 0xFFBD80E3, 0xFFFFA100, 0xFFA62929, // Zn,Ga,Ge,As,Se,Br
	    0xFF5CB8D1, 0xFF702EB0, 0xFF00FF00, 0xFF94FFFF, 0xFF94E0E0, 0xFF73C2C9, // Kr,Rb,Sr, Y,Zr,Nb
	    0xFF54B5B5, 0xFF3B9E9E, 0xFF248F8F, 0xFF0A7D8C, 0xFF006985, 0xFFC0C0C0, // Mo,Tc,Ru,Rh,Pd,Ag
	    0xFFFFD98F, 0xFFA67573, 0xFF668080, 0xFF9E63B5, 0xFFD47A00, 0xFF940094, // Cd,In,Sn,Sb,Te, I
	    0xFF429EB0, 0xFF57178F, 0xFF00C900, 0xFF70D4FF, 0xFFFFFFC7, 0xFFD9FFC7, // Xe,Cs,Ba,La,Ce,Pr
	    0xFFC7FFC7, 0xFFA3FFC7, 0xFF8FFFC7, 0xFF61FFC7, 0xFF45FFC7, 0xFF30FFC7, // Nd,Pm,Sm,Eu,Gd,Tb
	    0xFF1FFFC7, 0xFF00FF9C, 0xFF00E675, 0xFF00D452, 0xFF00BF38, 0xFF00AB24, // Dy,Ho,Er,Tm,Yb,Lu
	    0xFF4DC2FF, 0xFF4DA6FF, 0xFF2194D6, 0xFF267DAB, 0xFF266696, 0xFF175487, // Hf,Ta, W,Re,Os,Ir
	    0xFFD0D0E0, 0xFFFFD123, 0xFFB8B8D0, 0xFFA6544D, 0xFF575961, 0xFF9E4FB5, // Pt,Au,Hg,Tl,Pb,Bi
	    0xFFAB5C00, 0xFF754F45, 0xFF428296, 0xFF420066, 0xFF007D00, 0xFF70ABFA, // Po,At,Rn,Fr,Ra,Ac
	    0xFF00BAFF, 0xFF00A1FF, 0xFF008FFF, 0xFF0080FF, 0xFF006BFF, 0xFF545CF2, // Th,Pa, U,Np,Pu,Am
	    0xFF785CE3, 0xFF8A4FE3, 0xFFA136D4, 0xFFB31FD4, 0xFFB31FBA, 0xFFB30DA6, // Cm,Bk,Cf,Es,Fm,Md
	    0xFFBD0D87, 0xFFC70066, 0xFFCC0059, 0xFFD1004F, 0xFFD90045, 0xFFE00038, // No,Lr,Rf,Db,Sg,Bh
	    0xFFE6002E, 0xFFEB0026,													// Hs,Mt
		};

	public MoleculeRenderer() {
		this(DEFAULT_CAMERA_DISTANCE, DEFAULT_CAMERA_X, DEFAULT_CAMERA_Z, DEFAULT_FIELD_OF_VIEW);
		}

	public MoleculeRenderer(float cameraDistance, float fieldOfView) {
		this(cameraDistance, DEFAULT_CAMERA_X, DEFAULT_CAMERA_Z, fieldOfView);
		}

	public MoleculeRenderer(float cameraDistance, float cameraX, float cameraZ, float fieldOfView) {
		mCameraDistance = cameraDistance;
		mCameraX = cameraX;
		mCameraZ = cameraZ;
		mFieldOfView = fieldOfView;
		}

	//	public void setRandomSeed(long seed) {
	//	mRandomSeed = seed;
		//}

	public void drawMolecule(StereoMolecule mol, int width, int height, boolean rotateToOptimum, boolean moveAndZoomToOptimum, boolean blackBackground) {
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		setWidth(width);
		setHeight(height);

//		setPathTracingGIEngine(64);
//		setAmbientOcclusionEngine(Color.LIGHT_GRAY, Color.DARK_GRAY, 64, 1f);

/*		caustics needs a photon source, e.g. a mesh light (sun light doesn't suffice)
	    getAPI().parameter("caustics.emit", 10000000);
	    getAPI().parameter("caustics", "kd");
	    getAPI().parameter("caustics.gather", 64);
	    getAPI().parameter("caustics.radius", 0.5f);	*/

        getAPI().parameter("bucket.size", 64);

		getAPI().options(SunflowAPI.DEFAULT_OPTIONS);	// done to process above parameters

	    setCameraPosition(mCameraX, -mCameraDistance, mCameraZ);
		setCameraTarget(0,0,0);

        setThinlensCamera("thinLensCamera", mFieldOfView, (float)width/height);

/*        // construct a sun light
        getAPI().parameter( "up", new Vector3( 0, 0, 1 ) ); 
		getAPI().parameter( "east", new Vector3( 0, 1, 0 ) ); 
		getAPI().parameter( "sundir", new Vector3( 1, -1, 0.31f ) ); 
		getAPI().parameter( "turbidity", 2f ); 
		getAPI().parameter( "samples", 16 );
        getAPI().parameter( "ground.color", COLORSPACE_SRGB_NONLINEAR, 0.3f, 0.3f, 0.3f);
        getAPI().parameter( "ground.extendsky", true);
		getAPI().light("mySun", LIGHT_SUNSKY );*/

        setSunSkyLight("sun");

//	    setDirectionalLight("light", new Point3(-10f, 5f, 10f), new Vector3(10f, -5f, -10f), 1f, Color.white, 100f);
//		setPointLight("pointLight", new Point3(-10f, 5f, 10f),Color.WHITE, 100000f);
//		setSphereLight("sphereLight", new Point3(-10f, 5f, 10f), Color.white, 1000f, 16, 2f);
//		createMeshLight();
        
	    Point3[] coords = new Point3[mol.getAllAtoms()];
		for (int atom=0; atom<mol.getAllAtoms(); atom++)
			coords[atom] = new Point3(mol.getAtomX(atom), mol.getAtomY(atom), mol.getAtomZ(atom));

		if (rotateToOptimum) {
			rotateIntoView(coords);
			flipFeaturesToFront(mol, coords);
			}

		if (moveAndZoomToOptimum)
			scaleAndCenterForCamera(mol, coords, width, height, rotateToOptimum);

		if (blackBackground) {
			setDiffuseShader("blackBackgroundShader", new Color(0f, 0f, 0f));
			drawPlane("blackBackground", new Point3(0f, 100f, 0f), new Vector3(0f, 1f, 0f));
			}
		else {
			float zmin = coords[0].z - getAtomRadius(mol, 0);
			for (int atom=1; atom<mol.getAllAtoms(); atom++)
				if (zmin > coords[atom].z - getAtomRadius(mol, atom))
					zmin = coords[atom].z - getAtomRadius(mol, atom);
	
			setDiffuseShader("groundShader", new Color(0.8f, 0.8f, 0.8f));
			drawPlane("ground", new Point3(0f, 0f, zmin-0.01f), new Vector3(0f, 0f, 1f));
			}

		setDiffuseShader("bondShader", new Color(1.0f, 1.0f, 1.0f));
		for (int bond=0; bond<mol.getAllBonds(); bond++)
			createBond(mol, bond, coords);

		int[] atomType = new int[mol.getAllAtoms()];
		for (int i=0; i<mol.getAllAtoms(); i++)
			atomType[i] = (mol.getAtomicNo(i) << 20) + i;
		Arrays.sort(atomType);
		int previousAtomicNo = -1;
		for (int i=0; i<mol.getAllAtoms(); i++) {
			int atomicNo = (atomType[i] >> 20);
			if (previousAtomicNo != atomicNo) {
				previousAtomicNo = atomicNo;

				if (blackBackground) {
					Color color = (atomicNo == 1) ? new Color(0.6f, 0.6f, 0.6f)
					: (atomicNo == 6) ? new Color(0.2f, 0.2f, 0.2f) : new Color(ARGbsCpk[atomicNo]).darker();
					setShinyDiffuseShader("shaderAtom"+atomicNo, color, 0.6f);
					}
				else {
					Color color = (atomicNo == 1) ? new Color(0.95f, 0.95f, 1.0f)
					: (atomicNo == 6) ? new Color(0.4f, 0.4f, 0.4f) : new Color(ARGbsCpk[atomicNo]);
					setGlassShader("shaderAtom"+atomicNo, color, 2.1f);
					}
				}

			int atom = atomType[i] & 0x000FFFFF;
			float radius = getAtomRadius(mol, atom);
			createAtom(atom, coords[atom], radius);
			}
		}

	private void createMeshLight() {
		float[] MESH1_POINTS = {-1.79750967026f, -6.22097349167f, 5.70054674149f,
								-2.28231739998f, -7.26064729691f, 4.06224298477f,
								-4.09493303299f, -6.41541051865f, 4.06224298477f,
								-3.61012482643f, -5.37573671341f, 5.70054721832f };
		int[] MESH1_TRIANGLES = {0, 1, 2, 0, 2, 3};
		
		float[] MESH2_POINTS = {-4.25819396973f, -4.8784570694f, 5.70054674149f,
								-5.13696432114f, -5.61583280563f, 4.06224298477f,
								-6.422539711f, -4.08374404907f, 4.06224298477f,
								-5.54376888275f, -3.34636831284f, 5.70054721832f };
		int[] MESH2_TRIANGLES = {0, 1, 2, 0, 2, 3};

		drawMeshLight("meshlight1", new Color(255, 255, 255), 15f, 8, MESH1_POINTS, MESH1_TRIANGLES);
		drawMeshLight("meshlight2", new Color(255, 255, 255), 15f, 8, MESH2_POINTS, MESH2_TRIANGLES);
		}

	private void scaleAndCenterForCamera(StereoMolecule mol, Point3[] coords, int width, int height, boolean optimizePerspective) {
		// calculate simple size in three dimensions
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float minZ = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		float maxZ = Float.NEGATIVE_INFINITY;
		for (int i=0; i<coords.length; i++) {
			if (minX > coords[i].x)
				minX = coords[i].x;
			if (maxX < coords[i].x)
				maxX = coords[i].x;
			if (minY > coords[i].y)
				minY = coords[i].y;
			if (maxY < coords[i].y)
				maxY = coords[i].y;
			if (minZ > coords[i].z)
				minZ = coords[i].z;
			if (maxZ < coords[i].z)
				maxZ = coords[i].z;
			}

		if (optimizePerspective) {
			// rotate if width < height
			if (maxZ - minZ > maxX - minX) {
				float temp = minX;
				minX = minZ;
				minZ = -maxX;
				maxX = maxZ;
				maxZ = -temp;
				for (int i=0; i<coords.length; i++) {
					temp = -coords[i].x;
					coords[i].x = coords[i].z;
					coords[i].z = temp;
					}
				}
			}

		// center on all axes
		float xshift = (minX + maxX) / 2f;
		float yshift = (minY + maxY) / 2f;
		float zshift = (minZ + maxZ) / 2f;
		for (int i=0; i<coords.length; i++) {
			coords[i].x -= xshift;
			coords[i].y -= yshift;
			coords[i].z -= zshift;
			}

		// Stepwise optimize molecule location concerning x-,y- and z-axis
		// to center and scale molecule in camera perspective.
		xshift = 0f;
		yshift = 0f;
		zshift = 0f;
		float dy = maxY - minY;
		float maxPerspectiveX = 0.68f;
		while(true) {
			minX = Float.POSITIVE_INFINITY;
			minZ = Float.POSITIVE_INFINITY;
			maxX = Float.NEGATIVE_INFINITY;
			maxZ = Float.NEGATIVE_INFINITY;
			for (int i=0; i<coords.length; i++) {
				float cameraDistance = mCameraDistance + coords[i].y + yshift;
				float x = (coords[i].x+xshift) / cameraDistance;
				float z = (coords[i].z+zshift) / cameraDistance;
				float r = getAtomRadius(mol, i) / cameraDistance;
				if (minX > x-r)
					minX = x-r;
				if (maxX < x+r)
					maxX = x+r;
				if (minZ > z-r)
					minZ = z-r;
				if (maxZ < z+r)
					maxZ = z+r;
				}
	
			float dx = maxX - minX;
			float dz = maxZ - minZ;
			float factor = Math.min(maxPerspectiveX / dx, maxPerspectiveX * height / (dz * width));
			if (Math.abs(1f-factor) < 0.01
			 && Math.abs((minX+maxX) / dx) < 0.02
			 && Math.abs((minZ+maxZ) / dz) < 0.02)
				break;

			xshift -= (minX + maxX) * (mCameraDistance + yshift - dy/2) / 2f;
			zshift -= (minZ + maxZ) * (mCameraDistance + yshift - dy/2) / 2f;
			yshift += (mCameraDistance + yshift - dy/2) * (1f/factor - 1f);
			}

		for (int i=0; i<coords.length; i++) {
			coords[i].x += xshift;
			coords[i].y += yshift;
			coords[i].z += zshift;
			}
		}

	public void rotateIntoView(Point3[] coords) {
		float[] cog = new float[3];	// center of gravity
		for (int i=0; i<coords.length; i++) {
			cog[0] += coords[i].x;
			cog[1] += coords[i].y;
			cog[2] += coords[i].z;
			}
		for (int j=0; j<3; j++)
			cog[j] /= coords.length;

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<coords.length; i++) {
			coords[i].x -= cog[0];
			coords[i].y -= cog[1];
			coords[i].z -= cog[2];
			squareMatrix[0][0] += coords[i].x * coords[i].x;
			squareMatrix[0][1] += coords[i].x * coords[i].y;
			squareMatrix[0][2] += coords[i].x * coords[i].z;
			squareMatrix[1][0] += coords[i].y * coords[i].x;
			squareMatrix[1][1] += coords[i].y * coords[i].y;
			squareMatrix[1][2] += coords[i].y * coords[i].z;
			squareMatrix[2][0] += coords[i].z * coords[i].x;
			squareMatrix[2][1] += coords[i].z * coords[i].y;
			squareMatrix[2][2] += coords[i].z * coords[i].z;
			}

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		double[][] eigenVectorsLeft = svd.getU();

		for (int i=0; i<coords.length; i++) {
			float[] f = new float[3];
			for (int j=0; j<3; j++) {
				f[j] += coords[i].x * eigenVectorsLeft[0][j];
				f[j] += coords[i].y * eigenVectorsLeft[1][j];
				f[j] += coords[i].z * eigenVectorsLeft[2][j];
				}
			coords[i].x =  (float)f[0];
			coords[i].z =  (float)f[1];
			coords[i].y = -(float)f[2];
			}
		}

	private void flipFeaturesToFront(StereoMolecule mol, Point3[] coords) {
		float carbonMeanY = 0f;
		float heteroMeanY = 0f;
		int carbonCount = 0;
		int heteroCount = 0;
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (mol.getAtomicNo(atom) == 6) {
				carbonMeanY += coords[atom].y;
				carbonCount++;
				}
			else {
				heteroMeanY += coords[atom].y;
				heteroCount++;
				}
			}
		if (heteroMeanY / heteroCount < carbonMeanY / carbonCount) {
			for (int i=0; i<coords.length; i++) {
				coords[i].y *= -1;
				coords[i].z *= -1;
				}
			}
		}

	private void createAtom(int atom, Point3 a, float radius) {
	    drawSphere("atom"+atom, a.x, a.y, a.z, radius);
		}

	private float getAtomRadius(StereoMolecule mol, int atom) {
		return VDWRadii.VDW_RADIUS[mol.getAtomicNo(atom)] / 4f;
		}

	private void createBond(StereoMolecule mol, int bond, Point3[] coords) {
		int atom1 = mol.getBondAtom(0, bond);
		int atom2 = mol.getBondAtom(1, bond);
		float x = (coords[atom1].x+coords[atom2].x)/2f;
		float y = (coords[atom1].y+coords[atom2].y)/2f;
		float z = (coords[atom1].z+coords[atom2].z)/2f;
		float dx = coords[atom2].x-coords[atom1].x;
		float dy = coords[atom2].y-coords[atom1].y;
		float dz = coords[atom2].z-coords[atom1].z;
		float d = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
		float dxy = (float)Math.sqrt(dx*dx+dy*dy);
		float b = (float)Math.asin(coords[atom2].z>coords[atom1].z ? dxy/d : -dxy/d);
		float c = (dx < 0f) ? (float)Math.atan(dy/dx)+(float)Math.PI
				: (dx > 0f) ? (float)Math.atan(dy/dx)
				: (dy>0f) ? (float)Math.PI/2 : -(float)Math.PI/2;

		int order = mol.getBondOrder(bond);
		if (order == 1) {
			float dd = calculateBondReduction(mol, bond, 0.2f);
			if (dd != 0f)
				drawCylinder("bond"+bond, 0.2f, 0.2f, d/2-dd, x, y, z, 0f, b, c);
			return;
			}

		if (order == 2) {
			Vector3 ds = calculateDoubleBondShift(mol, bond, coords, 0.20f);
			float dd = calculateBondReduction(mol, bond, 0.20f+0.12f);
			if (dd != 0f) {
				drawCylinder("bond"+bond+"a", 0.12f, 0.12f, d/2-dd, x-ds.x, y-ds.y, z-ds.z, 0f, b, c);
				drawCylinder("bond"+bond+"b", 0.12f, 0.12f, d/2-dd, x+ds.x, y+ds.y, z+ds.z, 0f, b, c);
				}
			return;
			}

		if (order == 3) {
			Vector3 ds = calculateRandomOrthogonalShift(mol, bond, coords, 0.24f);
			float dd1 = calculateBondReduction(mol, bond, 0.12f);
			float dd2 = calculateBondReduction(mol, bond, 0.24f+0.08f);
			if (dd2 != 0f)
				drawCylinder("bond"+bond+"a", 0.08f, 0.08f, d/2-dd2, x-ds.x, y-ds.y, z-ds.z, 0f, b, c);
			if (dd1 != 0f)
				drawCylinder("bond"+bond+"b", 0.12f, 0.12f, d/2-dd1, x, y, z, 0f, b, c);
			if (dd2 != 0f)
				drawCylinder("bond"+bond+"c", 0.08f, 0.08f, d/2-dd2, x+ds.x, y+ds.y, z+ds.z, 0f, b, c);
			return;
			}
		}

	private Vector3 calculateDoubleBondShift(StereoMolecule mol, int bond, Point3[] coords, float length) {
		for (int i=0; i<2; i++) {
			int atom = mol.getBondAtom(i, bond);
			int otherAtom = mol.getBondAtom(1-i, bond);
			for (int j=0; j<mol.getConnAtoms(atom); j++) {
				int connAtom = mol.getConnAtom(atom, j);
				if (connAtom != otherAtom) {
					Vector3 vb = createUnitVector(coords[atom], coords[otherAtom]);
					Vector3 vc = createUnitVector(coords[atom], coords[connAtom]);
					vb.mul(-Vector3.dot(vb, vc));
					return Vector3.add(vc, vb, new Vector3()).normalize().mul(length);
					}
				}
			}
		return calculateRandomOrthogonalShift(mol, bond, coords, length);
		}

	private Vector3 calculateRandomOrthogonalShift(StereoMolecule mol, int bond, Point3[] coords, float length) {
		Vector3 v = createUnitVector(coords[mol.getBondAtom(0, bond)], coords[mol.getBondAtom(1, bond)]);
		float lxy = (float)Math.sqrt(v.x*v.x+v.y*v.y);
		return (lxy == 0f) ? new Vector3(length, 0f, 0f) : new Vector3(-v.x*v.z/lxy, -v.y*v.z/lxy, lxy).mul(length);
		}

	private float calculateBondReduction(StereoMolecule mol, int bond, float sideShift) {
		float atomRadius = Math.min(getAtomRadius(mol, mol.getBondAtom(0, bond)),
									getAtomRadius(mol, mol.getBondAtom(1, bond)));
		return (sideShift >= atomRadius) ? 0f
				: (float)Math.sqrt(atomRadius*atomRadius - sideShift*sideShift);
		}

	private Vector3 createUnitVector(Point3 p1, Point3 p2) {
		Vector3 v = new Vector3(p2.x-p1.x, p2.y-p1.y, p2.z-p1.z);
		return v.normalize();
		}
	}
