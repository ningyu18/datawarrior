package org.openmolecules.fx.viewer3d;

import javafx.scene.paint.PhongMaterial;

/**
 * Created by thomas on 15.11.15.
 */
public class NodeDetail {
    private PhongMaterial mMaterial;
    private int mAtom,mBond;
    private boolean mMayOverride,mIsSelected;

    public NodeDetail(PhongMaterial material, int atom, int bond, boolean mayOverride) {
        mMaterial = material;
        mAtom = atom;
        mBond = bond;
	    mMayOverride = mayOverride;
        }

    public PhongMaterial getMaterial() {
        return mMaterial;
        }

    public boolean mayOverrideMaterial() {
        return mMayOverride;
        }

    public boolean isAtom() {
        return mAtom != -1;
        }

    public boolean isBond() {
        return mBond != -1;
        }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean isSelected) {
        mIsSelected = isSelected;
    	}

	/**
	 * @return whether this node is transparent and used as helper for atom picking in wire or stick mode
	 */
	public boolean isTransparent() {
		return mMaterial == V3DMoleculeBuilder.sTransparentMaterial;
		}

    public int getAtom() {
        return mAtom;
        }

    public int getBond() {
        return mBond;
    }
    }
