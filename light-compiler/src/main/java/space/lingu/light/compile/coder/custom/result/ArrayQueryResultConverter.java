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

package space.lingu.light.compile.coder.custom.result;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.coder.custom.QueryContext;
import space.lingu.light.compile.coder.custom.row.RowConverter;
import space.lingu.light.compile.javac.TypeUtils;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;

/**
 * @author RollW
 */
public class ArrayQueryResultConverter extends AbstractQueryResultConverter {
    public final RowConverter converter;
    private final TypeMirror type;

    public ArrayQueryResultConverter(RowConverter converter) {
        super(converter);
        this.converter = converter;
        this.type = converter.getOutType().getTypeMirror();
        // type here is not the array's original type.
        // e.g., given long[], here the type is long.
    }

    @Override
    public void convert(QueryContext queryContext, GenerateCodeBlock block) {
        converter.onResultSetReady(queryContext, block);

        TypeName containerType = TypeName.get(type);

        TypeName arrayType = ArrayTypeName.of(containerType);
        final String tempContainerName = block.getTempVar("_arrayType");
        final String tempArrayListName = block.getTempVar("_arrayList");

        if (TypeUtils.isArray(type)) {
            // 2 dimensions array
            // In fact, the support is not very good, and it is now in an invalid state.
            TypeName ejectContainerType = TypeName.get(TypeUtils.getArrayElementType(type));
            ParameterizedTypeName arrayListType = ParameterizedTypeName.get(
                    ClassName.get(ArrayList.class), ejectContainerType.box());
            block.builder()
                    .addStatement("final $T $L = new $T()",
                            arrayListType, tempArrayListName, arrayListType)
                    .addStatement("final $T $L = new $T[0][]",
                            arrayType, tempContainerName,
                            ejectContainerType);
        } else {
            ParameterizedTypeName arrayListType = ParameterizedTypeName.get(
                    ClassName.get(ArrayList.class), containerType.box());
            block.builder()
                    .addStatement("final $T $L = new $T()",
                            arrayListType, tempArrayListName, arrayListType)
                    .addStatement("final $T $L = new $T[0]",
                            arrayType, tempContainerName,
                            containerType);
        }
        final String tempVar = block.getTempVar("_item");
        block.builder()
                .beginControlFlow("while ($L.next())", queryContext.getResultSetVarName())
                .addStatement("final $T $L", TypeName.get(type), tempVar);
        QueryContext scopedContext = queryContext.fork(tempVar);
        converter.convert(scopedContext, block);

        block.builder()
                .addStatement("$L.add($L)", tempArrayListName, tempVar)
                .endControlFlow();
        if (containerType.isPrimitive()) {
            // needs to convert if primitive
            joinsArray(queryContext.getOutVarName(), tempArrayListName,
                    arrayType, containerType, block
            );
        } else {
            block.builder().addStatement("final $T $L = $L.toArray($L)",
                    arrayType, queryContext.getOutVarName(), tempArrayListName, tempContainerName);
        }
        converter.onResultSetFinish(block);
    }

    private void joinsArray(String outVarName,
                            String arrayListName,
                            TypeName arrayType,
                            TypeName arrayContainerType,
                            GenerateCodeBlock block) {
        String listSizeVarName = block.getTempVar("_listSize");
        String tempIndexVarName = block.getTempVar("_index");
        block.builder()
                .addStatement("final int $L = $L.size()",
                        listSizeVarName, arrayListName)
                .addStatement("final $T $L = new $T[$L]",
                        arrayType, outVarName, arrayContainerType, listSizeVarName)
                .beginControlFlow("for (int $L = 0; $L < $L; $L++)",
                        tempIndexVarName, tempIndexVarName, listSizeVarName, tempIndexVarName)
                .addStatement("$L[$L] = $L.get($L)",
                        outVarName, tempIndexVarName, arrayListName, tempIndexVarName)
                .endControlFlow();
    }

}
