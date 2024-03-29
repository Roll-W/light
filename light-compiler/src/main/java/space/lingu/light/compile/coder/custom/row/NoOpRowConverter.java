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

package space.lingu.light.compile.coder.custom.row;

import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.coder.custom.QueryContext;
import space.lingu.light.compile.javac.TypeCompileType;

/**
 * @author RollW
 */
public class NoOpRowConverter extends RowConverter {
    public NoOpRowConverter(TypeCompileType outType) {
        super(outType);
    }

    @Override
    public void onResultSetReady(QueryContext queryContext, GenerateCodeBlock block) {
        // no-op
    }


    @Override
    public void convert(QueryContext queryContext, GenerateCodeBlock block) {
        // no-op
    }

    @Override
    public void onResultSetFinish(GenerateCodeBlock block) {
        // no-op
    }
}
