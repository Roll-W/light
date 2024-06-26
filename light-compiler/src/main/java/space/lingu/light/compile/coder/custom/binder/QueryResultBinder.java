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

package space.lingu.light.compile.coder.custom.binder;

import space.lingu.light.LightRuntimeException;
import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.coder.custom.QueryContext;
import space.lingu.light.compile.coder.custom.result.QueryResultConverter;

import java.sql.SQLException;

/**
 * @author RollW
 */
public abstract class QueryResultBinder {
    protected final QueryResultConverter converter;

    public QueryResultBinder(QueryResultConverter converter) {
        this.converter = converter;
    }


    public abstract void writeBlock(String handlerName,
                                    String connVarName,
                                    String stmtVarName,
                                    boolean canReleaseSet,
                                    boolean isReturn,
                                    boolean inTransaction,
                                    GenerateCodeBlock block);

    protected void end(QueryContext queryContext,
                       GenerateCodeBlock block) {
        if (queryContext.isInTransaction()) {
            block.builder().addStatement("$N.commit()", queryContext.getConnVarName());
        }
        if (queryContext.isNeedsReturn()) {
            block.builder().addStatement("return $L", queryContext.getOutVarName());
        }
        block.builder().nextControlFlow("catch ($T e)", SQLException.class);
        if (queryContext.isInTransaction()) {
            block.builder().addStatement("$N.rollback()", queryContext.getConnVarName());
        }
        block.builder().addStatement("throw new $T(e)", LightRuntimeException.class);
        if (queryContext.isCanReleaseSet()) {
            block.builder()
                    .nextControlFlow("finally")
                    .addStatement("$N.release($L)",
                            queryContext.getHandlerVarName(), queryContext.getConnVarName());
        }
        block.builder().endControlFlow();
    }
}
