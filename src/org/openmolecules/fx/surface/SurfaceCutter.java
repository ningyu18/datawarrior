package org.openmolecules.fx.surface;

import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;

import java.util.TreeMap;

import static org.openmolecules.fx.surface.SurfaceMesh.USE_NORMALS;

/**
 * Created by thomas on 23.04.16.
 */
public abstract class SurfaceCutter {
	protected abstract boolean pointIsOutside(float x, float y, float z);

	/**
	 * Determines the point of the cut between an inside and an outside point
	 * and adds(!) the new location to the three dimensions of location
	 * @param xi
	 * @param yi
	 * @param zi
	 * @param xo
	 * @param yo
	 * @param zo
	 * @param location buffer to which the three coordinates are added
	 */
	protected abstract void addCutPosition(float xi, float yi, float zi, float xo, float yo, float zo, float[] location);

	/**
	 * Removes all triangles and points from the mesh, which are outside the specified volume
	 *
	 * @return 0, 1, or 2 depending on whether no, a fraction, or the entire surface was removed
	 */
	public int cut(SurfaceMesh mesh) {
		ObservableFloatArray points = mesh.getPoints();
		ObservableIntegerArray faces = mesh.getFaces();
		int pointCount = points.size() / 3;

		boolean[] isOutside = new boolean[pointCount];
		int outsidePointCount = 0;
		for (int i = 0; i < pointCount; i++) {
			isOutside[i] = pointIsOutside(points.get(3 * i), points.get(3 * i + 1), points.get(3 * i + 2));
			if (isOutside[i])
				outsidePointCount++;
		}

		if (outsidePointCount == 0)
			return 0;

		if (outsidePointCount == pointCount) {
			faces.clear();
			points.clear();
			mesh.getTexCoords().clear();
			return 2;
		}

		TreeMap<Integer, EdgeLocation> translationMap = new TreeMap<>();
		int facePointSize = USE_NORMALS ? 3 : 2;
		int faceSize = 3 * facePointSize;
		for (int i1 = 0; i1 < faces.size(); i1 += faceSize) {
			int i2 = i1 + facePointSize;
			int i3 = i2 + facePointSize;
			int p1 = faces.get(i1);
			int p2 = faces.get(i2);
			int p3 = faces.get(i3);

			int outsideCount = 0;
			if (isOutside[p1]) outsideCount++;
			if (isOutside[p2]) outsideCount++;
			if (isOutside[p3]) outsideCount++;

			// triangle is partially outside and partially inside clip region
			if (outsideCount != 0 && outsideCount != 3) {
				// Collect first layer of outside points in translationMap
				// and calculate reasonable location for them directly on the clipping edge.
				if (isOutside[p1]) {
					if (!isOutside[p2])
						addTranslation(p2, p1, points, translationMap);
					if (!isOutside[p3])
						addTranslation(p3, p1, points, translationMap);
				}
				if (isOutside[p2]) {
					if (!isOutside[p1])
						addTranslation(p1, p2, points, translationMap);
					if (!isOutside[p3])
						addTranslation(p3, p2, points, translationMap);
				}
				if (isOutside[p3]) {
					if (!isOutside[p1])
						addTranslation(p1, p3, points, translationMap);
					if (!isOutside[p2])
						addTranslation(p2, p3, points, translationMap);
				}
			}
		}

		// write mean of translated outside point coordinates to points array
		for (int po:translationMap.keySet()) {
			EdgeLocation pt = translationMap.get(po);
			for (int i=0; i<3; i++)
				points.set(3*po+i, pt.coord[i]/pt.count);
		}

		// create new point indexes for all points inside or on clipping edge
		int[] newPointIndex = new int[pointCount];
		int newPointCount = 0;
		for (int i = 0; i < pointCount; i++) {
			if (isOutside[i] && translationMap.get(i) == null)
				newPointIndex[i] = -1;
			else
				newPointIndex[i] = newPointCount++;
		}

		//
		int newFaceCount = 0;
		for (int i1 = 0; i1 < faces.size(); i1 += faceSize) {
			int i2 = i1 + facePointSize;
			int i3 = i2 + facePointSize;
			int p1 = faces.get(i1);
			int p2 = faces.get(i2);
			int p3 = faces.get(i3);

			int outsideCount = 0;
			if (isOutside[p1]) outsideCount++;
			if (isOutside[p2]) outsideCount++;
			if (isOutside[p3]) outsideCount++;

			// Relocate all triangles that have their points inside or on the clipping edge
			// and skip those that are entirely outside.
			if (outsideCount != 3) {
				faces.set(i1, newPointIndex[p1]);
				faces.set(i2, newPointIndex[p2]);
				faces.set(i3, newPointIndex[p3]);
				faces.set(newFaceCount * faceSize, faces, i1, faceSize);
				newFaceCount++;
			}
		}
		faces.resize(newFaceCount * faceSize);
		faces.trimToSize();

		// reposition remaining points and resize array
		int destIndex = 0;
		for (int i=0; i<points.size(); i+=3) {
			if (newPointIndex[i/3] != -1) {
				points.set(destIndex, points, i, 3);
				destIndex += 3;
			}
		}
		points.resize(destIndex);
		points.trimToSize();
		return 1;
	}

	private void addTranslation(int pi, int po, ObservableFloatArray points, TreeMap<Integer,EdgeLocation> edgeLocationMap) {
		EdgeLocation location = edgeLocationMap.get(po);
		if (location == null) {
			location = new EdgeLocation();
			edgeLocationMap.put(po, location);
		}

		addCutPosition(points.get(3*pi), points.get(3*pi+1), points.get(3*pi+2),
					   points.get(3*po), points.get(3*po+1), points.get(3*po+2), location.coord);
		location.count++;
	}

	private class EdgeLocation {
		float[] coord;
		int count;

		public EdgeLocation() {
			coord = new float[3];
			count = 0;
		}

		public void add(float dx, float dy, float dz) {
			coord[0] += dx;
			coord[1] += dy;
			coord[2] += dz;
			count++;
		}
	}
}
