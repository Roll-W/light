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

package space.lingu.light.compile.coder.query.binder;

import space.lingu.light.LightRuntimeException;
import space.lingu.light.compile.JavaPoetClass;
import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.coder.query.result.QueryResultConverter;

import java.sql.SQLException;

/**
 * @author RollW
 */
public class InstantQueryResultBinder extends QueryResultBinder {
    public InstantQueryResultBinder(QueryResultConverter mConverter) {
        super(mConverter);
    }

    @Override
    public void writeBlock(String handlerName, String stmtVarName,
                           boolean canReleaseSet, boolean isReturn,
                           boolean inTransaction,
                           GenerateCodeBlock block) {
        if (inTransaction) {
            block.builder().addStatement("$N.beginTransaction()", handlerName);
        }
        final String outVar = block.getTempVar("_result");
        final String setVar = block.getTempVar("_resultSet");

        block.builder().beginControlFlow("try ($T $L = $N.executeQuery())",
                        JavaPoetClass.JdbcNames.RESULT_SET, setVar, stmtVarName);
        if (isReturn) {
            mConverter.convert(outVar, setVar, block);
        }
        if (inTransaction) {
            block.builder().addStatement("$N.endTransaction()", handlerName);
        }
        if (isReturn) {
            block.builder().addStatement("return $L", outVar);
        }
        block.builder()
                .nextControlFlow("catch($T e)", SQLException.class)
                .addStatement("throw new $T(e)", LightRuntimeException.class);

        if (canReleaseSet) {
            block.builder()
                    .nextControlFlow("finally")
                    .addStatement("$N.release($L)", handlerName, stmtVarName);
        }
        block.builder().endControlFlow();
    }
}
