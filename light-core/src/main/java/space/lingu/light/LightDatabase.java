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


import space.lingu.light.connect.ConnectionPool;
import space.lingu.light.log.JdkDefaultLogger;
import space.lingu.light.sql.DialectProvider;
import space.lingu.light.struct.DatabaseInfo;
import space.lingu.light.struct.Table;
import space.lingu.light.util.StringUtils;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * The base class of the database. Provides basic database management.
 *
 * @author RollW
 */
public abstract class LightDatabase {
    private static final String IMPL_SUFFIX = "_Impl";

    private DialectProvider dialectProvider;
    private ConnectionPool connectionPool;
    private DatasourceConfig sourceConfig;
    private Executor queryExecutor;
    private String name;

    public final DatasourceConfig getDatasourceConfig() {
        return sourceConfig;
    }

    public final ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    private LightLogger logger = JdkDefaultLogger.getGlobalLogger();

    public final LightLogger getLogger() {
        return logger;
    }

    /**
     * Do not implement or call this method in your program!
     */
    protected abstract LightInfo.LightInfoDao _LightInfoDao();

    public void setLogger(LightLogger logger) {
        if (logger == null) {
            return;
        }
        this.logger = logger;
    }

    public LightDatabase() {
    }

    private DatabaseInfo mDatabaseInfo;

    protected void init(DatabaseConfiguration conf) {
        registerAllTables();
        DatasourceConfig rawConfig = conf.datasourceConfig;

        this.name = conf.name;
        this.sourceConfig = rawConfig;
        if (conf.logger != null) {
            this.logger = conf.logger;
        }
        this.dialectProvider = conf.dialectProvider;

        ConnectionPool connectionPool = conf.connectionPool;
        connectionPool.setLogger(logger);
        connectionPool.setDatasourceConfig(sourceConfig);
        this.connectionPool = connectionPool;

        mDatabaseInfo = new DatabaseInfo(name, conf.databaseConfigurations);

        if (!checkContainsDatabase()) {
            createDatabase(mDatabaseInfo);
            String url = dialectProvider.getJdbcUrl(
                    rawConfig.getUrl(),
                    mDatabaseInfo
            );
            logger.debug("Database created, new url: " + url);
            DatasourceConfig newConfig = rawConfig.fork(url);
            sourceConfig = newConfig;
            connectionPool.setDatasourceConfig(newConfig);
        }

        initDatabaseEnv(mDatabaseInfo);

        createTables();
        createIndices();
    }

    private boolean checkContainsDatabase() {
        Connection rawConnection = rawConnection();
        try {
            String catalog = rawConnection.getCatalog();
            return !StringUtils.isEmpty(catalog);
        } catch (SQLException e) {
            throw new LightRuntimeException(e);
        } finally {
            releaseConnection(rawConnection);
        }
    }

    protected void registerAllTables() {
    }

    protected void createDatabase(DatabaseInfo info) {
        final String sql = dialectProvider.create(info);
        if (sql == null) {
            return;
        }

        executeRaw(rawConnection(), sql);
    }

    private void initDatabaseEnv(DatabaseInfo info) {
        String initEnv = dialectProvider.initDatabaseEnvironment(info);
        if (initEnv == null) {
            return;
        }
        executeRaw(rawConnection(), initEnv);
    }

    private void createTables() {
        List<String> statements = mTableStructCache.values()
                .stream().map(table -> {
                    if (Objects.equals(table.getName(), LightInfo.sTableName)) {
                        // not create the [LightInfo] table current until complete verify function.
                        // TODO: remove this while complete.
                        return null;
                    }
                    return dialectProvider.create(table);
                })
                .collect(Collectors.toList());
        for (String statement : statements) {
            if (logger != null) {
                logger.debug("Execute create table statement, statement: " + statement);
            }
            executeRawSqlWithNoReturn(statement);
        }
    }

    private void createIndices() throws LightIndexCreateException {
        List<String> statements = mTableStructCache.values()
                .stream().map(table -> {
                    if (Objects.equals(table.getName(), LightInfo.sTableName)) {
                        // not create the [LightInfo] table current until complete verify function.
                        return new ArrayList<String>();
                    }
                    return table.getIndices().stream().map(index ->
                                    dialectProvider.create(index))
                            .collect(Collectors.toList());
                })
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
        for (String statement : statements) {
            if (logger != null) {
                logger.debug("Execute create index statement, statement: " + statement);
            }
            try {
                executeRawSqlWithNoReturn(statement);
            } catch (LightRuntimeException e) {
                int errorCode = ((SQLException) e.getCause()).getErrorCode();
                if (errorCode == 1061) {
                    // 1061 - MySQL Error: ER_DUP_KEYNAME
                    //
                    // since MySQL not support "IF NOT EXIST" in creating an index,
                    // do special treatment for MySQL.
                    return;
                }
                throw new LightIndexCreateException(e.getCause());
            }
        }
    }

