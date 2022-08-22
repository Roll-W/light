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

import space.lingu.light.Update;
import space.lingu.light.compile.CompileErrors;
import space.lingu.light.compile.LightCompileException;
import space.lingu.light.compile.coder.annotated.translator.DeleteUpdateMethodTranslator;
import space.lingu.light.compile.coder.annotated.binder.DirectDeleteUpdateMethodBinder;
import space.lingu.light.compile.javac.ProcessEnv;
import space.lingu.light.compile.struct.AnnotateParameter;
import space.lingu.light.compile.struct.ParamEntity;
import space.lingu.light.compile.struct.UpdateMethod;
import space.lingu.light.util.Pair;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;

/**
 * 更新方法处理器
 *
 * @author RollW
 */
public class UpdateMethodProcessor implements Processor<UpdateMethod> {
    private final ExecutableElement mExecutable;
    private final TypeElement mContaining;
    private final ProcessEnv mEnv;
    private final UpdateMethod method = new UpdateMethod();

    public UpdateMethodProcessor(ExecutableElement element,
                                 TypeElement containing,
                                 ProcessEnv env) {
        mExecutable = element;
        mContaining = containing;
        mEnv = env;
    }

    @Override
    public UpdateMethod process() {
        AnnotateMethodProcessor delegate = new AnnotateMethodProcessor(mExecutable, mEnv);

        Update updateAnno = mExecutable.getAnnotation(Update.class);
        if (updateAnno == null) {
            throw new LightCompileException("A update method must be annotated with @Update.");
        }
        DaoProcessor.sHandleAnnotations.forEach(anno -> {
            if (anno != Update.class && mExecutable.getAnnotation(anno) != null) {
                mEnv.getLog().error(
                        CompileErrors.DUPLICATED_METHOD_ANNOTATION,
                        mExecutable
                );
            }
        });
        Pair<Map<String, ParamEntity>, List<AnnotateParameter>> pair =
                delegate.extractParameters(mContaining);

        method.setElement(mExecutable)
                .setEntities(pair.first)
                .setOnConflict(updateAnno.onConflict())
                .setParameters(pair.second)
                .setReturnType(mExecutable.getReturnType());
        DeleteUpdateMethodTranslator translator =
                DeleteUpdateMethodTranslator.create(
                        method.getReturnType(),
                        method.getParameters());
        if (translator == null) {
            mEnv.getLog().error(CompileErrors.UPDATE_INVALID_RETURN, mExecutable);
        }
        return method.setBinder(
                new DirectDeleteUpdateMethodBinder(translator)
        );
    }
}
