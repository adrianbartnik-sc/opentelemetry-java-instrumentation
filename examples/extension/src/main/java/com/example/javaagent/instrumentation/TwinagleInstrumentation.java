/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent.instrumentation;


import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

import com.soundcloud.jvmkit.module.http.server.HandlerRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class TwinagleInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return AgentElementMatchers.hasSuperType(
        namedOneOf("com.soundcloud.jvmkit.module.twirp.TwirpHandler"));
  }

  @Override
  public void transform(TypeTransformer typeTransformer) {
    typeTransformer.applyAdviceToMethod(
        namedOneOf("apply"), this.getClass().getName() + "$FortuneAdvice");
  }

  @SuppressWarnings("unused")
  public static class FortuneAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.Argument(value = 0) HandlerRequest request) {
      Span serverSpan = LocalRootSpan.fromContextOrNull(currentContext());

      if (request == null) {
        throw new RuntimeException("No request");
      } else {
        if (serverSpan == null) {
          throw new RuntimeException("No root span");
        } else {
          serverSpan.updateName(request.method().name().toUpperCase() + " " + request.path());
        }
      }
    }
  }
}