    public void executeRawSqlWithNoReturn(String sql) {
        if (sql == null) {
            return;
        }
        Connection conn = requireConnection();
        executeRaw(conn, sql);
    }

    private void executeRaw(Connection conn, String... sqls) {
        if (sqls == null || sqls.length == 0) {
            releaseConnection(conn);
            return;
        }
        try {
            for (String sql : sqls) {
                if (sql == null || sql.isEmpty()) {
                    continue;
                }
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.execute();
                stmt.close();
            }
        } catch (SQLException e) {
            throw new LightRuntimeException(e);
        } finally {
            releaseConnection(conn);
        }
    }

    private Connection rawConnection() {
        checkConnectionPool();
        return connectionPool.requireConnection();
    }

    public Connection requireConnection() throws LightRuntimeException {
        Connection rawConnection = rawConnection();
        String initEnvConn = getDialectProvider().initConnectionEnvironment(mDatabaseInfo);
        if (initEnvConn == null) {
            return rawConnection;
        }
        try {
            PreparedStatement initStmt = rawConnection.prepareStatement(initEnvConn);
            initStmt.execute();
            initStmt.close();
        } catch (SQLException e) {
            throw new LightRuntimeException(e);
        }
        return rawConnection;
    }

    public ManagedConnection requireManagedConnection() throws LightRuntimeException {
        return new ManagedConnection(this);
    }

    public void releaseConnection(Connection connection) throws LightRuntimeException, NullPointerException {
        checkConnectionPool();
        connectionPool.release(connection);
    }

    private void checkConnectionPool() {
        if (connectionPool == null) {
            throw new NullPointerException("ConnectionPool cannot be null.");
        }
    }

    public DialectProvider getDialectProvider() {
        return dialectProvider;
    }

    @Deprecated
    public PreparedStatement resolveStatement(String sql, Connection connection, boolean returnsGeneratedKey) {
        PreparedStatement stmt;
        if (connection == null) {
            throw new IllegalStateException("Connection is null.");
        }
        try {
            if (returnsGeneratedKey) {
                stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            } else {
                stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            }
        } catch (SQLException e) {
            logger.error("An error occurred while require a PreparedStatement.", e);
            throw new LightRuntimeException(e);
        }

        return stmt;
    }

    protected void clearAllTables() {
        mTableStructCache.values().forEach(table ->
                destroyTable(table.getName()));
    }

    protected void destroyTable(String tableName) {
        Table table = findTable(tableName);
        if (table == null) {
            return;
        }
        executeRawSqlWithNoReturn(dialectProvider.drop(table));
    }

    private final Map<String, Table> mTableStructCache =
            new ConcurrentHashMap<>();

    protected void registerTable(Table table) {
        if (table == null) {
            return;
        }
        mTableStructCache.put(table.getName(), table);
    }

    public Table findTable(String tableName) {
        if (!mTableStructCache.containsKey(tableName)) {
            return null;
        }
        return mTableStructCache.get(tableName);
    }

    private volatile Metadata metadata;
    private final byte[] metadataLock = new byte[0];

    public Metadata getMetadata() {
        if (metadata == null) {
            synchronized (metadataLock) {
                if (metadata == null) {
                    boolean supportsBatch;
                    boolean supportsTransaction;
                    try {
                        Connection connection =
                                getConnectionPool().requireConnection();
                        DatabaseMetaData databaseMetaData =
                                connection.getMetaData();
                        supportsBatch =
                                databaseMetaData.supportsBatchUpdates();
                        supportsTransaction =
                                databaseMetaData.supportsTransactions();
                        getConnectionPool().release(connection);
                    } catch (SQLException e) {
                        throw new LightRuntimeException(e);
                    }

                    metadata = new Metadata(supportsBatch, supportsTransaction);
                }
            }
        }

        return metadata;
    }

    public static class Metadata {
        public final boolean supportsBatch;
        public final boolean supportsTransaction;

        Metadata(boolean supportsBatch, boolean supportsTransaction) {
            this.supportsBatch = supportsBatch;
            this.supportsTransaction = supportsTransaction;
        }
    }

    public static class MigrationContainer {
        private final HashMap<Integer, TreeMap<Integer, Migration>> mMigrations = new HashMap<>();

        public void addMigrations(Migration... migrations) {
            for (Migration migration : migrations) {
                addMigration(migration);
            }
        }

        public void addMigrations(List<Migration> migrations) {
            for (Migration migration : migrations) {
                addMigration(migration);
            }
        }

        private void addMigration(Migration migration) {
            final int start = migration.startVersion;
            final int end = migration.endVersion;
            TreeMap<Integer, Migration> targetMap =
                    mMigrations.computeIfAbsent(start, k -> new TreeMap<>());
            targetMap.put(end, migration);
        }

        public Map<Integer, Map<Integer, Migration>> getMigrations() {
            return Collections.unmodifiableMap(mMigrations);
        }

        public List<Migration> findMigrationPath(int start, int end) {
            if (start == end) {
                return Collections.emptyList();
            }
            boolean migrateUp = end > start;
            List<Migration> result = new ArrayList<>();
            return findUpMigrationPath(result, migrateUp, start, end);
        }

