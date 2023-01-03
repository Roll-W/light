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

package space.lingu.light.compile.processor;

import space.lingu.light.compile.CompileErrors;
import space.lingu.light.compile.javac.ElementUtil;
import space.lingu.light.compile.javac.ProcessEnv;
import space.lingu.light.compile.javac.TypeUtil;
import space.lingu.light.compile.struct.*;
import space.lingu.light.util.Pair;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A general processor that provides
 * general process for parsing method
 * processors that use annotations directly.
 *
 * @author RollW
 */
public class AnnotateMethodProcessor {
    private final ExecutableElement mElement;
    private final ProcessEnv mEnv;

    public AnnotateMethodProcessor(ExecutableElement element, ProcessEnv env) {
        mElement = element;
        mEnv = env;
    }

    public Pair<Map<String, ParamEntity>, List<Parameter>> extractParameters(TypeElement element) {
        List<? extends VariableElement> elements = mElement.getParameters();
        List<Parameter> parameters = new ArrayList<>();
        elements.forEach(e -> {
            checkUnbound(e.asType(), mEnv, e);

            Processor<AnnotateParameter> processor =
                    new AnnotateParameterProcessor(e, element, mEnv);
            parameters.add(processor.process());
        });

        Map<String, ParamEntity> entityMap = new HashMap<>(extractEntities(parameters));
        return Pair.createPair(entityMap, parameters);
    }

    public Map<String, ParamEntity> extractEntities(List<Parameter> params) {
        final Map<String, ParamEntity> entityMap = new HashMap<>();
        params.forEach(param -> {
            if (param == null) {
                return;
            }

            TypeElement entityTypeElement = param.getWrappedType();
            if (TypeUtil.equalTypeMirror(param.getTypeMirror(), param.getWrappedType().asType())) {
                entityTypeElement = param.getType();
            }

            if (entityTypeElement == null) {
                mEnv.getLog().error(
                        CompileErrors.DAO_INVALID_METHOD_PARAMETER,
                        mElement
                );
                return;
            }

            // TODO 支持实体类片段
            Pojo pojo = new PojoProcessor(entityTypeElement, mEnv).process();
            if (entityTypeElement.getAnnotation(space.lingu.light.DataTable.class) == null) {
                mEnv.getLog().error(CompileErrors.ACTUAL_PARAM_ANNOTATED_DATATABLE, mElement);
                return;
            }
            DataTable dataTable = new DataTableProcessor(param.getWrappedType(), mEnv).process();
            ParamEntity paramEntity = new ParamEntity(dataTable, null);
            entityMap.put(param.getName(), paramEntity);
        });

        return entityMap;
    }

    public static void checkUnbound(TypeMirror typeMirror, ProcessEnv env, Element element) {
        TypeElement type = ElementUtil.asTypeElement(typeMirror);
        if (type == null) {
            return;
        }
        if (!ElementUtil.isIterable(type)) {
            return;
        }
        List<? extends TypeMirror> genericTypes = TypeUtil.getGenericTypes(typeMirror);
        if (genericTypes == null || genericTypes.isEmpty()) {
            env.getLog().error(
                    CompileErrors.NOT_BOUND_GENERIC_TYPES,
                    element
            );
        }
    }
}
