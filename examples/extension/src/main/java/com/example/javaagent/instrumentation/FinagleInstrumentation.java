/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent.instrumentation;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

import com.soundcloud.jvmkit.module.http.server.HandlerRequest;
import com.soundcloud.jvmkit.module.http.server.PathPattern;
import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Response;
import com.twitter.util.Future;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import scala.Tuple3;
import scala.runtime.AbstractFunction1;

public class FinagleInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return AgentElementMatchers.hasSuperType(
        namedOneOf("com.soundcloud.jvmkit.module.http.server.HandlerRouter"));
  }

  @Override
  public void transform(TypeTransformer typeTransformer) {
    typeTransformer.applyAdviceToMethod(
        namedOneOf("findMatchFor"), this.getClass().getName() + "$FortuneAdvice");
  }

  @SuppressWarnings("unused")
  public static class FortuneAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.Return(readOnly = true)
            Tuple3<Method, PathPattern, AbstractFunction1<HandlerRequest, Future<Response>>>
                returnValue) {
      Span serverSpan = LocalRootSpan.fromContextOrNull(currentContext());

      if (returnValue == null) {
        throw new RuntimeException("Couldn't find request");
      } else {
        if (serverSpan == null) {
          throw new RuntimeException("Couldn't find root span");
        } else {
          serverSpan.updateName(
              returnValue._1().name().toUpperCase() + " " + returnValue._2().rawPattern());
        }
      }
    }
  }
}