        private List<Migration> findUpMigrationPath(List<Migration> result,
                                                    boolean upgrade,
                                                    int start, int end) {
            while (upgrade ? start < end : start > end) {
                TreeMap<Integer, Migration> targetNodes = mMigrations.get(start);
                if (targetNodes == null) {
                    return null;
                }
                Set<Integer> keySet;
                if (upgrade) {
                    keySet = targetNodes.descendingKeySet();
                } else {
                    keySet = targetNodes.keySet();
                }
                boolean found = false;
                for (int targetVersion : keySet) {
                    final boolean shouldAddToPath;
                    if (upgrade) {
                        shouldAddToPath = targetVersion <= end && targetVersion > start;
                    } else {
                        shouldAddToPath = targetVersion >= end && targetVersion < start;
                    }
                    if (shouldAddToPath) {
                        result.add(targetNodes.get(targetVersion));
                        start = targetVersion;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return null;
                }
            }
            return result;
        }

    }

    public static class Builder<T extends LightDatabase> {
        private final Class<T> databaseClass;
        private final String name;
        private final Database database;
        private final DialectProvider dialectProvider;
        private final MigrationContainer migrationContainer;

        private DatasourceConfig datasourceConfig;
        private ConnectionPool connectionPool;
        private LightLogger logger;

        Builder(Class<T> clazz, DialectProvider provider) {
            if (clazz == null || provider == null) {
                throw new IllegalArgumentException("Cannot be null!");
            }
            databaseClass = clazz;
            dialectProvider = provider;
            database = clazz.getAnnotation(Database.class);
            if (database == null) {
                throw new IllegalStateException("Must be annotated with '@Database'!");
            }
            name = database.name();
            migrationContainer = new MigrationContainer();
        }

        Builder(Class<T> clazz, Class<? extends DialectProvider> providerClass) {
            this(clazz, Light.createDialectProviderInstance(providerClass));
        }

        public Builder<T> setLogger(LightLogger logger) {
            this.logger = logger;
            return this;
        }

        @LightExperimentalApi
        public Builder<T> addMigrations(Migration... migrations) {
            migrationContainer.addMigrations(migrations);
            return this;
        }


        @LightExperimentalApi
        public Builder<T> addMigration(Migration migration) {
            migrationContainer.addMigration(migration);
            return this;
        }

        /**
         * @return this
         */
        @LightExperimentalApi
        public Builder<T> deleteOnConflict() {
            return deleteOnConflict(true);
        }

        /**
         * @param enable enable
         * @return this
         */
        @LightExperimentalApi
        public Builder<T> deleteOnConflict(boolean enable) {
            return this;
        }

        public Builder<T> datasource(DatasourceConfig datasourceConfig) {
            this.datasourceConfig = datasourceConfig;
            return this;
        }

        public Builder<T> setConnectionPool(Class<? extends ConnectionPool> poolClass) {
            this.connectionPool = Light.createConnectionPoolInstance(poolClass);
            return this;
        }

        public Builder<T> setConnectionPool(ConnectionPool connectionPool) {
            this.connectionPool = connectionPool;
            return this;
        }

        private DatasourceConfig generateConfig() {
            if (datasourceConfig != null) {
                return datasourceConfig;
            }

            if (database.datasourceConfig().isEmpty()) {
                return new DatasourceLoader().load();
            }
            return new DatasourceLoader(
                    database.datasourceConfig(),
                    database.name()).load();
        }

        private DatabaseConfiguration createConf() {
            if (dialectProvider == null) {
                throw new IllegalStateException("DialectProvider cannot be null!");
            }
            DatasourceConfig config = generateConfig();
            Configurations configurations = getConfigurations();
            return new DatabaseConfiguration(
                    name,
                    config,
                    connectionPool,
                    dialectProvider,
                    logger,
                    migrationContainer,
                    configurations
            );
        }

        private Configurations getConfigurations() {
            Database database = databaseClass.getAnnotation(Database.class);
            LightConfiguration[] databaseConfigurations = database.configuration();
            List<LightConfiguration> lightConfigurations = new ArrayList<>(
                    Arrays.asList(databaseConfigurations)
            );
            LightConfiguration configuration = databaseClass.getAnnotation(LightConfiguration.class);
            if (configuration != null) {
                lightConfigurations.add(configuration);
            }
            LightConfigurations annotation = databaseClass.getAnnotation(LightConfigurations.class);
            if (annotation != null) {
                lightConfigurations.addAll(Arrays.asList(annotation.value()));
            }
            List<Configurations.Configuration> configurations = lightConfigurations
                    .stream()
                    .map(Configurations.Configuration::create)
                    .collect(Collectors.toList());
            return Configurations.createFrom(configurations);
        }

        public T build() {
            T database = Light.getGeneratedImplInstance(databaseClass,
                    IMPL_SUFFIX);
            database.init(createConf());
            return database;
        }
    }

}
