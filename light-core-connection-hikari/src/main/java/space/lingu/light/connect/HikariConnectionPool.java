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

package space.lingu.light.connect;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import space.lingu.light.DatasourceConfig;
import space.lingu.light.LightRuntimeException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Hikari Connection Pool implementation of {@link ConnectionPool}.
 * <p>
 * If you are using a different version of {@code HikariCP} or
 * just don't want to import any more dependencies,
 * you can copy this file to your project.
 * <p>
 * In most cases, it works fine.
 *
 * @author RollW
 */
public class HikariConnectionPool extends BaseConnectionPool {
    private final BiConsumer<HikariConfig, DatasourceConfig> configurable;

    private HikariDataSource source;
    private DatasourceConfig datasourceConfig;

    public HikariConnectionPool() {
        this((Consumer<HikariConfig>) null);
    }

    public HikariConnectionPool(Consumer<HikariConfig> configurable) {
        this((config, datasourceConfig) -> {
            if (configurable != null) {
                configurable.accept(config);
            }
        });
    }

    public HikariConnectionPool(
            BiConsumer<HikariConfig, DatasourceConfig> configurable) {
        this.configurable = configurable;
    }

    @Override
    public final void setDatasourceConfig(DatasourceConfig config) {
        if (this.datasourceConfig != null &&
                this.datasourceConfig.equals(config)) {
            logger.debug("Datasource config not changed, ignore.");
            return;
        }
        if (source != null) {
            source.close();
        }
        if (logger != null) {
            logger.debug("Set up HikariCP connection pool: " + config);
        }
        this.datasourceConfig = config;
        HikariConfig hikariConfig = new HikariConfig();
        preSetupHikariConfig(hikariConfig, config);
        source = new HikariDataSource(hikariConfig);
    }

    private void preSetupHikariConfig(HikariConfig hikariConfig,
                                      DatasourceConfig config) {
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        if (configurable != null) {
            configurable.accept(hikariConfig, config);
        }
        setupHikariConfig(hikariConfig);

        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getJdbcName());
    }

    @Override
    public Connection requireConnection() {
        checkPool();
        try {
            return source.getConnection();
        } catch (SQLException e) {
            throw new LightRuntimeException(e);
        }
    }

    @Override
    public void release(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new LightRuntimeException(e);
        }
    }

    @Override
    public void close() {
        checkPool();
        if (source != null) {
            source.close();
        }
    }

    private void checkPool() {
        if (source == null) {
            throw new LightRuntimeException("Not initialize Hikari datasource.");
        }
    }

    /**
     * Override this method to set up your personal configuration.
     * As an alternative, you can also use the constructor
     * ({@link #HikariConnectionPool(Consumer)})
     * to configure the {@link HikariConfig}.
     * <p>
     * The {@link Consumer#accept(Object)} method
     * will be called before this method.
     *
     * @param config HikariConfig
     */
    protected void setupHikariConfig(HikariConfig config) {
    }
}
