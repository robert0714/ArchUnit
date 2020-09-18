package com.tngtech.archunit.testutil.assertion;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.TestUtils.invoke;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class JavaAnnotationAssertion extends AbstractObjectAssert<JavaAnnotationAssertion, JavaAnnotation<?>> {
    public JavaAnnotationAssertion(JavaAnnotation<?> actual) {
        super(actual, JavaAnnotationAssertion.class);
    }

    public JavaAnnotationAssertion hasClassProperty(String propertyName, Class<?> expectedClass) {
        JavaClass actualClassValue = getPropertyOfType(propertyName, JavaClass.class);
        assertThatType(actualClassValue).as("Class<?> @%s.%s()", actual.getRawType().getSimpleName(), propertyName).matches(expectedClass);
        return this;
    }

    public JavaAnnotationAssertion withClassProperty(String propertyName, Class<?> expectedClass) {
        return hasClassProperty(propertyName, expectedClass);
    }

    public JavaAnnotationAssertion hasEnumProperty(String propertyName, Enum<?> expectedEnumConstant) {
        JavaEnumConstant actualEnumConstant = getPropertyOfType(propertyName, JavaEnumConstant.class);
        assertThat(actualEnumConstant)
                .as("%s @%s.%s()", actualEnumConstant.getDeclaringClass().getSimpleName(), actual.getRawType().getSimpleName(), propertyName)
                .isEquivalentTo(expectedEnumConstant);
        return this;
    }

    public JavaAnnotationAssertion hasAnnotationProperty(String propertyName, Class<? extends Annotation> expectedAnnotationType) {
        JavaAnnotation<?> actualAnnotationProperty = getPropertyOfType(propertyName, JavaAnnotation.class);
        assertThatType(actualAnnotationProperty.getType())
                .as("%s @%s.%s()", actualAnnotationProperty.getRawType().getSimpleName(), actual.getRawType().getSimpleName(), propertyName)
                .matches(expectedAnnotationType);
        return new JavaAnnotationAssertion(actualAnnotationProperty);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPropertyOfType(String propertyName, Class<T> propertyType) {
        Optional<?> property = actual.get(propertyName);
        assertThat(property).as("property '%s'", property).isPresent();
        assertThat(property.get()).as("property '%s'", property).isInstanceOf(propertyType);
        return (T) property.get();
    }

    @SuppressWarnings("rawtypes")
    public static Set<Map<String, Object>> runtimePropertiesOf(Set<? extends JavaAnnotation<?>> annotations) {
        List<Annotation> converted = new ArrayList<>();
        for (JavaAnnotation<?> annotation : annotations) {
            Annotation reflectionAnnotation = annotation.as((Class) annotation.getRawType().reflect());
            if (isRetentionRuntime(reflectionAnnotation)) {
                converted.add(reflectionAnnotation);
            }
        }
        return propertiesOf(converted.toArray(new Annotation[0]));
    }

    private static boolean isRetentionRuntime(Annotation annotation) {
        return annotation.annotationType().isAnnotationPresent(Retention.class)
                && annotation.annotationType().getAnnotation(Retention.class).value() == RUNTIME;
    }

    public static Set<Map<String, Object>> propertiesOf(Annotation[] annotations) {
        Set<Map<String, Object>> result = new HashSet<>();
        for (Annotation annotation : annotations) {
            result.add(propertiesOf(annotation));
        }
        return result;
    }

    private static Map<String, Object> propertiesOf(Annotation annotation) {
        Map<String, Object> props = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            Object returnValue = invoke(method, annotation);
            props.put(method.getName(), valueOf(returnValue));
        }
        return props;
    }

    private static Object valueOf(Object value) {
        if (value.getClass().isArray() && value.getClass().getComponentType().isPrimitive()) {
            return listFrom(value);
        }
        if (value instanceof String[]) {
            return ImmutableList.copyOf((String[]) value);
        }
        if (value instanceof Class) {
            return new SimpleTypeReference(((Class<?>) value).getName());
        }
        if (value instanceof Class[]) {
            return SimpleTypeReference.allOf((Class<?>[]) value);
        }
        if (value instanceof Enum) {
            return new SimpleEnumConstantReference((Enum<?>) value);
        }
        if (value instanceof Enum[]) {
            return SimpleEnumConstantReference.allOf((Enum<?>[]) value);
        }
        if (value instanceof Annotation) {
            return propertiesOf((Annotation) value);
        }
        if (value instanceof Annotation[]) {
            return propertiesOf((Annotation[]) value);
        }
        return value;
    }

    private static List<?> listFrom(Object primitiveArray) {
        checkArgument(primitiveArray.getClass().getComponentType().equals(int.class), "Only supports int[] at the moment, please extend");
        ImmutableList.Builder<Integer> result = ImmutableList.builder();
        for (int anInt : (int[]) primitiveArray) {
            result.add(anInt);
        }
        return result.build();
    }

    private static class SimpleTypeReference {
        private final String typeName;

        private SimpleTypeReference(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final SimpleTypeReference other = (SimpleTypeReference) obj;
            return Objects.equals(this.typeName, other.typeName);
        }

        @Override
        public String toString() {
            return typeName;
        }

        static List<SimpleTypeReference> allOf(Class<?>[] value) {
            ImmutableList.Builder<SimpleTypeReference> result = ImmutableList.builder();
            for (Class<?> c : value) {
                result.add(new SimpleTypeReference(c.getName()));
            }
            return result.build();
        }
    }

    private static class SimpleEnumConstantReference {
        private final SimpleTypeReference type;
        private final String name;

        SimpleEnumConstantReference(Enum<?> value) {
            this.type = new SimpleTypeReference(value.getDeclaringClass().getName());
            this.name = value.name();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final SimpleEnumConstantReference other = (SimpleEnumConstantReference) obj;
            return Objects.equals(this.type, other.type)
                    && Objects.equals(this.name, other.name);
        }

        @Override
        public String toString() {
            return type + "." + name;
        }

        static List<SimpleEnumConstantReference> allOf(Enum<?>[] values) {
            ImmutableList.Builder<SimpleEnumConstantReference> result = ImmutableList.builder();
            for (Enum<?> value : values) {
                result.add(new SimpleEnumConstantReference(value));
            }
            return result.build();
        }
    }
}
