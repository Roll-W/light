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

import com.google.auto.common.MoreTypes;
import space.lingu.light.compile.CompileErrors;
import space.lingu.light.compile.javac.ElementUtils;
import space.lingu.light.compile.javac.ProcessEnv;
import space.lingu.light.compile.javac.TypeCompileType;
import space.lingu.light.compile.javac.TypeUtils;
import space.lingu.light.compile.javac.VariableCompileType;
import space.lingu.light.compile.struct.AnnotateParameter;
import space.lingu.light.compile.writer.ClassWriter;
import space.lingu.light.util.Pair;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
public class AnnotateParameterProcessor implements Processor<AnnotateParameter> {
    private final VariableCompileType variableCompileType;
    private final TypeCompileType containing;
    private final ProcessEnv env;

    public AnnotateParameterProcessor(VariableCompileType compileType,
                                      TypeCompileType containing,
                                      ProcessEnv env) {
        this.variableCompileType = compileType;
        this.env = env;
        this.containing = containing;
    }

    @Override
    public AnnotateParameter process() {
        final String name = variableCompileType.getName();

        if (name.startsWith(ClassWriter.CLASS_MEMBER_PREFIX)) {
            env.getLog().error(CompileErrors.PARAM_NON_COMPLIANCE, variableCompileType);
        }
        Pair<TypeCompileType, Boolean> pair = extractPojo(
                variableCompileType.getTypeMirror());
        TypeCompileType wrappedType = pair.first;
        return new AnnotateParameter(
                variableCompileType,
                wrappedType,
                pair.second
        );
    }

    private TypeCompileType extractTypeFromIterator(TypeElement type,
                                                    DeclaredType declaredType) {
        List<? extends Element> elements = type.getEnclosedElements();
        for (Element e : elements) {
            if (isPublicMethod(e) && e.getSimpleName().contentEquals("iterator")) {
                TypeMirror asMember = env.getTypeUtils()
                        .asMemberOf(declaredType, e);
                return getGenericTypes(
                        TypeUtils.asExecutable(asMember).getReturnType()).get(0);
            }
        }
        env.getLog().error(
                CompileErrors.typeNotIterator(type),
                variableCompileType);
        return null;
    }

    public List<TypeCompileType> getGenericTypes(TypeMirror mirror) {
        List<TypeCompileType> typeElementList = new ArrayList<>();
        List<? extends TypeMirror> typeMirrors = MoreTypes.asDeclared(mirror).getTypeArguments();
        typeMirrors.forEach(typeMirror -> {
            typeElementList.add(env.getTypeCompileType(typeMirror));
        });
        return typeElementList;
    }

    private boolean isPublicMethod(Element e) {
        return ElementUtils.isPublic(e) && !ElementUtils.isStatic(e) &&
                e.getKind() == ElementKind.METHOD;
    }

    private Pair<TypeCompileType, Boolean> extractPojo(TypeMirror typeMirror) {
        if (TypeUtils.isArray(typeMirror)) {
            // It must be able to convert to TypeElement,
            // or its parameter types do not follow the rules
            TypeMirror innerMirror = TypeUtils.getArrayElementType(typeMirror);
            TypeCompileType compileType = env.getTypeCompileType(innerMirror);
            return Pair.createPair(compileType, true);
        }

        DeclaredType declaredType = TypeUtils.asDeclared(typeMirror);
        if (declaredType.getTypeArguments() == null || declaredType.getTypeArguments().isEmpty()) {
            TypeCompileType compileType = env.getTypeCompileType(typeMirror);

            return Pair.createPair(compileType, false);
        }
        TypeElement iterEle = env.getElementUtils().getTypeElement("java.lang.Iterable");
        TypeCompileType compileType = extractTypeFromIterator(iterEle, declaredType);
        return Pair.createPair(compileType, true);
    }
}
