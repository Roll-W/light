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

package space.lingu.light.compile.struct;

import space.lingu.light.compile.javac.TypeCompileType;
import space.lingu.light.compile.javac.VariableCompileType;

/**
 * @author RollW
 */
public class AnnotateParameter implements Parameter {
    private final VariableCompileType variableCompileType;
    private final TypeCompileType wrapperCompileType;
    private final boolean isMultiple;

    public AnnotateParameter(VariableCompileType variableCompileType,
                             TypeCompileType wrapperCompileType,
                             boolean isMultiple) {
        this.variableCompileType = variableCompileType;
        this.wrapperCompileType = wrapperCompileType;
        this.isMultiple = isMultiple;
    }

    @Override
    public String getName() {
        return variableCompileType.getName();
    }

    @Override
    public VariableCompileType getCompileType() {
        return variableCompileType;
    }

    @Override
    public boolean isMultiple() {
        return isMultiple;
    }

    @Override
    public TypeCompileType getWrappedCompileType() {
        return wrapperCompileType;
    }
}
