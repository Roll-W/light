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

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.List;

/**
 * DAO
 *
 * @author RollW
 */
public class Dao {
    private final TypeElement element;
    private ClassName className;
    private final String simpleName;

    private final List<InsertMethod> insertMethods;
    private final List<UpdateMethod> updateMethods;
    private final List<DeleteMethod> deleteMethods;
    private final List<QueryMethod> queryMethods;
    private final List<TransactionMethod> transactionMethods;

    private final ClassName implClassName;
    private final String implName;

    public Dao(TypeElement element,
               String simpleName,
               ClassName implClassName,
               String implName,
               List<InsertMethod> insertMethods,
               List<UpdateMethod> updateMethods,
               List<DeleteMethod> deleteMethods,
               List<QueryMethod> queryMethods,
               List<TransactionMethod> transactionMethods) {
        this.element = element;
        this.simpleName = simpleName;
        this.implClassName = implClassName;
        this.implName = implName;
        this.insertMethods = Collections.unmodifiableList(insertMethods);
        this.updateMethods = Collections.unmodifiableList(updateMethods);
        this.deleteMethods = Collections.unmodifiableList(deleteMethods);
        this.queryMethods = Collections.unmodifiableList(queryMethods);
        this.transactionMethods = Collections.unmodifiableList(transactionMethods);
    }

    public ClassName getClassName() {
        if (className == null) {
            className = ClassName.get(element);
        }
        return className;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public ClassName getImplClassName() {
        return implClassName;
    }

    public List<TransactionMethod> getTransactionMethods() {
        return transactionMethods;
    }

    public TypeElement getElement() {
        return element;
    }

    public List<InsertMethod> getInsertMethods() {
        return insertMethods;
    }

    public List<UpdateMethod> getUpdateMethods() {
        return updateMethods;
    }

    public List<DeleteMethod> getDeleteMethods() {
        return deleteMethods;
    }

    public List<QueryMethod> getQueryMethods() {
        return queryMethods;
    }

    public String getImplName() {
        return implName;
    }
}
