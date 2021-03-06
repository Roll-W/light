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
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 对连接进一步包装
 * @author RollW
 */
public class SharedConnection {
    private final AtomicBoolean mLock = new AtomicBoolean(false);
    private volatile Connection mConnection = null;
    protected final LightDatabase mDatabase;
    private Metadata metadata;

    public SharedConnection(LightDatabase database) {
        mDatabase = database;
    }

    public Connection acquire() {
        return getConnection(mLock.compareAndSet(false, true));
    }

    private Connection getConnection(boolean canUseCached) {
        final Connection conn;
        if (canUseCached) {
            if (mConnection == null) {
                mConnection = requireFromDatabase();
            }
            conn = mConnection;
        } else {
            conn = requireFromDatabase();
        }
        try {
            return conn;
        } finally {
            metadata = new Metadata(supportsBatch(), supportsTransaction());
        }
    }

    private Connection requireFromDatabase() {
        return mDatabase.requireConnection();
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void beginTransaction() {
        if (!checkSupportTransaction()) {
            return;
        }
        autoCommit(false);
    }

    public void commit() {
        if (!checkSupportTransaction()) {
            return;
        }
        try {
            mConnection.commit();
        } catch (SQLException e) {
            throw new LightRuntimeException(e);
        } finally {
            autoCommit(true);
        }
    }

    public void rollback() {
        if (!checkSupportTransaction()) {
            return;
        }
        try {
            mConnection.rollback();
        } catch (SQLException e) {
            throw new LightRuntimeException(e);
        } finally {
            autoCommit(true);
        }
    }

    public void release(Connection connection) {
        if (mConnection == connection) {
            mLock.set(false);
        }
    }

    public void back() {
        mDatabase.releaseConnection(mConnection);
        mConnection = null;
    }

    private void autoCommit(boolean autoCommit) {
        try {
            mConnection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new LightRuntimeException(e);
        }
    }

    private boolean checkSupportTransaction() {
        if (metadata == null) {
            release(acquire());
        }

        return metadata.supportsTransaction;
    }

    private boolean supportsTransaction() {
        try {
            if (mConnection.getMetaData().supportsTransactions()) {
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    private boolean supportsBatch() {
        try {
            if (mConnection.getMetaData().supportsBatchUpdates()) {
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    public static class Metadata {
        public final boolean supportsBatch;
        public final boolean supportsTransaction;

        Metadata(boolean supportsBatch, boolean supportsTransaction) {
            this.supportsBatch = supportsBatch;
            this.supportsTransaction = supportsTransaction;
        }
    }

}
