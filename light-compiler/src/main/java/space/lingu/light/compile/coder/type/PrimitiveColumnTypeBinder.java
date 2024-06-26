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


import com.squareup.javapoet.TypeName;
import space.lingu.light.LightRuntimeException;
import space.lingu.light.SQLDataType;
import space.lingu.light.compile.coder.ColumnTypeBinder;
import space.lingu.light.compile.coder.ColumnValueReader;
import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.coder.StatementBinder;
import space.lingu.light.compile.javac.ProcessEnv;
import space.lingu.light.compile.javac.TypeCompileType;
import space.lingu.light.compile.struct.ParseDataType;
import space.lingu.light.util.StringUtils;

import javax.lang.model.type.TypeKind;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Primitive type binder.
 *
 * @author RollW
 */
public class PrimitiveColumnTypeBinder extends ColumnTypeBinder implements StatementBinder, ColumnValueReader {
    private final String getter;
    private final String setter;
    private final String defaultValue;
    private final TypeName boxedName;

    public PrimitiveColumnTypeBinder(TypeCompileType type,
                                     String stmtSetter,
                                     String resSetGetter,
                                     SQLDataType dataType,
                                     String defaultValue) {
        super(type, dataType);
        this.getter = resSetGetter;
        this.setter = stmtSetter;
        this.defaultValue = defaultValue;
        this.boxedName = typeName.box();
    }

    private static final String PREFIX_GET = "get";

    private String cast(boolean primitive) {
        // TODO: add a getterOfField method
        if (getter.equals(PREFIX_GET +
                StringUtils.firstUpperCase(typeName.toString()))) {
            return "";
        }

        return forceCast(primitive);
    }

    private String forceCast(boolean primitive) {
        if (primitive) {
            return "(" + typeName + ") ";
        }
        return "(" + boxedName + ") ";
    }

    @Override
    public void readFromResultSet(String outVarName,
                                  String resultSetName,
                                  String indexName,
                                  GenerateCodeBlock block) {
        readFromResultSet(outVarName, resultSetName, indexName, block,
                true, false);
    }

    protected void readFromResultSet(String outVarName,
                                     String resultSetName,
                                     String indexName,
                                     GenerateCodeBlock block,
                                     boolean checkColumn,
                                     boolean allowBoxedValue) {
        int index = parseIndex(indexName);
        boolean check = checkColumn && index < 0;
        if (check) {
            block.builder()
                    .beginControlFlow("if ($L < 0)", indexName)
                    .addStatement("$L = $L", outVarName, defaultValue)
                    .nextControlFlow("else");
        }
        if (!allowBoxedValue) {
            block.builder().addStatement("$L = $L$L.$L($L)",
                    outVarName, cast(true), resultSetName,
                    getter, indexName);
        } else {
            block.builder().addStatement("$L = $L.getObject($L, $T.class)",
                    outVarName, resultSetName, indexName, boxedName);
        }

        if (check) {
            block.builder().endControlFlow();
        }
    }

    @Override
    public void bindToStatement(String stmtVarName, String indexVarName, String valueVarName, GenerateCodeBlock block) {
        block.builder()
                .beginControlFlow("try")
                .addStatement("$L.$L($L, $L)", stmtVarName, setter, indexVarName, valueVarName)
                .nextControlFlow("catch ($T e)", SQLException.class)
                .addStatement("throw new $T(e)", LightRuntimeException.class)
                .endControlFlow();
    }

    private static int parseIndex(String indexVarName) {
        try {
            return Integer.parseInt(indexVarName);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static List<PrimitiveColumnTypeBinder> create(ProcessEnv env) {
        List<PrimitiveColumnTypeBinder> binderList = new ArrayList<>();
        CANDIDATE_TYPE_MAPPING.forEach((parseDataType, info) -> {
            SQLDataType sqlDataType;
            switch (parseDataType) {
                case SHORT:
                case INT:
                case BYTE:
                    sqlDataType = SQLDataType.INT;
                    break;
                case LONG:
                    sqlDataType = SQLDataType.LONG;
                    break;
                case DOUBLE:
                    sqlDataType = SQLDataType.DOUBLE;
                    break;
                case FLOAT:
                    sqlDataType = SQLDataType.FLOAT;
                    break;
                case CHAR:
                    sqlDataType = SQLDataType.CHAR;
                    break;
                case BOOLEAN:
                    sqlDataType = SQLDataType.BOOLEAN;
                    break;
                default: {
                    throw new IllegalArgumentException("Illegal type of " + parseDataType);
                }
            }

            binderList.add(new PrimitiveColumnTypeBinder(
                    info.getType(env),
                    info.setter, info.getter,
                    sqlDataType,
                    info.defaultValue)
            );
        });
        return binderList;
    }

    private static final Map<ParseDataType, Info> CANDIDATE_TYPE_MAPPING = new EnumMap<>(ParseDataType.class);

    static {
        CANDIDATE_TYPE_MAPPING.put(ParseDataType.INT, new Info(TypeKind.INT, "setInt", "getInt", "0"));
        CANDIDATE_TYPE_MAPPING.put(ParseDataType.SHORT, new Info(TypeKind.SHORT, "setShort", "getShort", "0"));
        CANDIDATE_TYPE_MAPPING.put(ParseDataType.LONG, new Info(TypeKind.LONG, "setLong", "getLong", "0L"));
        CANDIDATE_TYPE_MAPPING.put(ParseDataType.CHAR, new Info(TypeKind.CHAR, "setChar", "getChar", "0"));
        CANDIDATE_TYPE_MAPPING.put(ParseDataType.BYTE, new Info(TypeKind.BYTE, "setByte", "getByte", "0"));
        CANDIDATE_TYPE_MAPPING.put(ParseDataType.DOUBLE, new Info(TypeKind.DOUBLE, "setDouble", "getDouble", "0"));
        CANDIDATE_TYPE_MAPPING.put(ParseDataType.FLOAT, new Info(TypeKind.FLOAT, "setFloat", "getFloat", "0F"));
        CANDIDATE_TYPE_MAPPING.put(ParseDataType.BOOLEAN, new Info(TypeKind.BOOLEAN, "setBoolean", "getBoolean", "false"));
    }

    private static class Info {
        final TypeKind typeKind;
        final String setter;
        final String getter;
        final String defaultValue;

        private Info(TypeKind typeKind, String setter,
                     String getter, String defaultValue) {
            this.typeKind = typeKind;
            this.setter = setter;
            this.getter = getter;
            this.defaultValue = defaultValue;
        }

        TypeCompileType getType(ProcessEnv env) {
            return env.getTypeCompileType(typeKind);
        }
    }

}
