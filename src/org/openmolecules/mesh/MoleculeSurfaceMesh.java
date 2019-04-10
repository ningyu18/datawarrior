package org.openmolecules.mesh;

import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.VDWRadii;

/**
 * Created by thomas on 27.03.16.
 */
public class MoleculeSurfaceMesh extends SmoothMarchingCubesMesh implements VDWRadii {
	public static final String[] SURFACE_TYPE = { "Connolly", "Lee-Richards" };
	public static final int CONNOLLY = 0;
	public static final int LEE_RICHARDS = 1;

	public static final float DEFAULT_VOXEL_SIZE = 0.4f;	// angstrom
	public static final float DEFAULT_PROBE_SIZE = 1.4f;	// angstrom
	private static final float ISOLEVEL_VALUE = 5.0f; 		// >0 to avoid the array initialization with negative value
	// and not an integer to reduce exact matches if we read an integer array.
	private static final float RADIUS_SURPLUS = 1.0f;		// voxel edge lengths to consider beyond sphere radii

	/**
	 * Generate the molecule's Connolly or Lee-Richards surface mesh using an improved
	 * marching cubes algorithm that avoids small or skinny triangles.
	 * It uses a probe with 1.4 Angstrom radius and a voxel size of DEFAULT_VOXEL_SIZE in Angstrom.
	 * @param mol
	 * @param type CONNOLLY or LEE_RICHARDS
	 * @param meshBuilder
	 */
	public MoleculeSurfaceMesh(Conformer mol, int type, MeshBuilder meshBuilder) {
		this(mol, type, DEFAULT_PROBE_SIZE, DEFAULT_VOXEL_SIZE, meshBuilder);
	}

	/**
	 * Generate the molecule's Connolly or Lee-Richards surface mesh using an improved
	 * marching cubes algorithm that avoids small or skinny triangles.
	 * If probeSize!=0 then the solvent accessible surface (Conolly surface)
	 * is created using a sphere with the given size as probe.
	 * Otherwise the created surface is just the outer hull of all atom spheres
	 * using van-der-Waals radii.
	 * @param mol
	 * @param type CONNOLLY or LEE_RICHARDS
	 * @param probeSize radius of spherical solvent probe in Angstrom
	 * @param voxelSize size of internal voxels to track space occupation
	 * @param meshBuilder
	 */
	public MoleculeSurfaceMesh(Conformer mol, int type, float probeSize, float voxelSize, MeshBuilder meshBuilder) {
		super(meshBuilder, voxelSize);

		float xmin = Float.MAX_VALUE;
		float xmax = Float.MIN_VALUE;
		float ymin = Float.MAX_VALUE;
		float ymax = Float.MIN_VALUE;
		float zmin = Float.MAX_VALUE;
		float zmax = Float.MIN_VALUE;
		for (int atom=0; atom<mol.getMolecule().getAllAtoms(); atom++) {
			float r = VDW_RADIUS[mol.getMolecule().getAtomicNo(atom)];
			float x = (float)mol.getX(atom);
			float y = (float)mol.getY(atom);
			float z = (float)mol.getZ(atom);
			if (xmin > x - r)
				xmin = x - r;
			if (ymin > y - r)
				ymin = y - r;
			if (zmin > z - r)
				zmin = z - r;
			if (xmax < x + r)
				xmax = x + r;
			if (ymax < y + r)
				ymax = y + r;
			if (zmax < z + r)
				zmax = z + r;
		}

		float xsize = xmax - xmin + 2*probeSize;
		float ysize = ymax - ymin + 2*probeSize;
		float zsize = zmax - zmin + 2*probeSize;
		int sx = (int)(xsize/voxelSize+3f);	// 3f instead of 2f to avoid edge effects
		int sy = (int)(ysize/voxelSize+3f);
		int sz = (int)(zsize/voxelSize+3f);

		float offsetX = xmin - probeSize - ((sx-1)*voxelSize - xsize) / 2f;
		float offsetY = ymin - probeSize - ((sy-1)*voxelSize - ysize) / 2f;
		float offsetZ = zmin - probeSize - ((sz-1)*voxelSize - zsize) / 2f;

		// 3-dimensional voxel grid with values on voxel corners, where the voxels touch.
		// The coordinates within the voxel box range from 0 ... sx-1, (sy-1, sz-1)
		float[] grid = new float[sx*sy*sz];

		for (int atom=0; atom<mol.getMolecule().getAllAtoms(); atom++) {
			// translate atom coordinates to voxel space
			float x = ((float)mol.getX(atom) - offsetX) / voxelSize;
			float y = ((float)mol.getY(atom) - offsetY) / voxelSize;
			float z = ((float)mol.getZ(atom) - offsetZ) / voxelSize;

			// radius to consider for voxel updates
			float r = (probeSize + VDW_RADIUS[mol.getMolecule().getAtomicNo(atom)]) / voxelSize;
			int x1 = Math.max(0, (int)(x-r));
			int x2 = Math.min(sx-1, (int)(x+r+1));
			int y1 = Math.max(0, (int)(y-r));
			int y2 = Math.min(sy-1, (int)(y+r+1));
			int z1 = Math.max(0, (int)(z-r));
			int z2 = Math.min(sz-1, (int)(z+r+1));
			for (int xi=x1; xi<=x2; xi++) {
				for (int yi=y1; yi<=y2; yi++) {
					for (int zi=z1; zi<=z2; zi++) {
						int i = xi*sy*sz + yi*sz + zi;
						if (grid[i] < ISOLEVEL_VALUE + RADIUS_SURPLUS) {
							float dx = x - xi;
							float dy = y - yi;
							float dz = z - zi;
							float d = r - (float)Math.sqrt(dx*dx + dy*dy + dz*dz) + ISOLEVEL_VALUE;
							if (grid[i] < d)
								grid[i] = d;
						}
					}
				}
			}
		}

		if (type == CONNOLLY && probeSize != 0)
			grid = removeProbeAccessibleVolume(grid, sx, sy, sz, probeSize/voxelSize);

		setOffset(offsetX, offsetY, offsetZ);
		polygonise(grid, sx, sy, sz, ISOLEVEL_VALUE);
	}

