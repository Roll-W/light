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
import com.squareup.javapoet.TypeName;
import space.lingu.light.compile.JavaPoetClass;
import space.lingu.light.compile.MethodNames;
import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.struct.ExpressionBind;
import space.lingu.light.compile.struct.SQLCustomMethod;
import space.lingu.light.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author RollW
 */
public class SQLCustomMethodWriter {
    private final SQLCustomMethod method;

    public SQLCustomMethodWriter(SQLCustomMethod method) {
        this.method = method;
    }

    public void prepare(String stmtVar, String connName,
                        String handlerName,
                        GenerateCodeBlock block) {
        List<Pair<ExpressionBind, String>> listVars = createSqlQueryAndArgs(stmtVar, connName, handlerName, block);
        bindArgs(stmtVar, listVars, block);
    }

    private List<Pair<ExpressionBind, String>> createSqlQueryAndArgs(String outVarName,
                                                                     String connName,
                                                                     String handlerName,
                                                                     GenerateCodeBlock block) {
        List<Pair<ExpressionBind, String>> pairList = new ArrayList<>();
        final String argCountArray = block.getTempVar("_argsCountArray");
        List<String> argsSizeParams = new ArrayList<>();

        method.getExpressionBinds().forEach(bind -> {
            if (!bind.getBinder().isMultiple) {
                argsSizeParams.add("1");
                pairList.add(Pair.createPair(bind, "1"));
                return;
            }
            String argCountSingle = block.getTempVar("_argsCount");
            argsSizeParams.add(argCountSingle);
            bind.getBinder().getArgsCount(bind.getExpression(), argCountSingle, block);
            pairList.add(Pair.createPair(bind, argCountSingle));
        });

        StringBuilder argsArrayInitBuilder = new StringBuilder("{");
        StringJoiner argsArrayInitJoiner = new StringJoiner(", ");

        argsSizeParams.forEach(argsArrayInitJoiner::add);
        argsArrayInitBuilder.append(argsArrayInitJoiner).append("}");

        block.builder()
                .addStatement("final $T $L = $L.$L()", JavaPoetClass.MANAGED_CONNECTION,
                        connName, handlerName,
                        MethodNames.sSQLHandlerNewConnection)
                .addStatement("final $T $L = $L",
                        ArrayTypeName.of(TypeName.INT),
                        argCountArray, argsArrayInitBuilder.toString())
                .addStatement("final $T $L = $L.acquire($L, $L)",
                        JavaPoetClass.JdbcNames.PREPARED_STMT,
                        outVarName, handlerName, connName, argCountArray);
        return pairList;
    }

    void bindArgs(String outName,
                  List<Pair<ExpressionBind, String>> listSizeVars,
                  GenerateCodeBlock block) {
        final String argIndex = block.getTempVar("_argIndex");
        AtomicInteger constInputs = new AtomicInteger();
        List<String> varInputs = new ArrayList<>();
        block.builder().addStatement("$T $L = $L", TypeName.INT, argIndex, 1);

        listSizeVars.forEach(param -> {
            if (constInputs.get() > 0 || !varInputs.isEmpty()) {
                String cons = constInputs.get() > 0
                        ? String.valueOf((constInputs.get() + 1))
                        : "1";
                StringJoiner vars = new StringJoiner("");
                varInputs.forEach(s -> vars.add(" + " + s));
                // _argIndex update

                block.builder().addStatement("$L = $L$L", argIndex, cons, vars.toString());
            }

            // save to a temp variable
            String tempVar = block.getTempVar("_tmpArg");
            block.builder().addStatement("$T $L = $L",
                    param.first.getType().toTypeName(),
                    tempVar, param.first.getExpression());

            param.first.getBinder().bindToStatement(outName, argIndex,
                    tempVar, block);
            List<Pair<ExpressionBind, String>> pairList = listSizeVars.stream()
                    .filter(pair -> pair.first.getExpression().equals(param.first.getExpression()))
                    .collect(Collectors.toList());
            if (pairList.isEmpty()) {
                constInputs.getAndIncrement();
            } else {
                pairList.forEach(queryParameterStringPair ->
                        varInputs.add(queryParameterStringPair.second));
            }
        });
    }

}
