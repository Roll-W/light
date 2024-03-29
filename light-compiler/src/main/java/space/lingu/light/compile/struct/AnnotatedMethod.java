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

import space.lingu.light.compile.coder.annotated.binder.AnnotatedMethodBinder;
import space.lingu.light.compile.javac.MethodCompileType;
import space.lingu.light.compile.javac.TypeCompileType;

import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
public interface AnnotatedMethod<P extends Parameter> extends Method<P> {
    @Override
    MethodCompileType getMethodCompileType();

    Map<String, ParamEntity> getEntities();

    AnnotatedMethodBinder getBinder();

    @Override
    List<P> getParameters();

    @Override
    TypeCompileType getReturnType();
}
