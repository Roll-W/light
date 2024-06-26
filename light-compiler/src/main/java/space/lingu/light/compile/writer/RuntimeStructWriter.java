/*
 * Copyright (C) 2022 Lingu Light Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package space.lingu.light.compile.writer;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import space.lingu.light.Order;
import space.lingu.light.SQLDataType;
import space.lingu.light.compile.JavaPoetClass;
import space.lingu.light.compile.MethodNames;
import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.struct.*;
import space.lingu.light.struct.Table;
import space.lingu.light.struct.TableColumn;
import space.lingu.light.struct.TableIndex;
import space.lingu.light.struct.TablePrimaryKey;
import space.lingu.light.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Convert to runtime struct.
 *
 * @author RollW
 * @see space.lingu.light.struct
 */
public class RuntimeStructWriter {
    private final DataTable dataTable;

    public RuntimeStructWriter(DataTable dataTable) {
        this.dataTable = dataTable;
    }

    public String writeDatabase(GenerateCodeBlock block) {
        return null;
    }

    /**
     * Mapping table to {@link space.lingu.light.struct.Table}
     *
     * @return The name of the temporary variable
     */
    public String writeDataTable(GenerateCodeBlock block, String databaseConfVarName) {
        String classSimpleName = ((ClassName) dataTable.getTypeName()).simpleName();
        final String tableVarName = block.getTempVar("_tableOf" + StringUtils.firstUpperCase(classSimpleName));
        final String columnListVarName = block.getTempVar("_columnListOf" + StringUtils.firstUpperCase(classSimpleName));
        final String indexListVarName = block.getTempVar("_indexListOf" + StringUtils.firstUpperCase(classSimpleName));
        TypeName columnListType = ParameterizedTypeName
                .get(ClassName.get(List.class), ClassName.get(TableColumn.class));
        TypeName columnArrayListType = createArrayListType(TableColumn.class);

        TypeName indexListType = ParameterizedTypeName
                .get(ClassName.get(List.class), ClassName.get(TableIndex.class));
        TypeName indexArrayListType = createArrayListType(TableIndex.class);

        block.builder()
                .addStatement("$T $L = new $T()", columnListType, columnListVarName, columnArrayListType)
                .addStatement("$T $L = new $T()", indexListType, indexListVarName, indexArrayListType);
        String tableConfVarName = writeConfigurationsAndFork(dataTable,
                "TbOf" + dataTable.getTypeCompileType().getSimpleName().toString(),
                databaseConfVarName, block);
        dataTable.getIndices().forEach(index ->
                writeIndex(block, index, indexListVarName, tableConfVarName));
        dataTable.getFields().getFields().forEach(field ->
                writeTableColumn(block, field, columnListVarName, tableConfVarName));
        final String pkVarName = writePrimaryKey(block, columnListVarName, dataTable.getPrimaryKey());

        block.builder()
                .addStatement("$T $L = new $T($S, $L, $L, $L, $L)",
                        Table.class, tableVarName, Table.class,
                        dataTable.getTableName(),
                        columnListVarName,
                        pkVarName,
                        indexListVarName,
                        tableConfVarName
                );
        return tableVarName;
    }

    private String writeConfigurationsAndFork(Configurable configurable,
                                              String prefix,
                                              String prevConfVarName,
                                              GenerateCodeBlock block) {
        final String forkVarName = block.getTempVar("_configurationsFork" + prefix);
        String tempConf = Configurable.writeConfiguration(configurable, prefix, block);
        block.builder().addStatement("$T $L = $L.plus($L)",
                JavaPoetClass.CONFIGURATIONS,
                forkVarName, prevConfVarName, tempConf);
        return forkVarName;
    }

