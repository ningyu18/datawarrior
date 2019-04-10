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
 * @author Thomas Sander
 */

package com.actelion.research.chem.descriptor;

import com.actelion.research.chem.SSSearcherWithIndex;
import com.actelion.research.chem.reaction.ReactionSearcher;

public interface DescriptorConstants {
    public static final int DESCRIPTOR_TYPE_UNKNOWN = -1;
    public static final int DESCRIPTOR_TYPE_MOLECULE = 1;
    public static final int DESCRIPTOR_TYPE_REACTION = 2;

    public static final DescriptorInfo DESCRIPTOR_FFP512 = 
                            new DescriptorInfo("FragmentFingerprint512",
                                               "FragFp",
											   SSSearcherWithIndex.cIndexVersion,
                                               DESCRIPTOR_TYPE_MOLECULE,
                                               true,
                                               true,
                                               false);
    public static final DescriptorInfo DESCRIPTOR_PFP512 = 
                            new DescriptorInfo("PathFingerprint512",
                                               "PathFp",
											   "1.1",
                                               DESCRIPTOR_TYPE_MOLECULE,
                                               true,
                                               true,
                                               false);
    public static final DescriptorInfo DESCRIPTOR_HashedCFp = 
                            new DescriptorInfo("HashedSphericalFingerprint512",
                                               "SphereFp",
											   "2.1",
                                               DESCRIPTOR_TYPE_MOLECULE,
                                               true,
                                               true,
                                               true);	// for the creation of up/down bonds
    public static final DescriptorInfo DESCRIPTOR_SkeletonSpheres = 
                                               new DescriptorInfo("HashedSkeletonSphereCount1024",
                                               "SkelSpheres",
											   "1.1",
                                               DESCRIPTOR_TYPE_MOLECULE,
                                               false,
                                               true,
                                               true);	// for the creation of up/down bonds
    public static final DescriptorInfo DESCRIPTOR_OrganicFunctionalGroups = 
    										   new DescriptorInfo("FunctionalGroupTreeCount1024",
    										   "OrgFunctions",
											   "1.0",
    										   DESCRIPTOR_TYPE_MOLECULE,
    										   false,
    										   false,
    										   true);	// for the creation of up/down bonds
    public static final DescriptorInfo DESCRIPTOR_CenteredSkeletonFragments = 
										        new DescriptorInfo("CenteredSkeletonFragments",
										        "CentSkelFrags",
												"1.0",
										        DESCRIPTOR_TYPE_MOLECULE,
										        false,
										        true,
										        true);	// for the creation of up/down bonds
    public static final DescriptorInfo DESCRIPTOR_TopoPPHistDist = 
                            new DescriptorInfo("TopologicalPharmacophoreHistograms",
                                               "TopPPHist",
                                               "version",
                                               DESCRIPTOR_TYPE_MOLECULE,
                                               false,
                                               false,
                                               false);
    public static final DescriptorInfo DESCRIPTOR_Flexophore = 
                            new DescriptorInfo("Flexophore",
                                               "Flexophore",
											   "4.4",
                                               DESCRIPTOR_TYPE_MOLECULE,
                                               false,
                                               false,
                                               false);
    public static final DescriptorInfo DESCRIPTOR_Flexophore_HighRes =
        					new DescriptorInfo("FlexophoreHighResolution",
        									   "FlexophoreHighRes",
												"version",
        									   DESCRIPTOR_TYPE_MOLECULE,
        									   false,
        									   false,
        									   false);
    public static final DescriptorInfo DESCRIPTOR_ReactionIndex = 
                            new DescriptorInfo("ReactionIndex",
                                               "RxnIdx",
											   ReactionSearcher.cIndexVersion,
                                               DESCRIPTOR_TYPE_REACTION,
                                               false,
                                               false,
                                               false);
    public static final DescriptorInfo DESCRIPTOR_IntegerVector = 
    						new DescriptorInfo("IntegerVector",
    											"IntVec",
												"1.0",
    											DESCRIPTOR_TYPE_UNKNOWN,
    											false,
    											false,
    											false);
   
    public static final DescriptorInfo DESCRIPTOR_MAX_COMMON_SUBSTRUCT = 
        					new DescriptorInfo("MaximumCommonSubstructure",
        										"Structure",
												"1.0",
        										DESCRIPTOR_TYPE_MOLECULE,
        										false,
        										true,
        										false);

    public static final DescriptorInfo DESCRIPTOR_SUBSTRUCT_QUERY_IN_BASE = 
							new DescriptorInfo("SubStructureQueryInBase",
												"SSSQinB",
												"1.0",
												DESCRIPTOR_TYPE_MOLECULE,
												false,
												false, // ??? TODO check
												false);
    
    public static final DescriptorInfo DESCRIPTOR_FULL_FRAGMENT_SET = 
							new DescriptorInfo("FullFragmentSet",
											   "FullFragSet",
											   "1.0",
											   DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE,
											   true,
											   true,
											   false);
    
    public static final DescriptorInfo DESCRIPTOR_PhysicoChemicalProperties = 
							new DescriptorInfo("DescriptorPhysicoChemicalProperties",
											   "PhysChem",
												"version",
											   DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE,
											   false,
											   false,
											   false);

    public static final DescriptorInfo DESCRIPTOR_BINARY_SKELETONSPHERES =
							new DescriptorInfo("BinarySkeletonSpheres",
												"BinSkelSpheres",
												"10052017",
												DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE,
												true,
												true,
												false);

    public static final DescriptorInfo[] DESCRIPTOR_LIST = {
                                                DESCRIPTOR_FFP512,
                                                DESCRIPTOR_PFP512,
                                                DESCRIPTOR_HashedCFp,
                                                DESCRIPTOR_SkeletonSpheres,
                                                DESCRIPTOR_OrganicFunctionalGroups,
                                                DESCRIPTOR_Flexophore
                                                };

    public static final DescriptorInfo[] DESCRIPTOR_EXTENDED_LIST = {
                                                DESCRIPTOR_FFP512,
                                                DESCRIPTOR_PFP512,
                                                DESCRIPTOR_HashedCFp,
                                                DESCRIPTOR_SkeletonSpheres,
												DESCRIPTOR_BINARY_SKELETONSPHERES,
                                                DESCRIPTOR_CenteredSkeletonFragments,
                                                DESCRIPTOR_FULL_FRAGMENT_SET,
                                                DESCRIPTOR_MAX_COMMON_SUBSTRUCT,
                                                DESCRIPTOR_SUBSTRUCT_QUERY_IN_BASE,
                                                DESCRIPTOR_TopoPPHistDist,
                                                DESCRIPTOR_OrganicFunctionalGroups,
                                                DESCRIPTOR_Flexophore,
                                                DESCRIPTOR_Flexophore_HighRes,
                                                DESCRIPTOR_ReactionIndex,
                                                DESCRIPTOR_IntegerVector,
                                                DESCRIPTOR_FULL_FRAGMENT_SET,
                                                DESCRIPTOR_PhysicoChemicalProperties,
                                                DESCRIPTOR_BINARY_SKELETONSPHERES
                                                };
    }

