package com.robinloom.classcraft.processor;

import com.robinloom.classcraft.annotations.GenerateMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@javax.annotation.processing.SupportedAnnotationTypes("com.robinloom.classcraft.annotations.GenerateMapper")
@javax.annotation.processing.SupportedSourceVersion(SourceVersion.RELEASE_25)
public class MapperProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(GenerateMapper.class);

        for (Element element : elements) {
            if (!(element instanceof TypeElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateMapper can only be applied to classes");
                continue;
            }

            TypeElement sourceClass = (TypeElement) element;

            if (sourceClass.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateMapper can only be applied to classes");
                continue;
            }

            try {
                generateMapper(sourceClass);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate mapper: " + e.getMessage());
            }
        }

        return true;
    }

    private void generateMapper(TypeElement sourceClass) throws IOException {
        GenerateMapper annotation = sourceClass.getAnnotation(GenerateMapper.class);

        ClassName sourceClassName = ClassName.get(sourceClass);
        String targetFqn = annotation.to();
        String[] parts = targetFqn.split("\\.");
        String targetSimpleName = parts[parts.length - 1];
        String targetPackage = targetFqn.substring(0, targetFqn.lastIndexOf('.'));
        ClassName targetClass = ClassName.get(targetPackage, targetSimpleName);

        PackageElement pkg = elements.getPackageOf(sourceClass);
        String packageName = pkg.getQualifiedName().toString();

        String mapperClassName = sourceClassName.simpleName() + "Mapper";

        // Get fields from source class
        List<VariableElement> sourceFields = sourceClass.getEnclosedElements().stream()
            .filter(e -> e.getKind() == javax.lang.model.element.ElementKind.FIELD)
            .map(e -> (VariableElement) e)
            .collect(Collectors.toList());

        // Try to detect if target class is mutable (has setters)
        boolean targetIsMutable = isMutableClass(targetFqn, sourceFields);

        // Generate Mapper class with static methods
        TypeSpec.Builder mapperBuilder = TypeSpec.classBuilder(mapperClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addJavadoc("Auto-generated mapper between $L and $L.\n\n" +
                    "Assumes both classes have identical fields in the same order.\n" +
                    (targetIsMutable ? "Uses no-arg constructor + setters for mutable target.\n" :
                     "Uses all-args constructor for immutable target.\n"),
                sourceClassName.simpleName(), targetSimpleName);

        // Private constructor to prevent instantiation
        mapperBuilder.addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build());

        String sourceVarName = sourceClassName.simpleName().toLowerCase();
        String targetVarName = targetSimpleName.toLowerCase();

        // toTarget static method
        MethodSpec.Builder toTargetBuilder = MethodSpec.methodBuilder("to" + targetSimpleName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(sourceClassName, sourceVarName)
            .returns(targetClass)
            .addStatement("if ($L == null) return null", sourceVarName);

        if (targetIsMutable) {
            // Use no-arg constructor + setters
            toTargetBuilder.addStatement("$T $L = new $T()", targetClass, targetVarName, targetClass);
            for (VariableElement field : sourceFields) {
                String fieldName = field.getSimpleName().toString();
                toTargetBuilder.addStatement("$L.set$L($L.get$L())",
                    targetVarName, capitalize(fieldName), sourceVarName, capitalize(fieldName));
            }
            toTargetBuilder.addStatement("return $L", targetVarName);
        } else {
            // Use all-args constructor
            StringBuilder targetArgs = new StringBuilder();
            for (int i = 0; i < sourceFields.size(); i++) {
                if (i > 0) targetArgs.append(", ");
                targetArgs.append(sourceVarName).append(".get").append(capitalize(sourceFields.get(i).getSimpleName().toString())).append("()");
            }
            toTargetBuilder.addStatement("return new $T($L)", targetClass, targetArgs.toString());
        }

        mapperBuilder.addMethod(toTargetBuilder.build());

        // toSource static method
        MethodSpec.Builder toSourceBuilder = MethodSpec.methodBuilder("to" + sourceClassName.simpleName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(targetClass, targetVarName)
            .returns(sourceClassName)
            .addStatement("if ($L == null) return null", targetVarName);

        boolean sourceIsMutable = isMutableClass(sourceClass.getQualifiedName().toString(), sourceFields);

        if (sourceIsMutable) {
            // Use no-arg constructor + setters
            toSourceBuilder.addStatement("$T $L = new $T()", sourceClassName, sourceVarName, sourceClassName);
            for (VariableElement field : sourceFields) {
                String fieldName = field.getSimpleName().toString();
                toSourceBuilder.addStatement("$L.set$L($L.get$L())",
                    sourceVarName, capitalize(fieldName), targetVarName, capitalize(fieldName));
            }
            toSourceBuilder.addStatement("return $L", sourceVarName);
        } else {
            // Use all-args constructor
            StringBuilder sourceArgs = new StringBuilder();
            for (int i = 0; i < sourceFields.size(); i++) {
                if (i > 0) sourceArgs.append(", ");
                sourceArgs.append(targetVarName).append(".get").append(capitalize(sourceFields.get(i).getSimpleName().toString())).append("()");
            }
            toSourceBuilder.addStatement("return new $T($L)", sourceClassName, sourceArgs.toString());
        }

        mapperBuilder.addMethod(toSourceBuilder.build());

        // Write mapper class
        JavaFile mapperFile = JavaFile.builder(packageName, mapperBuilder.build()).build();
        mapperFile.writeTo(filer);
    }

    /**
     * Detect if a class is mutable by checking for:
     * 1. No-arg constructor exists
     * 2. Setter methods for all fields exist
     */
    private boolean isMutableClass(String classFqn, List<VariableElement> expectedFields) {
        try {
            // Try to get TypeElement for the target class
            TypeElement targetElement = elements.getTypeElement(classFqn);
            if (targetElement == null) {
                // Target class not yet compiled, assume immutable (safer)
                return false;
            }

            // Check for no-arg constructor
            boolean hasNoArgConstructor = targetElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR)
                .map(e -> (javax.lang.model.element.ExecutableElement) e)
                .anyMatch(e -> e.getParameters().isEmpty());

            if (!hasNoArgConstructor) return false;

            // Check for setters for all fields
            for (VariableElement field : expectedFields) {
                String fieldName = field.getSimpleName().toString();
                String setterName = "set" + capitalize(fieldName);

                boolean hasSetterMethod = targetElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == javax.lang.model.element.ElementKind.METHOD)
                    .map(e -> (javax.lang.model.element.ExecutableElement) e)
                    .anyMatch(e -> e.getSimpleName().toString().equals(setterName) &&
                                   e.getParameters().size() == 1);

                if (!hasSetterMethod) return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
