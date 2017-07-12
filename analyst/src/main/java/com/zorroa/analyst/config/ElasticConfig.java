package com.zorroa.analyst.config;

import com.zorroa.analyst.Application;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.elastic.ElasticClientUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

/**
 * Created by chambers on 2/12/16.
 */
@Configuration
public class ElasticConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticConfig.class);

    @Autowired
    ApplicationProperties properties;

    @Bean
    public Client elastic() throws IOException {
        /**
         * If we're not master or data, the no need to even start elastic.
         */
        if (!properties.getBoolean("analyst.index.data") &&
                !properties.getBoolean("analyst.index.master")) {
            return null;
        }

        Random rand = new Random(System.nanoTime());
        int number = rand.nextInt(99999);

        String hostName = InetAddress.getLocalHost().getHostName();
        String nodeName = String.format("%s_%05d", hostName, number);
        String elasticMaster = properties.getString("analyst.index.host");

        Settings.Builder builder =
                Settings.settingsBuilder()
                        .put("cluster.name", "zorroa")
                        .put("path.data", properties.getString("analyst.path.index"))
                        .put("path.home", properties.getString("analyst.path.home"))
                        .put("node.name", nodeName)
                        .put("node.master", properties.getBoolean("analyst.index.master"))
                        .put("node.data", properties.getBoolean("analyst.index.data"))
                        .put("cluster.routing.allocation.disk.threshold_enabled", false)
                        .put("http.enabled", "false")
                        .put("network.host", "0.0.0.0")
                        .put("discovery.zen.no_master_block", "write")
                        .put("discovery.zen.fd.ping_timeout", "3s")
                        .put("discovery.zen.fd.ping_retries", 10)
                        .put("discovery.zen.ping.multicast.enabled", false)
                        .putArray("discovery.zen.ping.unicast.hosts", elasticMaster)
                        .put("action.auto_create_index", "-archivist*");

        if (Application.isUnitTest()) {
            logger.info("Elastic in unit test mode");
            builder.put("node.master", true);
            builder.put("index.refresh_interval", "1s");
            builder.put("index.translog.disable_flush", false);
            builder.put("node.local", true);
        }
        else {
            logger.info("Connecting to elastic master: {}", elasticMaster);
        }
        return ElasticClientUtils.initializeClient(builder);
    }
}
