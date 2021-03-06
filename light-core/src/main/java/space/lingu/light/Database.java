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

package space.lingu.light;

import space.lingu.light.sql.DialectProvider;

import java.lang.annotation.*;

/**
 * 标注为一个数据库。如存在多个数据库标记，则名称不能相同。
 * 名称标注规范须遵循所用数据库规范。
 * @author RollW
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface Database {
    /**
     * 数据库名称。如存在多个数据库标记，则名称不能相同。
     * 名称标注规范须遵循所用数据库规范。
     * @return 数据库名称
     */
    String name();

    /**
     * 数据连接配置读取路径
     * @return 数据连接配置读取路径
     */
    String datasourceConfig() default "/light.properties";

    int version();

    Class<?>[] tables();

    Class<? extends DialectProvider> dialect() default DialectProvider.class;
}
