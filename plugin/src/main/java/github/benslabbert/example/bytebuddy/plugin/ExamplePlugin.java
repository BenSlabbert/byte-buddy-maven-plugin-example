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
    System.err.println("apply");

    MethodList<InDefinedShape> declaredMethods = typeDescription.getDeclaredMethods();
    System.err.println("declaredMethods: " + declaredMethods);

    // add toString
    if (declaredMethods.filter(isToString()).isEmpty()) {
      System.err.println("create toString");
      builder = builder.method(isToString()).intercept(ToStringMethod.prefixedBy("prefix"));
    }

    // create new method
    if (declaredMethods.filter(customMethodElementMatcher()).isEmpty()) {
      System.err.println("add custom method");
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
    System.err.println("close");
  }

  @Override
  public boolean matches(TypeDescription typeDefinitions) {
    System.err.println("typeDefinitions: " + typeDefinitions);
    AnnotationList declaredAnnotations = typeDefinitions.getDeclaredAnnotations();
    System.err.println("declaredAnnotations: " + declaredAnnotations);
    boolean annotationPresent = declaredAnnotations.isAnnotationPresent(ApplyTransformation.class);
    System.err.println("annotationPresent " + annotationPresent);
    return annotationPresent;
  }

  private static class TryFinallyAdvice {

    private TryFinallyAdvice() {}

    @Advice.OnMethodEnter
    private static void onEnter(
        @Advice.AllArguments Object[] args,
        @Advice.Origin("#m") String methodName,
        @Advice.FieldValue(value = "log") Logger log) {
      log.info("Entering method: " + methodName);
      log.info("args: " + Arrays.toString(args));
      PlatformTransactionManager.begin();
    }

    @Advice.OnMethodExit(onThrowable = Exception.class)
    private static void onExit(
        @Advice.Thrown Throwable throwable, @Advice.FieldValue(value = "log") Logger log) {
      log.info("Exiting method...");
      if (throwable != null) {
        log.error("Exception thrown: " + throwable);
        PlatformTransactionManager.rollback();
        return;
      }

      PlatformTransactionManager.commit();
    }
  }
}
