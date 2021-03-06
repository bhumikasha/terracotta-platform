/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.json.Json;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class AttachDetachCommand2x2IT extends DynamicConfigIT {
  private static final String OUTPUT_JSON_FILE = "attach-detach-output.json";

  public AttachDetachCommand2x2IT() {
    super(Duration.ofSeconds(180));
  }

  @Test
  public void test_attach_detach_with_unconfigured_nodes() throws Exception {
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    Cluster cluster = Json.parse(Paths.get("target", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));

    // add a node
    assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    cluster = Json.parse(Paths.get("target", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // add a stripe
    assertThat(configToolInvocation("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    cluster = Json.parse(Paths.get("target", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getNodeAddresses(), hasSize(3));

    // remove the previously added stripe
    assertThat(configToolInvocation("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    cluster = Json.parse(Paths.get("target", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // remove the previously added node
    assertThat(configToolInvocation("detach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    cluster = Json.parse(Paths.get("target", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
  }

  private void downloadToLocal() throws IOException {
    angela.tsa().browse(getNode(1, 1), ".").list().stream()
        .filter(remoteFile -> remoteFile.getName().equals(OUTPUT_JSON_FILE))
        .findAny()
        .get()
        .downloadTo(Paths.get("target").resolve(OUTPUT_JSON_FILE).toFile());
  }
}
