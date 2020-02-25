import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import datadog.trace.agent.test.base.HttpServerTestAdvice;
import datadog.trace.agent.tooling.Instrumenter;
import java.util.Collection;
import java.util.Collections;
import net.bytebuddy.agent.builder.AgentBuilder;

@AutoService(Instrumenter.class)
public class GrizzlyTestInstrumentation implements Instrumenter {

  @Override
  public AgentBuilder instrument(final AgentBuilder agentBuilder) {
    return agentBuilder
        .type(named("org.glassfish.grizzly.http.server.HttpHandlerChain"))
        .transform(
            new AgentBuilder.Transformer.ForAdvice()
                .advice(named("doHandle"), HttpServerTestAdvice.ServerEntryAdvice.class.getName()));
  }

  @Override
  public Collection<String> getLibraryBlacklistedPrefixes() {
    return Collections.emptySet();
  }
}
