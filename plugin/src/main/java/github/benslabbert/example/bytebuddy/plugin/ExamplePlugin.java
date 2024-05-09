/* Licensed under Apache-2.0 2024. */
package github.benslabbert.example.bytebuddy.plugin;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isToString;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import github.benslabbert.example.bytebuddy.annotation.ApplyTransformation;
import github.benslabbert.example.bytebuddy.annotation.PlatformTransactionManager;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.AllArguments;
import net.bytebuddy.asm.Advice.FieldValue;
import net.bytebuddy.asm.Advice.OnMethodEnter;
import net.bytebuddy.asm.Advice.OnMethodExit;
import net.bytebuddy.asm.Advice.Origin;
import net.bytebuddy.asm.Advice.Thrown;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodDescription.InDefinedShape;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.ToStringMethod;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;

public class ExamplePlugin implements Plugin {

  @Override
  public DynamicType.Builder<?> apply(
      DynamicType.Builder<?> builder,
      TypeDescription typeDescription,
      ClassFileLocator classFileLocator) {

    MethodList<InDefinedShape> declaredMethods = typeDescription.getDeclaredMethods();

    // add toString
    if (declaredMethods.filter(isToString()).isEmpty()) {
      builder = builder.method(isToString()).intercept(ToStringMethod.prefixedBy("prefix"));
    }

    // create new method
    if (declaredMethods.filter(customMethodElementMatcher()).isEmpty()) {
      builder =
          builder
              .defineMethod("customMethod", void.class, Modifier.PUBLIC)
              .intercept(StubMethod.INSTANCE);
    }

    if (!declaredMethods.filter(named("existingMethod")).isEmpty()) {
      builder =
          builder.method(named("existingMethod")).intercept(Advice.to(TryFinallyAdvice.class));
    }

    builder =
        builder
            .method(isAnnotatedWith(named(ApplyTransformation.class.getCanonicalName())))
            .intercept(Advice.to(TryFinallyAdvice.class));

    return builder;
  }

  private static <T extends MethodDescription>
      ElementMatcher.Junction<T> customMethodElementMatcher() {
    return named("customMethod")
        .and(takesNoArguments())
        .and(returns(TypeDescription.ForLoadedType.of(Void.class)));
  }

  @Override
  public void close() {
    // nothing open
  }

  @Override
  public boolean matches(TypeDescription typeDefinitions) {
    AnnotationList declaredAnnotations = typeDefinitions.getDeclaredAnnotations();
    return declaredAnnotations.isAnnotationPresent(ApplyTransformation.class);
  }

  private static class TryFinallyAdvice {

    private TryFinallyAdvice() {}

    @OnMethodEnter
    private static void onEnter(
        @AllArguments Object[] args,
        @Origin("#m") String methodName,
        @FieldValue(value = "log") Logger log) {
      log.info("Entering method: {}", methodName);
      log.info("args: {}", args == null ? "null" : Arrays.toString(args));
      PlatformTransactionManager.begin();
    }

    @OnMethodExit(onThrowable = Exception.class)
    private static void onExit(@Thrown Throwable throwable, @FieldValue(value = "log") Logger log) {
      log.info("Exiting method...");
      if (throwable != null) {
        log.error("Exception thrown", throwable);
        PlatformTransactionManager.rollback();
        return;
      }

      PlatformTransactionManager.commit();
    }
  }
}
