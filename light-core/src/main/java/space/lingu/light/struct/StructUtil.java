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

package space.lingu.light.struct;

import space.lingu.light.LightRuntimeException;

import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public final class StructUtil {
    public static TableColumn findByName(String columnName, List<TableColumn> columns) {
        for (TableColumn column : columns) {
            if (Objects.equals(columnName, column.getName())) {
                return column;
            }
        }
        throw new LightRuntimeException("Not found column by given name, probably you write a wrong name. Column name: " + columnName);
    }

    private StructUtil() {
    }
}
