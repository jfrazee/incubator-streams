/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.streams.example.test;

import org.apache.streams.config.ComponentConfigurator;
import org.apache.streams.config.StreamsConfigurator;
import org.apache.streams.elasticsearch.ElasticsearchClientManager;
import org.apache.streams.example.ElasticsearchHdfs;
import org.apache.streams.example.ElasticsearchHdfsConfiguration;
import org.apache.streams.jackson.StreamsJacksonMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.testng.Assert.assertNotEquals;

/**
 * ElasticsearchHdfsIT is an integration test for ElasticsearchHdfs.
 */
public class ElasticsearchHdfsIT {

  private final static Logger LOGGER = LoggerFactory.getLogger(ElasticsearchHdfsIT.class);

  ObjectMapper MAPPER = StreamsJacksonMapper.getInstance();

  protected ElasticsearchHdfsConfiguration testConfiguration;
  protected Client testClient;

  private int count = 0;

  @BeforeClass
  public void prepareTest() throws Exception {

    testConfiguration = new StreamsConfigurator<>(ElasticsearchHdfsConfiguration.class).detectCustomConfiguration("ElasticsearchHdfsIT");
    testClient = ElasticsearchClientManager.getInstance(testConfiguration.getSource()).client();

    ClusterHealthRequest clusterHealthRequest = Requests.clusterHealthRequest();
    ClusterHealthResponse clusterHealthResponse = testClient.admin().cluster().health(clusterHealthRequest).actionGet();
    assertNotEquals(clusterHealthResponse.getStatus(), ClusterHealthStatus.RED);

    IndicesExistsRequest indicesExistsRequest = Requests.indicesExistsRequest(testConfiguration.getSource().getIndexes().get(0));
    IndicesExistsResponse indicesExistsResponse = testClient.admin().indices().exists(indicesExistsRequest).actionGet();
    assertThat(indicesExistsResponse.isExists(), is(true));

    SearchRequestBuilder countRequest = testClient
        .prepareSearch(testConfiguration.getSource().getIndexes().get(0))
        .setTypes(testConfiguration.getSource().getTypes().get(0));
    SearchResponse countResponse = countRequest.execute().actionGet();

    count = (int)countResponse.getHits().getTotalHits();

    assertNotEquals(count, 0);
  }

  @Test
  public void ElasticsearchHdfsIT() throws Exception {

    ElasticsearchHdfs backup = new ElasticsearchHdfs(testConfiguration);
    backup.run();

  }

}
