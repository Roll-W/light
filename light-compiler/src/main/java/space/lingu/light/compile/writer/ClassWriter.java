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

package space.lingu.light.compile.writer;

import com.squareup.javapoet.*;
import space.lingu.light.compile.LightCompileException;
import space.lingu.light.compile.LightProcessor;
import space.lingu.light.compile.javac.ProcessEnv;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import javax.annotation.processing.Filer;
import javax.tools.Diagnostic;

/**
 * 类生成器
 * @author RollW
 */
public abstract class ClassWriter {
    public static final String CLASS_MEMBER_PREFIX = "__";
    public static final String CLASS_SUFFIX = "_Impl";
    public static final String NOTE = "<p><strong>Generated by Lingu Light.</strong></p>\n\n" +
            "<p>\nThe copyright of Light belongs to Lingu Light project.\n</p>\n\n<br>\n" +
            "<p>Note: do not modify the generated file.</p>";

    private static final String JDK_VERSION = System.getProperty("java.version");

    protected ClassName className;
    protected Filer filer;
    protected ProcessEnv mEnv;

    protected final Map<String, FieldSpec> sharedFieldSpecs = new HashMap<>();
    protected final Map<String, MethodSpec> sharedMethodSpecs = new HashMap<>();
    protected final Set<String> sharedFieldNames = new HashSet<>();
    protected final Set<String> sharedMethodNames = new HashSet<>();

    protected abstract TypeSpec.Builder createTypeSpecBuilder();

    public FieldSpec getOrCreateField(SharedFieldSpec fieldSpec) {
        if (sharedFieldSpecs.get(fieldSpec.getUniqueKey()) == null) {
            sharedFieldSpecs.put(fieldSpec.getUniqueKey(),
                    fieldSpec.build(this, makeUniqueName(
                            sharedFieldNames, fieldSpec.baseName)));
        }
        return sharedFieldSpecs.get(fieldSpec.getUniqueKey());
    }

    public MethodSpec getOrCreateMethod(SharedMethodSpec methodSpec) {
        if (sharedMethodSpecs.get(methodSpec.getUniqueKey()) == null) {
            sharedMethodSpecs.put(methodSpec.getUniqueKey(),
                    methodSpec.build(this, makeUniqueName(
                            sharedMethodNames, methodSpec.baseName)));
        }
        return sharedMethodSpecs.get(methodSpec.getUniqueKey());
    }


    private String makeUniqueName(Set<String> set, String value) {
        if (!value.startsWith(CLASS_MEMBER_PREFIX)) {
            return makeUniqueName(set, CLASS_MEMBER_PREFIX + value);
        }
        if (set.add(value)) {
            return value;
        }
        int idx = 1;
        while (true) {
            if (set.add(value + "_" + idx)) {
                return value + "_" + idx;
            }
            idx++;
        }
    }

    public ClassWriter(ClassName className, ProcessEnv env) {
        this.className = className;
        mEnv = env;
        filer = env.getFiler();
    }

    public void write() {
        TypeSpec.Builder builder = createTypeSpecBuilder();

        addGenerated(builder);
        addSuppressWarnings(builder);
        addNote(builder);

        sharedFieldSpecs.values().forEach(builder::addField);
        sharedMethodSpecs.values().forEach(builder::addMethod);

        try {
            JavaFile.builder(className.packageName(), builder.build())
                    .build()
                    .writeTo(filer);
        } catch (IOException e) {
            mEnv.getLog().error("Generate java file failed");
            throw new LightCompileException("Generate java file failed.", e);
        }
    }

    private void addNote(TypeSpec.Builder builder) {
        builder.addJavadoc(NOTE);
    }

    private void addSuppressWarnings(TypeSpec.Builder builder) {
        AnnotationSpec anno = AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "{$S, $S}", "unchecked", "deprecation")
                .build();
        builder.addAnnotation(anno);
    }

    private void addGenerated(TypeSpec.Builder builder) {
        ClassName generated;
        if (getJdkVersion() < 9) {
            generated = ClassName.get("javax.annotation", "Generated");
        } else {
            generated = ClassName.get("javax.annotation.processing", "Generated");
        }
        AnnotationSpec anno = AnnotationSpec.builder(generated)
                .addMember("value", "$S", LightProcessor.class.getCanonicalName())
                .build();
        builder.addAnnotation(anno);
    }

    private static int getJdkVersion() {
        // 1.8 -> 8
        // 11 -> 11
        // higher than 11 we don't consider it
        if (JDK_VERSION.contains("1.7.")) {
            return 7;
        }
        if (JDK_VERSION.contains("1.8.")) {
            return 8;
        }
        if (JDK_VERSION.startsWith("9.")) {
            return 9;
        }
        if (JDK_VERSION.startsWith("10.")) {
            return 10;
        }
        if (JDK_VERSION.startsWith("11.")) {
            return 11;
        }

        return 12;
    }


    public abstract static class SharedFieldSpec {
        final String baseName;
        final TypeName type;

        SharedFieldSpec(String baseName, TypeName type) {
            this.baseName = baseName;
            this.type = type;
        }

        abstract String getUniqueKey();

        abstract void prepare(ClassWriter writer, FieldSpec.Builder builder);

        FieldSpec build(ClassWriter writer, String name) {
            FieldSpec.Builder builder = FieldSpec.builder(type, name);
            prepare(writer, builder);
            return builder.build();
        }

    }

    public abstract static class SharedMethodSpec {
        private final String baseName;

        protected SharedMethodSpec(String baseName) {
            this.baseName = baseName;
        }

        protected abstract String getUniqueKey();

        protected abstract void prepare(String methodName, ClassWriter writer, MethodSpec.Builder builder);

        MethodSpec build(ClassWriter writer, String name) {
            MethodSpec.Builder builder = MethodSpec.methodBuilder(name);
            prepare(name, writer, builder);
            return builder.build();
        }
    }
}
