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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 共享{@code PreparedStatement}减少内存和时间开销，同时保证线程安全。
 * @author RollW
 */
public abstract class SharedSQLStatement {
    private final AtomicBoolean mLock = new AtomicBoolean(false);

    protected final LightDatabase mDatabase;
    protected final SharedConnection mSharedConnection;

    private volatile PreparedStatement mStatement;
    private volatile Connection mConnection;

    public SharedSQLStatement(LightDatabase database) {
        mDatabase = database;
        mSharedConnection = new SharedConnection(database);
    }

    protected abstract String createQuery();

    public boolean supportsTransaction() {
        return mSharedConnection.getMetadata().supportsTransaction;
    }

    public boolean supportsBatch() {
        return mSharedConnection.getMetadata().supportsBatch;
    }

    public void beginTransaction() {
        mSharedConnection.beginTransaction();
    }

    public void endTransaction() {
        mSharedConnection.commit();
    }

    public void rollback() {
        mSharedConnection.rollback();
    }

    public final PreparedStatement acquire() {
        return getStatement(mLock.compareAndSet(false, true));
    }

    private PreparedStatement getStatement(boolean canUseCached) {
        final PreparedStatement stmt;
        if (canUseCached) {
            if (mStatement == null) {
                mStatement = createNewStatement();
            }
            stmt = mStatement;
        } else {
            stmt = createNewStatement();
        }
        return stmt;
    }


    private PreparedStatement createNewStatement() {
        mConnection = mSharedConnection.acquire();
        String query = createQuery();
        return mDatabase.resolveStatement(query, mConnection, true);
    }

    public final void release(PreparedStatement statement) {
        if (statement == mStatement) {
            mLock.set(false);
            mSharedConnection.release(mConnection);
        }
    }
}
