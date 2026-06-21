package com.whu.shoppingplatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DataSourceRouter extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRouter.class);

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static final String MASTER = "master";
    public static final String SLAVE = "slave";

    public static void setMaster() {
        CONTEXT_HOLDER.set(MASTER);
    }

    public static void setSlave() {
        CONTEXT_HOLDER.set(SLAVE);
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String key = CONTEXT_HOLDER.get();
        if (key == null) {
            key = MASTER;
        }
        log.debug("当前数据源: {}", key);
        return key;
    }
}