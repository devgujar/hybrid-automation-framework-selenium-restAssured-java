package common.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Auto-applies {@link RetryAnalyzer} to every {@code @Test} method without each test
 * declaring it. Register once per suite as a {@code <listener>}.
 * <p><b>Design pattern:</b> IAnnotationTransformer hook (TestNG SPI).</p>
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}

