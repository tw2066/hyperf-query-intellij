package dev.ekvedaras.hyperfquery.inspection

import dev.ekvedaras.hyperfquery.BaseTestCase
import dev.ekvedaras.hyperfquery.services.HyperfQuerySettings

internal class DasObjectsInspectionTest : BaseTestCase() {
    fun testWarnsAboutUnknownSchema() {
        assertInspection("inspection/unknownSchema.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutUnknownTable() {
        assertInspection("inspection/unknownTable.php", UnknownTableOrViewInspection())
    }

    fun testDoesNotWarnAboutKnownTable() {
        assertInspection("inspection/knownTable.php", UnknownTableOrViewInspection())
    }

    fun testDoesNotWarnAboutKnownTableWhenUsingPrefixes() {
        useTablePrefix("failed_")
        assertInspection("inspection/knownWithPrefixTable.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutUnknownTableWhenUsingPrefixes() {
        useTablePrefix("failed")
        assertInspection("inspection/unknownWithPrefixTable.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutUnknownSchemaTable() {
        assertInspection("inspection/unknownSchemaTable.php", UnknownTableOrViewInspection())
    }

    fun testDoesNotWarnAboutKnownSchemaTable() {
        assertInspection("inspection/knownSchemaTable.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutTableFromOtherSchema() {
        assertInspection("inspection/tableFromOtherSchema.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutUnknownColumn() {
        assertInspection("inspection/unknownColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownColumn() {
        assertInspection("inspection/knownColumn.php", UnknownColumnInspection())
    }

    fun testWarnsAboutUnknownTableColumn() {
        assertInspection("inspection/unknownTableColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownTableColumn() {
        assertInspection("inspection/knownTableColumn.php", UnknownColumnInspection())
    }

    fun testWarnsAboutUnknownSchemaTableColumn() {
        assertInspection("inspection/unknownSchemaTableColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownSchemaTableColumn() {
        assertInspection("inspection/knownSchemaTableColumn.php", UnknownColumnInspection())
    }

    fun testWarnsAboutUnknownAliasColumn() {
        assertInspection("inspection/unknownAliasColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownAliasColumn() {
        assertInspection("inspection/knownAliasColumn.php", UnknownColumnInspection())
        assertInspection("inspection/knownAliasColumn.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutColumnFromOtherTable() {
        assertInspection("inspection/columnFromOtherTable.php", UnknownColumnInspection())
    }

    fun testWarnsAboutUnknownTableAndColumn() {
        assertInspection("inspection/unknownTableAndColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownSelectRawColumn() {
        assertInspection("inspection/knownSelectRawColumn.php", UnknownColumnInspection())
    }

    fun testWarnsAboutUnknownSelectRawColumn() {
        assertInspection("inspection/unknownSelectRawColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutComplexSelectRawExpression() {
        assertInspection("inspection/selectRawComplexExpression.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownRawExpressionColumns() {
        assertInspection("inspection/knownRawExpressionColumns.php", UnknownColumnInspection())
    }

    fun testWarnsAboutUnknownRawExpressionColumn() {
        assertInspection("inspection/unknownRawExpressionColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownSelectRawCommaSeparatedColumns() {
        assertInspection("inspection/knownSelectRawColumns.php", UnknownColumnInspection())
    }

    fun testWarnsAboutUnknownSelectRawCommaSeparatedColumn() {
        assertInspection("inspection/unknownSelectRawColumns.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutPrefixedRawExpressionColumns() {
        addPrefixedGoodsConfig()
        assertInspection("inspection/knownPrefixedRawColumns.php", UnknownColumnInspection())
    }

    fun testWarnsAboutUnknownPrefixedRawExpressionColumn() {
        addPrefixedGoodsConfig()
        assertInspection("inspection/unknownPrefixedRawColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutOperatorInJoinCallWithOperator() {
        assertInspection("inspection/joinWithOperator.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutUnknownColumnForWhereInMethodValuesList() {
        assertInspection("inspection/noInspectionsForValuesInWhereIn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutUnknownColumnForNestedArrayKeys() {
        assertInspection("inspection/noInspectionsForNestedArrayKeys.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownJsonColumn() {
        assertInspection("inspection/knownJsonColumn.php", UnknownColumnInspection())
    }

    fun testItDoesNotWarnAboutUnknownColumnInObjectsAsCreateFunctionValues() {
        assertInspection("inspection/newObjectWithinCreateValue.php", UnknownColumnInspection())
    }

    fun testWarnsAboutColumnFromOtherConnectionSchema() {
        addDatabasesConfig()
        assertInspection("inspection/unknownColumnOnConnection.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownColumnOnConnection() {
        addDatabasesConfig()
        assertInspection("inspection/knownColumnOnConnection.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutKnownDbFacadeTable() {
        assertInspection("inspection/knownDbTable.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutUnknownDbFacadeTable() {
        assertInspection("inspection/unknownDbTable.php", UnknownTableOrViewInspection())
    }

    fun testDoesNotWarnAboutKnownModelTableProperty() {
        assertInspection("inspection/knownModelTableProperty.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutUnknownModelTableProperty() {
        assertInspection("inspection/unknownModelTableProperty.php", UnknownTableOrViewInspection())
    }

    fun testDoesNotWarnAboutKnownModelTablePropertyOnConnection() {
        addDatabasesConfig()
        assertInspection("inspection/knownModelTablePropertyOnConnection.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutModelTablePropertyFromOtherConnectionSchema() {
        addDatabasesConfig()
        assertInspection("inspection/unknownModelTablePropertyOnConnection.php", UnknownTableOrViewInspection())
    }

    fun testWarnsAboutUnknownSchemaInModelTableProperty() {
        assertInspection("inspection/unknownModelSchemaTableProperty.php", UnknownTableOrViewInspection())
    }

    fun testDoesNotWarnAboutKnownConnection() {
        addDatabasesConfig()
        assertInspection("inspection/knownConnection.php", UnknownConnectionInspection())
    }

    fun testWarnsAboutUnknownConnection() {
        addDatabasesConfig()
        assertInspection("inspection/unknownConnection.php", UnknownConnectionInspection())
    }

    fun testDoesNotWarnAboutConnectionWithoutConfigFile() {
        assertInspection("inspection/connectionWithoutConfig.php", UnknownConnectionInspection())
    }

    fun testDoesNotWarnAboutKnownModelConnectionProperty() {
        addDatabasesConfig()
        assertInspection("inspection/knownModelConnectionProperty.php", UnknownConnectionInspection())
    }

    fun testWarnsAboutUnknownModelConnectionProperty() {
        addDatabasesConfig()
        assertInspection("inspection/unknownModelConnectionProperty.php", UnknownConnectionInspection())
    }

    fun testDoesNotWarnAboutPropertiesOutsideModel() {
        addDatabasesConfig()
        assertInspection("inspection/notModelProperties.php", UnknownConnectionInspection())
        assertInspection("inspection/notModelProperties.php", UnknownTableOrViewInspection())
    }
}
