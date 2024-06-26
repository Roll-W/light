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

import space.lingu.light.compile.coder.GenerateCodeBlock;
import space.lingu.light.compile.coder.custom.QueryContext;
import space.lingu.light.compile.coder.custom.row.RowConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
public abstract class AbstractQueryResultConverter implements QueryResultConverter {
    protected final List<RowConverter> rowConverters = new ArrayList<>();

    public AbstractQueryResultConverter(List<RowConverter> converters) {
        rowConverters.addAll(converters);
    }

    public AbstractQueryResultConverter(RowConverter converter) {
        rowConverters.add(converter);
    }

    public abstract void convert(QueryContext queryContext, GenerateCodeBlock block);
}
