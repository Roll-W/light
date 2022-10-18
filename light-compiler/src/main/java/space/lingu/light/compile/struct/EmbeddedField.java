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

/**
 * An embedded field.
 *
 * @author RollW
 */
public class EmbeddedField {
    private final Field field;
    private final String prefix;
    private final EmbeddedField parent;
    private Pojo pojo;

    public EmbeddedField(Field field,
                         String prefix,
                         EmbeddedField parent) {
        this.field = field;
        this.prefix = prefix;
        this.parent = parent;
    }

    public Field getField() {
        return field;
    }

    public String getPrefix() {
        return prefix;
    }

    public EmbeddedField getParent() {
        return parent;
    }

    public Pojo getPojo() {
        return pojo;
    }

    public void setPojo(Pojo pojo) {
        this.pojo = pojo;
    }

    public FieldGetter getGetter() {
        return field.getGetter();
    }

    public FieldSetter getSetter() {
        return field.getSetter();
    }

    public EmbeddedField getRootEmbeddedField() {
        if (parent == null) {
            return this;
        }
        return parent.getRootEmbeddedField();
    }
}
