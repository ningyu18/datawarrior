package org.openmolecules.mesh;

public interface MeshBuilder {
	int addPoint(float x, float y, float z);
	void addTriangle(int i1, int i2, int i3);
	void getPoint(int index, float[] xyz);
	}
