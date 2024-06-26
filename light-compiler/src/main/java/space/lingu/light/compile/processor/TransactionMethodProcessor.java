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

import space.lingu.light.compile.coder.annotated.binder.DirectTransactionMethodBinder;
import space.lingu.light.compile.coder.annotated.binder.TransactionMethodBinder;
import space.lingu.light.compile.coder.annotated.translator.TransactionMethodTranslator;
import space.lingu.light.compile.javac.ElementUtils;
import space.lingu.light.compile.javac.MethodCompileType;
import space.lingu.light.compile.javac.ProcessEnv;
import space.lingu.light.compile.javac.TypeCompileType;
import space.lingu.light.compile.struct.TransactionMethod;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
public class TransactionMethodProcessor implements Processor<TransactionMethod> {
    private final MethodCompileType methodCompileType;
    private final TypeCompileType containing;
    private final ProcessEnv env;

    public TransactionMethodProcessor(MethodCompileType methodCompileType,
                                      TypeCompileType containing,
                                      ProcessEnv env) {
        this.methodCompileType = methodCompileType;
        this.containing = containing;
        this.env = env;
    }

    @Override
    public TransactionMethod process() {
        List<String> paramNames = new ArrayList<>();
        methodCompileType.getParameters().forEach(variableElement ->
                paramNames.add(variableElement.getSimpleName().toString()));
        TransactionMethod.CallType callType = getCallType();

        TransactionMethodTranslator transactionMethodTranslator =
                new TransactionMethodTranslator(
                        methodCompileType.getName(),
                        callType
                );
        TransactionMethodBinder binder = new DirectTransactionMethodBinder(
                transactionMethodTranslator);

        return new TransactionMethod(methodCompileType, paramNames, binder, callType);
    }

    private TransactionMethod.CallType getCallType() {
        ExecutableElement executableElement = methodCompileType.getElement();
        TypeElement typeElement = containing.getElement();
        if (!ElementUtils.isDefault(executableElement)) {
            return TransactionMethod.CallType.DIRECT;
        }
        if (ElementUtils.isInterface(typeElement)) {
            return TransactionMethod.CallType.DEFAULT;
        }
        return TransactionMethod.CallType.INHERITED_DEFAULT;
    }
}
