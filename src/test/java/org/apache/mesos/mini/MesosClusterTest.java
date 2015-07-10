package org.apache.mesos.mini;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.mesos.mini.docker.DockerUtil;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class MesosClusterTest {

    private static final MesosClusterConfig config = MesosClusterConfig.builder()
            .numberOfSlaves(3)
            .privateRegistryPort(15000) // Currently you have to choose an available port by yourself
            .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]", "ports(*):[9201-9201,9301-9301]", "ports(*):[9202-9202,9302-9302]"})
            .build();

    @ClassRule
    public static MesosCluster cluster = new MesosCluster(config);

    @AfterClass
    public static void callShutdownHook() {
        new DockerUtil(config.dockerClient).stop();
    }

    @Test
    public void mesosClusterCanBeStarted() throws Exception {
        cluster.start();

        JSONObject stateInfo = cluster.getStateInfo();

        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));
    }

    @Test
    public void mesosClusterCanBeStarted2() throws Exception {
        cluster.start();
        JSONObject stateInfo = cluster.getStateInfo();
        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));


        String mesosMasterUrl = cluster.getMesosMasterURL();
        Assert.assertTrue(mesosMasterUrl.contains(":5050"));
    }

    @Test
    public void testPullAndStartContainer() throws UnirestException {
        cluster.start();
        String containerId = cluster.addAndStartContainer("tutum/hello-world");
        String ipAddress = config.dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getIpAddress();
        String url = "http://" + ipAddress + ":80";
        Assert.assertEquals(200, Unirest.get(url).asString().getStatus());
    }
}