	private float[] removeProbeAccessibleVolume(float[] inGrid, int sx, int sy, int sz, float r) {
		float[] outGrid = inGrid.clone();

		int i = 0;
		for (int ix=0; ix<sx-1; ix++) {
			for (int iy=0; iy<sy-1; iy++) {
				for (int iz=0; iz<sz-1; iz++) {
					boolean isSmaller = (inGrid[i] <= ISOLEVEL_VALUE);
					if (isSmaller ^ (inGrid[i+sy*sz] <= ISOLEVEL_VALUE)) {
						float pos = (ISOLEVEL_VALUE - inGrid[i]) / (inGrid[i+sy*sz] - inGrid[i]);
						removeProbeVolume(outGrid, r, ix+pos, iy, iz, sx, sy, sz);
					}
					if (isSmaller ^ (inGrid[i+sz] <= ISOLEVEL_VALUE)) {
						float pos = (ISOLEVEL_VALUE - inGrid[i]) / (inGrid[i+sz] - inGrid[i]);
						removeProbeVolume(outGrid, r, ix, iy+pos, iz, sx, sy, sz);
					}
					if (isSmaller ^ (inGrid[i+1] <= ISOLEVEL_VALUE)) {
						float pos = (ISOLEVEL_VALUE - inGrid[i]) / (inGrid[i+1] - inGrid[i]);
						removeProbeVolume(outGrid, r, ix, iy, iz+pos, sx, sy, sz);
					}

					i++;
				}
				i++;
			}
			i+= sz;
		}

		return outGrid;
	}

	private void removeProbeVolume(float[] outGrid, float r, float x, float y, float z, int sx, int sy, int sz) {
		int x1 = Math.max(0, (int)(x-r));
		int x2 = Math.min(sx-1, (int)(x+r+1));
		int y1 = Math.max(0, (int)(y-r));
		int y2 = Math.min(sy-1, (int)(y+r+1));
		int z1 = Math.max(0, (int)(z-r));
		int z2 = Math.min(sz-1, (int)(z+r+1));
		for (int xi=x1; xi<=x2; xi++) {
			for (int yi=y1; yi<=y2; yi++) {
				for (int zi=z1; zi<=z2; zi++) {
					int i = xi*sy*sz + yi*sz + zi;
					if (outGrid[i] > ISOLEVEL_VALUE - RADIUS_SURPLUS) {
						float dx = x - xi;
						float dy = y - yi;
						float dz = z - zi;
						float d = (float)Math.sqrt(dx*dx + dy*dy + dz*dz) - r + ISOLEVEL_VALUE;
						if (outGrid[i] > d)
							outGrid[i] = d;
					}
				}
			}
		}
	}
}
