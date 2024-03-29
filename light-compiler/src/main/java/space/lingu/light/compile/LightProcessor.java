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

package space.lingu.light.compile;

import com.google.auto.service.AutoService;
import space.lingu.light.LightDatabase;
import space.lingu.light.compile.javac.ElementUtils;
import space.lingu.light.compile.javac.JavacBaseProcessor;
import space.lingu.light.compile.processor.DatabaseProcessor;
import space.lingu.light.compile.processor.Processor;
import space.lingu.light.compile.struct.Database;
import space.lingu.light.compile.writer.DatabaseWriter;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Process {@link space.lingu.light.Database} annotation.
 *
 * @author RollW
 */
@AutoService(javax.annotation.processing.Processor.class)
@SupportedAnnotationTypes({"space.lingu.light.Database"})
public class LightProcessor extends JavacBaseProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement element : annotations) {
            Set<? extends Element> annotatedClass = roundEnv.getElementsAnnotatedWith(element);

            for (Element e : annotatedClass) {
                TypeElement classElement = (TypeElement) e;
                if (!classElement.getKind().isClass()) {
                    getEnv().getLog().error(
                            CompileErrors.DATABASE_NOT_CLASS,
                            classElement
                    );
                }

                if (!ElementUtils.isAbstract(classElement)) {
                    getEnv().getLog().error(
                            CompileErrors.DATABASE_NOT_ABSTRACT_CLASS,
                            classElement
                    );
                }

                if (!checkSuperClass(classElement)) {
                    getEnv().getLog().error(
                            CompileErrors.DATABASE_NOT_EXTENDS_BASE,
                            classElement
                    );
                }
                try {
                    Processor<Database> databaseProcessor = new DatabaseProcessor(classElement, env);
                    DatabaseWriter writer = new DatabaseWriter(
                            databaseProcessor.process(),
                            env
                    );
                    writer.write();
                } catch (LightCompileException ex) {
                    getEnv().getLog().error(
                            CompileErrors.buildFailed()
                    );
                    throw ex;
                } catch (Exception ex) {
                    getEnv().getLog().error(CompileErrors.bugReportWithMessage(ex.getMessage()));
                    getEnv().getLog().error(CompileErrors.buildFailed());
                    throw new LightCompileException(ex);
                }
            }
        }
        getEnv().getLog().note(CompileErrors.buildSuccess());
        return roundEnv.processingOver();
    }

    private boolean checkSuperClass(TypeElement element) {
        TypeElement iter = element;
        while (iter.getSuperclass() != null) {
            if (iter.getKind() != ElementKind.CLASS) {
                return false;
            }
            TypeElement e = (TypeElement) typeUtils.asElement(iter.getSuperclass());
            if (e == null || e.getQualifiedName() == null) {
                return false;
            }
            if (e.getQualifiedName().contentEquals(LightDatabase.class.getCanonicalName())) {
                return true;
            }
            iter = e;
        }
        return false;
    }
}
