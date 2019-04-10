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

package com.actelion.research.chem.io;

public interface CompoundTableConstants {
    String cColumnUnassignedItemText = "<Unassigned>";
    String cColumnUnassignedCode = "<none>";
    String cColumnNameRowList = "List '";

	// visible special columns
    String cColumnTypeIDCode = "idcode";
    String cColumnTypeRXNCode = "rxncode";

    String[] cParentSpecialColumnTypes = {
                                    cColumnTypeIDCode,
                                    cColumnTypeRXNCode };

        // non-parent special columns cannot be displayed
    String cColumnType2DCoordinates = "idcoordinates2D";
    String cColumnType3DCoordinates = "idcoordinates3D";
    String cColumnTypeAtomColorInfo = "atomColorInfo";
        // in addition to these all DescriptorHandler.SHORT_NAMEs are valid column types

    int cTextExclusionTypeContains = 1;
    int cTextExclusionTypeStartsWith = 2;
    int cTextExclusionTypeEquals = 3;
    int cTextExclusionTypeRegEx = 4;

    int cMaxTextCategoryCount = 65536;
    int cMaxDateOrDoubleCategoryCount = 256;

    // summary mode for displaying values.
    int cSummaryModeNormal = 0;
    int cSummaryModeMean = 1;
    int cSummaryModeMedian = 2;
    int cSummaryModeMinimum = 3;
    int cSummaryModeMaximum = 4;
    int cSummaryModeSum = 5;
    String[] cSummaryModeText = { "All Values", "Mean Value", "Median Value", "Lowest Value", "Highest Value", "Sum" };
    String[] cSummaryModeCode = { "displayNormal","displayMean","displayMedian","displayMin","displayMax","displaySum" };

    int cDataTypeAutomatic = 0;
    int cDataTypeNumerical = 1;
    int cDataTypeDate = 2;
    int cDataTypeString = 3;
    String[] cDataTypeCode = {"auto", "num", "date", "text"};
    String[] cDataTypeText = {"Automatic", "Numerical", "Date", "Text"};

    // highlight mode for part-of-structure highlighting depending on current record similarity
    int cStructureHiliteModeFilter = 0;
    int cStructureHiliteModeCurrentRow = 1;
    int cStructureHiliteModeNone = 2;
    String[] cStructureHiliteModeText = { "Most Recent Filter", "Current Row Similarity", "No Highlighting" };
    String[] cHiliteModeCode = { "hiliteFilter", "hiliteCurrent", "hiliteNone" };

	String NEWLINE_REGEX = "\\r?\\n|\\r";	// regex for platform independent new line char(s)
	String NEWLINE_STRING = "<NL>";	// used in .dwar, .txt and .cvs files to indicate next line within a cell
	String TAB_STRING = "<TAB>";	// used in .dwar, .txt and .cvs files to indicate a tabulator within a cell
    String cEntrySeparator = "; ";
    byte[] cEntrySeparatorBytes = { ';', ' '};
    String cLineSeparator = "\n";
    byte cLineSeparatorByte = '\n'; // this must be equal to cLineSeparator
    String cRangeSeparation = " <= x < ";
    String cRangeNotAvailable = "<none>";
    String cDefaultDetailSeparator = "|#|";
    String cDetailIndexSeparator = ":";
    String cTextMultipleCategories = "<multiple categories>";

    String cColumnPropertyUseThumbNail = "useThumbNail";
    String cColumnPropertyImagePath = "imagePath";
    String cColumnPropertySpecialType = "specialType";
    String cColumnPropertyParentColumn = "parent";
    String cColumnPropertyIdentifierColumn = "idColumn";
    String cColumnPropertyIsClusterNo = "isClusterNo";
    String cColumnPropertyDataMin = "dataMin";
    String cColumnPropertyDataMax = "dataMax";
    String cColumnPropertyCyclicDataMax = "cyclicDataMax";
    String cColumnPropertyDetailCount = "detailCount";
    String cColumnPropertyDetailName = "detailName";
    String cColumnPropertyDetailType = "detailType";
    String cColumnPropertyOrbitType = "orbitType";
    String cColumnPropertyDetailSource = "detailSource";
    String cColumnPropertyDetailSeparator = "detailSeparator";
    String cColumnPropertyDescriptorVersion = "version";
    String cColumnPropertyIsDisplayable = "isDisplayable";
    String cColumnPropertyBinBase = "binBase";
    String cColumnPropertyBinSize = "binSize";
    String cColumnPropertyBinIsLog = "binIsLog";
    String cColumnPropertyBinIsDate = "binIsDate";
    String cColumnPropertyLookupCount = "lookupCount";
    String cColumnPropertyLookupName = "lookupName";
    String cColumnPropertyLookupURL = "lookupURL";
    String cColumnPropertyLookupEncode = "lookupEncode";
    String cColumnPropertyLookupDetailURL = "lookupDetailURL";
    String cColumnPropertyLaunchCount = "launchCount";
    String cColumnPropertyLaunchName = "launchName";
    String cColumnPropertyLaunchCommand = "launchCommand";
    String cColumnPropertyLaunchOption = "launchOption";
    String cColumnPropertyLaunchDecoration = "launchDecoration";
	String cColumnPropertyLaunchAllowMultiple = "launchAllowMultiple";
    String cColumnPropertyReferencedColumn = "refColumn";
    String cColumnPropertyReferenceStrengthColumn = "refStrengthColumn";
    String cColumnPropertyReferenceType = "refType";
    String cColumnPropertyReferenceTypeRedundant = "redundant";	// a connection is always referenced on both records
    String cColumnPropertyReferenceTypeTopDown = "topdown";	// a connection is only referenced from top record

    String cNativeFileHeaderStart = "<datawarrior-fileinfo>";
    String cNativeFileHeaderEnd = "</datawarrior-fileinfo>";
    String cNativeFileVersion = "version";
    String cNativeFileRowCount = "rowcount";
    String cNativeFileCreated = "created";

    String cColumnPropertyStart = "<column properties>";
    String cColumnPropertyEnd = "</column properties>";
    String cColumnName = "columnName";
    String cColumnProperty = "columnProperty";

    String cHitlistDataStart = "<hitlist data>";
    String cHitlistDataEnd = "</hitlist data>";
    String cHitlistName = "hitlistName";
    String cHitlistData = "hitlistData";

    String cDetailDataStart = "<detail data>";
    String cDetailDataEnd = "</detail data>";
    String cDetailID = "detailID";

    String cPropertiesStart = "<datawarrior properties>";
    String cPropertiesEnd = "</datawarrior properties>";

    String cDataDependentPropertiesStart = "<data dependent properties type=\"";
    String cDataDependentPropertiesEnd = "</data dependent properties>";

    String cExtensionNameFileExplanation = "explanation";
    String cExtensionNameMacroList = "macroList";

    String cFileExplanationStart = "<datawarrior "+cExtensionNameFileExplanation+">";
    String cFileExplanationEnd = "</datawarrior "+cExtensionNameFileExplanation+">";

    String cMacroListStart = "<datawarrior "+cExtensionNameMacroList+">";
    String cMacroListEnd = "</datawarrior "+cExtensionNameMacroList+">";

    String cAutoStartMacro = "autoStartMacro";
	}
