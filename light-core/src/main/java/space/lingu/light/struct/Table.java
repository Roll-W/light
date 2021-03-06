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

import space.lingu.light.Index;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
public class Table {
    /**
     * Table name
     */
    private String name;

    /**
     * Table columns
     */
    private final List<TableColumn> columns = new ArrayList<>();

    private TablePrimaryKey primaryKey;
    /**
     * Table indices
     */
    private final List<TableIndex> indices = new ArrayList<>();

    public Table() {
        name = "";
    }

    public Table(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Table setName(String name) {
        this.name = name;
        return this;
    }

    public List<TableColumn> getColumns() {
        return columns;
    }

    public Table setColumns(List<TableColumn> columns) {
        this.columns.clear();
        this.columns.addAll(columns);
        return this;
    }

    public List<TableIndex> getIndices() {
        return indices;
    }

    public Table setIndices(List<TableIndex> indices) {
        this.indices.clear();
        this.indices.addAll(indices);
        return this;
    }



    @Override
    public String toString() {
        return "Table{" +
                "name='" + name + '\'' +
                ", columns=" + columns +
                ", indices=" + indices +
                '}';
    }
}