    private void writeTableColumn(GenerateCodeBlock block, Field field,
                                  String listVarName,
                                  String tableConfVarName) {
        final String tableColumnVarName = block.getTempVar("_tableColumn" + StringUtils.firstUpperCase(field.getName()));
        boolean autoGen = false;
        if (dataTable.getPrimaryKey().getFields().hasField(field)) {
            autoGen = dataTable.getPrimaryKey().isAutoGenerate();
        }
        String fieldConfVarName = writeConfigurationsAndFork(field,
                "ColumnOf" + StringUtils.firstUpperCase(field.getName()),
                tableConfVarName,
                block);
        block.builder()
                // Params:

                // String name, String fieldName,
                // String defaultValue,
                // boolean hasDefaultValue,
                // SQLDataType dataType,
                // boolean nullable,
                // boolean autoGenerate,
                // Configurations
                .addStatement(
                        "$T $L = new $T($S, $S, $S, $L,\n$T.$L, $L, $L, $L)",
                        TableColumn.class, tableColumnVarName, TableColumn.class,
                        // params
                        field.getColumnName(),
                        field.getName(),
                        field.getDefaultValue(),
                        field.isHasDefault(),
                        SQLDataType.class, field.getDataType(),
                        field.getNullability() != Nullability.NONNULL,
                        autoGen,
                        fieldConfVarName
                )
                .addStatement("$L.add($L)", listVarName, tableColumnVarName);
    }

    private void writeIndex(GenerateCodeBlock block,
                            Index index, String indexListVarName,
                            String tableConfVarName) {
        final String tableIndexVarName = block.getTempVar("_tableIndexOf" + index.getName());
        ArrayTypeName orderArrayType = ArrayTypeName.of(ClassName.get(Order.class));
        ArrayTypeName stringArrayType = ArrayTypeName.of(ClassName.get(String.class));
        String indexConfVarName = writeConfigurationsAndFork(index,
                "ColumnOf" + index.getName(), tableConfVarName, block);

        final String indexOrderArrayVarName = block.getTempVar("_tableIndexOrdersOf" + index.getName());
        final String indexColumnsArrayVarName = block.getTempVar("_tableIndexColumnsOf" + index.getName());
        StringJoiner orders = new StringJoiner(", ");
        index.getOrders().forEach(order ->
                orders.add("Order." + order.name())
        );

        StringJoiner columnsJoiner = new StringJoiner(", ");
        index.getFields().getFields().forEach(field ->
                columnsJoiner.add("\"" + field.getColumnName() + "\""));

        block.builder()
                .addStatement("$T $L = {$L}", orderArrayType,
                        indexOrderArrayVarName, orders.toString())
                .addStatement("$T $L = {$L}", stringArrayType,
                        indexColumnsArrayVarName, columnsJoiner.toString())
                .addStatement("$T $L = new $T($S, $S, $L, $L, $L, $L)",
                        TableIndex.class, tableIndexVarName, TableIndex.class,
                        dataTable.getTableName(),
                        index.getName(), index.isUnique(),
                        indexOrderArrayVarName,
                        indexColumnsArrayVarName, indexConfVarName
                )
                .addStatement("$L.add($L)", indexListVarName, tableIndexVarName);
    }

    private String writePrimaryKey(GenerateCodeBlock block,
                                   String listVarName,
                                   PrimaryKey key) {
        final String simpleName = dataTable.getTypeCompileType().getSimpleName().toString();
        final String primaryKeyVarName = block.getTempVar("_pkOf" + simpleName);
        String pkColumnsVarName = block.getTempVar("_pkTableColumnsOf" + simpleName);
        TypeName keyArrayListType = createArrayListType(TableColumn.class);
        block.builder().addStatement("$T $L = new $T()",
                keyArrayListType, pkColumnsVarName, keyArrayListType);
        for (Field field : key.getFields().getFields()) {
            block.builder().addStatement("$L.add($T.$L($S, $L))",
                    pkColumnsVarName,
                    JavaPoetClass.UtilNames.STRUCT_UTIL, MethodNames.sFindByName,
                    field.getColumnName(), listVarName);
        }
        block.builder()
                .addStatement("$T $L = new $T($L, $L)",
                        TablePrimaryKey.class, primaryKeyVarName,
                        TablePrimaryKey.class,
                        pkColumnsVarName, key.isAutoGenerate()
                );

        return primaryKeyVarName;
    }

    private static ParameterizedTypeName createArrayListType(Class<?> clz) {
        return ParameterizedTypeName
                .get(ClassName.get(ArrayList.class), ClassName.get(clz));
    }
}
