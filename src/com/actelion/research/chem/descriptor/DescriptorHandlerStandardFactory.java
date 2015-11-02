package com.actelion.research.chem.descriptor;

import com.actelion.research.chem.StereoMolecule;

public class DescriptorHandlerStandardFactory extends DescriptorHandlerStandard2DFactory {
	private static DescriptorHandlerStandardFactory sFactory;

	public static DescriptorHandlerFactory getFactory() {
		if (sFactory == null) {
			synchronized(DescriptorHandlerStandardFactory.class) {
				if (sFactory == null)
					sFactory = new DescriptorHandlerStandardFactory();
				}
			}
		return sFactory;
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DescriptorHandler getDefaultDescriptorHandler(String shortName) {
		DescriptorHandler<Object, StereoMolecule> dh = super.getDefaultDescriptorHandler(shortName);
		if (dh != null)
			return dh;

		if (DESCRIPTOR_Flexophore.shortName.equals(shortName))
			return DescriptorHandlerFlexophore.getDefaultInstance();

		return null;
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DescriptorHandler create(String shortName) {
		DescriptorHandler dh = super.create(shortName);
		if (dh != null)
			return dh;

		if (DESCRIPTOR_Flexophore.shortName.equals(shortName))
			return new DescriptorHandlerFlexophore();

		return null;
		}
	}
