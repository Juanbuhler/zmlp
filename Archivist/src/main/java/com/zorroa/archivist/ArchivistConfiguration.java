package com.zorroa.archivist;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.net.InetAddress;

import javax.annotation.PreDestroy;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.ByteStreams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ArchivistConfiguration {

     private static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    @Value("${archivist.index.alias}")
    private String alias;

    @Bean
    public Client elasticSearchClient() throws IOException {

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", "zorroa")
                .put("node.name", InetAddress.getLocalHost().getHostName())
                .put("discovery.zen.ping.multicast.enabled", false)
                .build();

        Node node = nodeBuilder()
                .data(true)
                .settings(settings)
                .node();

        Client client = node.client();
        setupElasticSearchMapping(client);

        return client;
    }

    @PreDestroy
    void shutdown() throws ElasticsearchException, IOException {
        elasticSearchClient().close();
    }

    /**
     * Automatically sets up elastic search if its not setup already.
     *
     * @param client
     * @throws IOException
     */
    private void setupElasticSearchMapping(Client client) throws IOException {

        ClassPathResource resource = new ClassPathResource("elastic-mapping.json");
        byte[] mappingSource = ByteStreams.toByteArray(resource.getInputStream());

        /*
         * Eventually we'll have to keep track of this number somewhere for
         * re-mapping purposes, but for now we're hard coding to 1
         */
        String indexName = String.format("%s_%02d", alias, 1);

        try {
            logger.info("Setting up ElasticSearch index: {} with alias: {}", indexName, alias);

            client.admin()
                .indices()
                .prepareCreate(indexName)
                .setSource(mappingSource)
                .execute()
                .actionGet();

            client.admin()
                .indices()
                .prepareAliases()
                .addAlias(alias, indexName)
                .execute()
                .actionGet();
        }
        catch (IndexAlreadyExistsException e) {
            logger.info("Index: {} with alias: {} alrady exists", indexName, alias);
        }
    }
}
