package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.testutil.Assertions.*;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;


@RunWith(DataProviderRunner.class)
public class ClassFileImporterAnnotationsTest {

    @Test
    public void meta_annotation_types_are_transitively_imported() {
        JavaClass javaClass = new ClassFileImporter().importClass(MetaAnnotatedClass.class);
        JavaAnnotation<JavaClass> someAnnotation = javaClass.getAnnotationOfType(SomeAnnotation.class.getName());
        JavaAnnotation<JavaClass> someMetaAnnotation = someAnnotation.getRawType()
                .getAnnotationOfType(SomeMetaAnnotation.class.getName());
        JavaAnnotation<JavaClass> someMetaMetaAnnotation = someMetaAnnotation.getRawType()
                .getAnnotationOfType(SomeMetaMetaAnnotation.class.getName());
        JavaAnnotation<JavaClass> someMetaMetaMetaAnnotation = someMetaMetaAnnotation.getRawType()
                .getAnnotationOfType(SomeMetaMetaMetaAnnotationWithParameters.class.getName());

        assertThatType(someMetaMetaMetaAnnotation.getType()).matches(SomeMetaMetaMetaAnnotationWithParameters.class);
    }

    @DataProvider
    public static Object[][] elementsAnnotatedWithSomeAnnotation() {
        return testForEach(
                new ClassFileImporter().importClass(MetaAnnotatedClass.class)
        );
    }

    @Test
    @UseDataProvider("elementsAnnotatedWithSomeAnnotation")
    public void parameters_of_meta_annotations_are_transitively_imported(HasAnnotations<?> annotatedWithSomeAnnotation) {
        JavaAnnotation<?> someAnnotation = annotatedWithSomeAnnotation
                .getAnnotationOfType(SomeAnnotation.class.getName());
        JavaAnnotation<?> metaAnnotationWithParameters = someAnnotation.getRawType()
                .getAnnotationOfType(MetaAnnotationWithParameters.class.getName());

        assertThatAnnotation(metaAnnotationWithParameters).hasEnumProperty("someEnum", SomeEnum.CONSTANT);
        assertThatAnnotation(metaAnnotationWithParameters).hasAnnotationProperty("parameterAnnotation", ParameterAnnotation.class)
                .withClassProperty("value", SomeAnnotationParameterType.class);

        JavaAnnotation<JavaClass> metaMetaMetaAnnotation = someAnnotation
                .getRawType().getAnnotationOfType(SomeMetaAnnotation.class.getName())
                .getRawType().getAnnotationOfType(SomeMetaMetaAnnotation.class.getName())
                .getRawType().getAnnotationOfType(SomeMetaMetaMetaAnnotationWithParameters.class.getName());

        assertThatAnnotation(metaMetaMetaAnnotation)
                .hasClassProperty("classParam", SomeMetaMetaMetaAnnotationClassParameter.class)
                .hasEnumProperty("enumParam", SomeMetaMetaMetaAnnotationEnumParameter.VALUE)
                .hasAnnotationProperty("annotationParam", SomeMetaMetaMetaParameterAnnotation.class)
                .withClassProperty("value", SomeMetaMetaMetaParameterAnnotationClassParameter.class);
    }

    private @interface MetaAnnotationWithParameters {
        SomeEnum someEnum();

        ParameterAnnotation parameterAnnotation();
    }

    private @interface SomeMetaMetaMetaAnnotationWithParameters {
        Class<?> classParam();

        SomeMetaMetaMetaAnnotationEnumParameter enumParam();

        SomeMetaMetaMetaParameterAnnotation annotationParam();
    }

    @SomeMetaMetaMetaAnnotationWithParameters(
            classParam = SomeMetaMetaMetaAnnotationClassParameter.class,
            enumParam = SomeMetaMetaMetaAnnotationEnumParameter.VALUE,
            annotationParam = @SomeMetaMetaMetaParameterAnnotation(SomeMetaMetaMetaParameterAnnotationClassParameter.class)
    )
    private @interface SomeMetaMetaAnnotation {
    }

    @SomeMetaMetaAnnotation
    private @interface SomeMetaAnnotation {
    }

    @MetaAnnotationWithParameters(
            someEnum = SomeEnum.CONSTANT,
            parameterAnnotation = @ParameterAnnotation(SomeAnnotationParameterType.class)
    )
    @SomeMetaAnnotation
    private @interface SomeAnnotation {
    }

    private enum SomeEnum {
        CONSTANT,
        VARIABLE
    }

    private @interface ParameterAnnotation {
        Class<?> value();
    }

    private static class SomeAnnotationParameterType {
    }

    @SomeAnnotation
    private static class MetaAnnotatedClass {
    }

    private static class SomeMetaMetaMetaAnnotationClassParameter {
    }

    private enum SomeMetaMetaMetaAnnotationEnumParameter {
        VALUE
    }

    private @interface SomeMetaMetaMetaParameterAnnotation {
        Class<?> value();
    }

    private static class SomeMetaMetaMetaParameterAnnotationClassParameter {
    }
}
