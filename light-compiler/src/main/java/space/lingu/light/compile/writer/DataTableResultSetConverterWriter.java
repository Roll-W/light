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

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import space.lingu.light.compile.JavaPoetClass;
import space.lingu.light.compile.MethodNames;
import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.coder.custom.QueryContext;
import space.lingu.light.compile.struct.DataTable;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
public class DataTableResultSetConverterWriter extends ClassWriter.SharedMethodSpec {
    private final QueryContext queryContext;
    private final DataTable table;

    public DataTableResultSetConverterWriter(QueryContext queryContext,
                                             DataTable table) {
        super("entityResultSetConverter_" + table.getTypeName().toString());
        // TODO:
        this.queryContext = queryContext;
        this.table = table;
    }

    @Override
    public String getUniqueKey() {
        return "GenericEntityConverterOf-" + table.getTypeCompileType().getQualifiedName();
    }

    @Override
    public void prepare(String methodName, ClassWriter writer,
                        MethodSpec.Builder builder) {
        ParameterSpec resSetParam = ParameterSpec
                .builder(JavaPoetClass.JdbcNames.RESULT_SET, "resultSet")
                .build();
        builder.addParameter(resSetParam)
                .addModifiers(Modifier.PRIVATE)
                .returns(table.getTypeName())
                .addCode(buildConvertMethodBody(writer, resSetParam));
    }

    private CodeBlock buildConvertMethodBody(ClassWriter writer,
                                             ParameterSpec resultSetParam) {
        GenerateCodeBlock block = new GenerateCodeBlock(writer);
        String tableVar = block.getTempVar("_dataTable");
        block.builder().addStatement("final $T $L", table.getTypeName(), tableVar);
        List<FieldReadWriteWriter.FieldWithNumber> fieldWithNumberList = new ArrayList<>();

        table.getFields().getFields().forEach(field -> {
            String indexVar = block.getTempVar("_resultSetIndexOf" + table.getTypeCompileType().getSimpleName());
            block.builder()
                    .addStatement("final $T $L = $L.$T($N, $S)",
                            TypeName.INT, indexVar,
                            queryContext.getHandlerVarName(),
                            MethodNames.sGetColumnIndex,
                            resultSetParam,
                            field.getColumnName());

            fieldWithNumberList.add(
                    new FieldReadWriteWriter.FieldWithNumber(field, indexVar)
            );
        });

        FieldReadWriteWriter.readFromResultSet(tableVar, table,
                resultSetParam.name, fieldWithNumberList, block);

        block.builder().addStatement("return $L", tableVar);
        return block.generate();
    }
}
