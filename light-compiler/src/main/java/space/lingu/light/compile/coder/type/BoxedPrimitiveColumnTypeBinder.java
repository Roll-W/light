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

package space.lingu.light.compile.coder.type;

import space.lingu.light.LightRuntimeException;
import space.lingu.light.compile.coder.ColumnTypeBinder;
import space.lingu.light.compile.coder.ColumnValueReader;
import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.coder.StatementBinder;
import space.lingu.light.compile.javac.ProcessEnv;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 基本数据类型的包装类处理
 * @author RollW
 */
public class BoxedPrimitiveColumnTypeBinder extends ColumnTypeBinder implements StatementBinder, ColumnValueReader {
    private final PrimitiveColumnTypeBinder mBinder;

    public BoxedPrimitiveColumnTypeBinder(TypeMirror type, PrimitiveColumnTypeBinder primitiveBinder) {
        super(type, primitiveBinder.getDataType());
        mBinder = primitiveBinder;
    }

    private static TypeMirror getBoxedFromPrimitive(TypeMirror primitive, ProcessEnv env) {
        return env.getTypeUtils().boxedClass((PrimitiveType) primitive).asType();
    }

    public static List<BoxedPrimitiveColumnTypeBinder> create(List<PrimitiveColumnTypeBinder> primitiveBinders, ProcessEnv env) {
        List<BoxedPrimitiveColumnTypeBinder> binders = new ArrayList<>();
        primitiveBinders.forEach(binder ->
                binders.add(
                        new BoxedPrimitiveColumnTypeBinder(
                                getBoxedFromPrimitive(binder.type(), env), binder))
        );
        return binders;
    }

    @Override
    public void readFromResultSet(String outVarName, String resultSetName,
                                  String indexName, GenerateCodeBlock block) {
        mBinder.readFromResultSet(outVarName, resultSetName, indexName, block);
    }

    @Override
    public void bindToStatement(String stmtVarName, String indexVarName,
                                String valueVarName, GenerateCodeBlock block) {
        block.builder()
                .beginControlFlow("if ($L == null)", valueVarName)
                .beginControlFlow("try")
                .addStatement("$L.setNull($L, $L)", stmtVarName, indexVarName, Types.NULL)
                .nextControlFlow("catch ($T e)", SQLException.class)
                .addStatement("throw new $T(e)", LightRuntimeException.class)
                .endControlFlow()
                .nextControlFlow("else");
        mBinder.bindToStatement(stmtVarName, indexVarName, valueVarName, block);
        block.builder().endControlFlow();
    }
}
