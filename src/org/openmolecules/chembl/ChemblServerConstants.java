package org.openmolecules.chembl;

import com.actelion.research.chem.descriptor.DescriptorConstants;

public interface ChemblServerConstants {
	public static final String SERVER_URL = "http://chembl.openmolecules.org";	// this only used by the client
//	public static final String SERVER_URL = "http://morpheus:8083";				// this only used by the client
//	public static final String SERVER_URL = "http://localhost:8083";			// this only used by the client

	public static final int MAX_QUERY_TARGETS = 128;

  	public static final int RESULT_COLUMN_IDCODE = 0;
	public static final int RESULT_COLUMN_IDCOORDINATES = 1;
	public static final int RESULT_COLUMN_FFP512 = 2;
	public static final int RESULT_COLUMN_MOLREGNO = 3;
	public static final int RESULT_COLUMN_ACTIVITY_MODIFIER = 4;
	public static final int RESULT_COLUMN_ACTIVITY_VALUE = 5;
	public static final int RESULT_COLUMN_ACTIVITY_UNIT = 6;
	public static final int RESULT_COLUMN_ACTIVITY_TYPE = 7;
	public static final int RESULT_COLUMN_ACTIVITY_LIGAND_EFFICIENCY = 8;
	public static final int RESULT_COLUMN_ACTIVITY_BINDING_EFFICIENCY = 9;
	public static final int RESULT_COLUMN_ACTIVITY_SURFACE_EFFICIENCY = 10;
	public static final int RESULT_COLUMN_ASSAY_INDEX = 11;
	public static final int RESULT_COLUMN_ASSAY_CATEGORY = 12;
	public static final int RESULT_COLUMN_ASSAY_ORGANISM = 13;
	public static final int RESULT_COLUMN_TARGET_ID = 14;
	public static final int RESULT_COLUMN_TARGET_NAME = 15;
	public static final int RESULT_COLUMN_TARGET_ORGANISM = 16;
	public static final int RESULT_COLUMN_TARGET_DESCRIPTION = 17;
	public static final int RESULT_COLUMN_SEQUENCE_ACCESSION = 18;
	public static final int RESULT_COLUMN_REFERENCE = 19;
	public static final int RESULT_COLUMN_PUBMED_ID = 20;
	public static final int RESULT_COLUMN_DOI = 21;

	public static final String[] COLUMN_NAME = { "idcode",
												  "idcoordinates",
												  DescriptorConstants.DESCRIPTOR_FFP512.shortName,
												  "MolRegNo",
												  "m",
												  "Value",
												  "Unit",
												  "Type",
												  "Ligand Efficiency",
												  "Binding Efficiency",
												  "Surface Efficiency",
												  "Assay Index",
												  "Assay Category",
												  "Assay Organism",
												  "Target ID",
												  "Target Name",
												  "Target Organism",
												  "Target Description",
												  "Sequence Accession",
												  "Reference",
												  "Pubmed ID",
												  "DOI" };

	public static final String[] COLUMN_NAME_GET_ACTIVES_FROM_ACTIVE_TARGET = { "Active Compound", "Activity", "Activity Type" };
	public static final int GET_ACTIVES_FROM_ACTIVE_TARGET_RESULT_COLUMN_ACTIVE_IDCODE = 0;
	public static final int GET_ACTIVES_FROM_ACTIVE_TARGET_RESULT_COLUMN_ACTIVITY = 1;
	public static final int GET_ACTIVES_FROM_ACTIVE_TARGET_RESULT_COLUMN_ACTIVITY_TYPE = 2;

	public static final String[] COLUMN_NAME_FIND_ACTIVES_FLEXOPHORE = { "Similarity Flexophore", "Similarity SkeletonSpheres", "Active Compound", "Activity [nM]", "Activity Type", "Target" };
	public static final String[] COLUMN_NAME_FIND_ACTIVES_SKELSPHERES = { "Query Compound", "Similarity SkeletonSpheres", "Active Compound", "Activity [nM]", "Activity Type", "Target" };
	public static final int FIND_ACTIVES_RESULT_COLUMN_QUERY_IDCODE = 0;	// first column in case of skelSpheres
	public static final int FIND_ACTIVES_RESULT_COLUMN_FLEXOPHORE = 0;		// first column in case of flexophore
	public static final int FIND_ACTIVES_RESULT_COLUMN_SKELSPHERES = 1;
	public static final int FIND_ACTIVES_RESULT_COLUMN_ACTIVE_IDCODE = 2;
	public static final int FIND_ACTIVES_RESULT_COLUMN_ACTIVITY = 3;
	public static final int FIND_ACTIVES_RESULT_COLUMN_ACTIVITY_TYPE = 4;
	public static final int FIND_ACTIVES_RESULT_COLUMN_TARGET = 5;

	public static final int TARGET_COLUMN_ID = 0;
	public static final int TARGET_COLUMN_TYPE = 1;
	public static final int TARGET_COLUMN_NAME = 2;
	public static final int TARGET_COLUMN_ORGANISM = 3;
	public static final int TARGET_COLUMN_ACCESSION = 4;
	public static final int TARGET_COLUMN_CLASSIFICATION_LEVEL_1 = 5;

	// Column count and individual column order in protein class dictionary
	public static final int CLASS_DICTIONARY_COLUMNS = 3;
	public static final int CLASS_DICTIONARY_COLUMN_NAME = 0;
	public static final int CLASS_DICTIONARY_COLUMN_LEVEL = 1;	// protein class level 0:root, 1 ... PROTEIN_CLASS_LEVELS
	public static final int CLASS_DICTIONARY_COLUMN_PARENT = 2; // Java array indexes of parent record

	public static final int PROTEIN_CLASS_LEVELS = 6;

	public static final String REQUEST_GET_VERSION = "getVersion";
	public static final String REQUEST_GET_VERSION_AND_TARGETS = "getVersionAndTargets";
	public static final String REQUEST_GET_TARGET_LIST = "getTargets";
	public static final String REQUEST_GET_PROTEIN_CLASS_DICTIONARY = "getProteinClassDictionary";
	public static final String REQUEST_GET_ACTIVE_TARGET_LIST = "getActiveTargets";
	public static final String REQUEST_GET_ASSAY_DETAILS = "getAssayDetails";
	public static final String REQUEST_FIND_ACTIVES_FLEXOPHORE = "findActivesFlexophore";	// skelSpheres & flexophore
	public static final String REQUEST_FIND_ACTIVES_SKELSPHERES = "findActivesSkelSpheres";	// skelSpheres only
	public static final String REQUEST_GET_KNOWN_ACTIVES = "getKnownActives";
	public static final String KEY_ID = "id";
	public static final String KEY_ID_LIST = "idList";
	public static final String KEY_IDCODE = "idcode";
	public static final String KEY_IDCODE_LIST = "idcodeList";
	public static final String QUERY_DOC_ID_LIST = "docIDs";
	public static final String QUERY_TARGET_LIST = "targetIDs";
	public static final String QUERY_STRUCTURE_SEARCH_SPEC = "ssSpec";
	}
