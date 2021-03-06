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

import com.squareup.javapoet.ClassName;
import space.lingu.light.Dao;
import space.lingu.light.DataConverters;
import space.lingu.light.compile.LightCompileException;
import space.lingu.light.compile.javac.ElementUtil;
import space.lingu.light.compile.javac.ProcessEnv;
import space.lingu.light.compile.struct.DataConverter;
import space.lingu.light.compile.struct.Database;
import space.lingu.light.compile.struct.DataTable;
import space.lingu.light.compile.struct.DatabaseDaoMethod;
import space.lingu.light.compile.writer.ClassWriter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * @author RollW
 */
public class DatabaseProcessor implements Processor<Database> {
    private final TypeElement mElement;
    private final Database database = new Database();
    private final space.lingu.light.Database anno;
    private final ProcessEnv mEnv;
    private final List<? extends Element> enclosedElements;

    public DatabaseProcessor(TypeElement element, ProcessEnv env) {
        mElement = element;
        anno = mElement.getAnnotation(space.lingu.light.Database.class);
        mEnv = env;
        enclosedElements = mElement.getEnclosedElements();
    }


    @Override
    public Database process() {
        ClassName superClass = ClassName.get(mElement);
        String packageName = superClass.packageName();
        String implName = superClass.simpleName() + ClassWriter.CLASS_SUFFIX;

        TypeMirror dialectClass = null;
        try {
            Class<?> clz = anno.dialect();
        } catch (MirroredTypeException e) {
            dialectClass = e.getTypeMirror();
        }

        List<TypeMirror> tableClassMirror = new ArrayList<>();
        try {
            Class<?>[] tableClasses = anno.tables();
        } catch (MirroredTypesException e) {
            // it will and should be caught
            tableClassMirror.addAll(e.getTypeMirrors());
        }

        TypeElement dialectElement = (TypeElement) mEnv.getTypeUtils().asElement(dialectClass);

        database.setDataTableList(processDataTables(tableClassMirror))
                .setSuperClassElement(mElement)
                .setImplName(implName)
                .setDatabaseDaoMethods(getAllDaoMethods())
                .setImplClassName(ClassName.get(packageName, implName))
                .setDialectElement(dialectElement);
        return database;
    }

    private List<DataTable> processDataTables(List<? extends TypeMirror> mirrors) {
        if (mirrors == null) {
            throw new LightCompileException("Cannot required data table classes.");
        }
        List<DataTable> dataTableList = new ArrayList<>();
        mirrors.forEach(typeMirror -> {
            TypeElement element = ElementUtil.asTypeElement(typeMirror);
            if (element == null) {
                throw new LightCompileException("Please check datatable classes.");
            }
            dataTableList.add(new DataTableProcessor(element, mEnv).process());
        });

        Set<String> nameSet = new HashSet<>();
        dataTableList.forEach(dataTable -> {
            if (nameSet.contains(dataTable.getTableName())) {
                throw new LightCompileException("Cannot have the same table name!");
            }
            nameSet.add(dataTable.getTableName());
        });

        return dataTableList;
    }

    private List<DataConverter> getDataConverterMethods() {
        DataConverters dataConvertersAnno = mElement.getAnnotation(DataConverters.class);
        List<DataConverter> dataConverterList = new ArrayList<>();
        if (dataConvertersAnno == null) {
            return Collections.emptyList();
        }
        List<? extends TypeMirror> convertersClassMirror = null;
        try {
            Class<?>[] classes = dataConvertersAnno.value();
        } catch (MirroredTypesException e) {
            convertersClassMirror = e.getTypeMirrors();
        }
        convertersClassMirror.forEach(typeMirror -> {
            TypeElement convertersElement = ElementUtil.asTypeElement(typeMirror);
            if (convertersElement == null) {
                throw new LightCompileException("Please check if there any fault in DataConverters annotation.");
            }
            convertersElement.getEnclosedElements().forEach(enclosedElement -> {
                if (enclosedElement.getAnnotation(space.lingu.light.DataConverter.class) == null) {
                    return;
                }
                if (enclosedElement.getKind() == ElementKind.METHOD) {
                    if (!ElementUtil.isStatic(enclosedElement) || !ElementUtil.isPublic(enclosedElement)) {
                        throw new LightCompileException("A DataConverter method must be static and public.");
                    }
                    Processor<DataConverter> converterProcessor =
                            new DataConverterProcessor((ExecutableElement) enclosedElement,
                                    convertersElement, mEnv);
                    dataConverterList.add(converterProcessor.process());
                }
            });
        });
        return dataConverterList;
    }

    private List<DatabaseDaoMethod> getAllDaoMethods() {
        List<DatabaseDaoMethod> daoMethods = new ArrayList<>();

        for (Element e : enclosedElements) {
            if (e.getKind() != ElementKind.METHOD || !ElementUtil.isAbstract(e)) {
                continue;
            }

            ExecutableElement method = (ExecutableElement) e;
            TypeElement returnType = (TypeElement) mEnv.getTypeUtils().asElement(method.getReturnType());
            Dao daoAnno = returnType.getAnnotation(Dao.class);
            if (daoAnno == null) {
                throw new LightCompileException("An abstract method in a database class whose return type " +
                        "must be an abstract class or interface annotated with @Dao");
            }
            DaoProcessor daoProcessor = new DaoProcessor(returnType, mEnv);
            DatabaseDaoMethod daoMethod = new DatabaseDaoMethod()
                    .setDao(daoProcessor.process())
                    .setElement(method);
            daoMethods.add(daoMethod);
        }

        return daoMethods;
        // ???????????????????????????????????????
    }


}
