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

package space.lingu.light.log;

import space.lingu.light.LightLogger;

/**
 * Logger with no output.
 *
 * @author RollW
 */
public final class LightEmptyLogger implements LightLogger {
    private LightEmptyLogger() {
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void setDebugEnable(boolean isDebug) {
    }

    @Override
    public void debug(String message) {
    }

    @Override
    public void debug(String message, Throwable throwable) {
    }

    @Override
    public void error(String message) {
    }

    @Override
    public void error(String message, Throwable throwable) {
    }

    @Override
    public void error(Throwable throwable) {
    }

    @Override
    public void info(String message) {
    }

    @Override
    public void info(String message, Throwable throwable) {
    }

    @Override
    public void trace(String message) {
    }

    @Override
    public void trace(String message, Throwable throwable) {
    }

    @Override
    public void trace(Throwable throwable) {
    }

    @Override
    public void warn(String message) {
    }

    @Override
    public void warn(String message, Throwable throwable) {
    }

    public static LightEmptyLogger getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static final class SingletonHolder {
        static final LightEmptyLogger INSTANCE = new LightEmptyLogger();
    }
}
