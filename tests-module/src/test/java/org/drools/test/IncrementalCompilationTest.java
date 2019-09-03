package org.drools.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;

public class IncrementalCompilationTest {

    @Test
    public void testIncrementalCompilation()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        final String groupId = "org.kie.test";
        final String artifactId = "container-isolation-kjar1";
        final String version1 = "1.0.0.Final";
        final String version2 = "1.0.3.Final";
        final String sessionName1 = "kjar1.session";
        final String sessionName2 = "kjar103.session";

        final KieServices ks = KieServices.Factory.get();
        KieContainer kieContainer = ks.newKieContainer(ks.newReleaseId(groupId, artifactId, version1));

        assertPerson(kieContainer, sessionName1, "Person from kjar1");

        final Results updateResults = kieContainer.updateToVersion(ks.newReleaseId(groupId, artifactId, version2));
        Assertions.assertThat(updateResults.getMessages(Message.Level.ERROR)).isEmpty();

        assertPerson(kieContainer, sessionName2, "Person from kjar3");
    }

    private void assertPerson(final KieContainer kieContainer, final String sessionName, final String expectedId)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        final KieServices kieServices = KieServices.get();
        final Object person = kieContainer.getClassLoader().loadClass("org.kie.server.testing.Person").newInstance();
        List<Command<?>> commands = new ArrayList<>();
        commands.add(kieServices.getCommands().newInsert(person, "person"));
        commands.add(kieServices.getCommands().newFireAllRules());
        final BatchExecutionCommand batchExecution = kieServices.getCommands().newBatchExecution(commands);
        final StatelessKieSession kieSession = kieContainer.newStatelessKieSession(sessionName);
        final ExecutionResults results = kieSession.execute(batchExecution);
        Assertions.assertThat(results.getValue("person")).isNotNull();
        Assertions.assertThat(results.getValue("person").toString()).contains(expectedId);
    }
}
