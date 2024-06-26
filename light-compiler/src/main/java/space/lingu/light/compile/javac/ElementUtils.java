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

package space.lingu.light.compile.javac;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public final class ElementUtils {
    public static boolean equalTypeElement(TypeElement f, TypeElement s) {
        if (f == null || s == null) {
            return false;
        }
        if (f == s) {
            return true;
        }
        final String firstQualified = f.getQualifiedName().toString();
        final String secondQualified = s.getQualifiedName().toString();
        return Objects.equals(firstQualified, secondQualified);
    }

    public static PackageElement getPackage(Element element) {
        while(element.getKind() != ElementKind.PACKAGE) {
            element = element.getEnclosingElement();
        }

        return (PackageElement) element;
    }

    public static boolean isFinal(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    public static boolean isPublic(Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC);
    }

    public static boolean isProtected(Element element) {
        return element.getModifiers().contains(Modifier.PROTECTED);
    }

    public static boolean isTransient(Element element) {
        return element.getModifiers().contains(Modifier.TRANSIENT);
    }

    public static boolean isStatic(Element element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    public static boolean isPrivate(Element element) {
        return element.getModifiers().contains(Modifier.PRIVATE);
    }

    public static boolean isAbstract(Element element) {
        return element.getModifiers().contains(Modifier.ABSTRACT);
    }

    public static boolean isDefault(Element element) {
        return element.getModifiers().contains(Modifier.DEFAULT);
    }

    public static boolean isInterface(Element element) {
        return element.getKind() == ElementKind.INTERFACE;
    }

    public static boolean isClass(Element element) {
        return element.getKind() == ElementKind.CLASS;
    }

    public static boolean isLong(Element element) {
        return isLongBoxed(element) || isLongUnboxed(element);
    }

    public static boolean isLongUnboxed(Element element) {
        return element.asType().getKind() == TypeKind.LONG;
    }

    public static boolean isLongBoxed(Element element) {
        if (!(element instanceof TypeElement)) {
            return false;
        }
        return ClassName.get((TypeElement) element).equals(TypeName.LONG.box());
    }

    public static boolean isVoidBoxed(Element element) {
        if (!(element instanceof TypeElement)) {
            return false;
        }
        return ClassName.get((TypeElement) element).equals(TypeName.VOID.box());
    }

    public static boolean isCollection(Element element) {
        if (element == null) {
            return false;
        }
        return isTypeOf(Collection.class, element);
    }

    public static boolean isIterable(Element element) {
        if (element == null) {
            return false;
        }
        return isTypeOf(Iterable.class, element);
    }

    public static boolean isList(Element element) {
        if (element == null) {
            return false;
        }
        return isTypeOf(List.class, element);
    }

    public static boolean isTypeOf(Class<?> clazz, Element element) {
        if (element == null) {
            return false;
        }
        return MoreTypes.isTypeOf(clazz, element.asType());
    }

    public static boolean canConvertToTypeElement(TypeMirror mirror) {
        return !TypeUtils.isPrimitive(mirror);
    }

    public static TypeElement asTypeElement(TypeMirror mirror) {
        try {
            return MoreTypes.asTypeElement(mirror);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static List<TypeElement> getGenericElements(TypeMirror mirror) {
        // TypeElement will lose infos
        List<TypeElement> typeElementList = new ArrayList<>();
        List<? extends TypeMirror> typeMirrors = MoreTypes.asDeclared(mirror)
                .getTypeArguments();
        typeMirrors.forEach(typeMirror ->
            typeElementList.add(asTypeElement(typeMirror)));
        return typeElementList;
    }


    private ElementUtils() {
    }
}
