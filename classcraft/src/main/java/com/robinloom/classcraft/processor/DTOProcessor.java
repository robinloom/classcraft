package com.robinloom.classcraft.processor;

import com.robinloom.classcraft.annotations.GenerateDTO;
import com.robinloom.classcraft.annotations.Ignore;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@javax.annotation.processing.SupportedAnnotationTypes("com.robinloom.classcraft.annotations.GenerateDTO")
@javax.annotation.processing.SupportedSourceVersion(SourceVersion.RELEASE_25)
public class DTOProcessor extends AbstractProcessor {

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
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(GenerateDTO.class);

        for (Element element : elements) {
            if (!(element instanceof TypeElement classElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateDTO can only be applied to classes");
                continue;
            }

            // Only process classes, not interfaces/enums
            if (classElement.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateDTO can only be applied to classes, not " + classElement.getKind());
                continue;
            }

            try {
                generateDTO(classElement);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate DTO: " + e.getMessage());
            }
        }

        return true;
    }

    private void generateDTO(TypeElement classElement) throws IOException {
        GenerateDTO annotation = classElement.getAnnotation(GenerateDTO.class);

        if (annotation == null) {
            return;
        }

        List<VariableElement> fields = classElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.FIELD)
            .filter(e -> e.getAnnotation(Ignore.class) == null)
            .map(e -> (VariableElement) e)
            .toList();

        String dtoClassName = classElement.getSimpleName() + annotation.suffix();
        PackageElement pkg = elements.getPackageOf(classElement);
        String packageName = pkg.getQualifiedName().toString();

        // Generate DTO class
        TypeSpec.Builder dtoClassBuilder = TypeSpec.classBuilder(dtoClassName)
            .addModifiers(Modifier.PUBLIC);

        // Add fields (private, final when immutable)
        for (VariableElement field : fields) {
            if (annotation.mutable()) {
                dtoClassBuilder.addField(
                    TypeName.get(field.asType()),
                    field.getSimpleName().toString(),
                    Modifier.PRIVATE);
            } else {
                dtoClassBuilder.addField(
                    TypeName.get(field.asType()),
                    field.getSimpleName().toString(),
                    Modifier.PRIVATE, Modifier.FINAL);
            }
        }

        // Add no-arg constructor if requested (final fields can't be left unassigned)
        if (annotation.generateNoArgConstructor() && annotation.mutable()) {
            dtoClassBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build());
        }

        // Add all-args constructor
        MethodSpec.Builder allArgsConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC);
        for (VariableElement field : fields) {
            allArgsConstructor.addParameter(TypeName.get(field.asType()), field.getSimpleName().toString());
            allArgsConstructor.addStatement("this.$L = $L", field.getSimpleName(), field.getSimpleName());
        }
        dtoClassBuilder.addMethod(allArgsConstructor.build());

        // Add getters
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            dtoClassBuilder.addMethod(MethodSpec.methodBuilder(getterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(field.asType()))
                .addStatement("return this.$L", fieldName)
                .build());
        }

        // Add setters (mutable DTOs only)
        if (annotation.mutable()) {
            for (VariableElement field : fields) {
                String fieldName = field.getSimpleName().toString();
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                dtoClassBuilder.addMethod(MethodSpec.methodBuilder(setterName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.get(field.asType()), fieldName)
                    .addStatement("this.$L = $L", fieldName, fieldName)
                    .build());
            }
        }

        // Add equals() if requested
        if (annotation.generateEqualsHashCode()) {
            MethodSpec.Builder equalsBuilder = MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Object.class, "o")
                .returns(boolean.class)
                .addStatement("if (this == o) return true")
                .addStatement("if (o == null || getClass() != o.getClass()) return false")
                .addStatement("$L that = ($L) o", dtoClassName, dtoClassName);

            for (VariableElement field : fields) {
                String fieldName = field.getSimpleName().toString();
                if (isPrimitive(field)) {
                    equalsBuilder.addStatement("if ($L != that.$L) return false", fieldName, fieldName);
                } else {
                    equalsBuilder.addStatement("if (!$T.equals($L, that.$L)) return false", Objects.class, fieldName, fieldName);
                }
            }

            equalsBuilder.addStatement("return true");
            dtoClassBuilder.addMethod(equalsBuilder.build());

            // Add hashCode()
            MethodSpec.Builder hashCodeBuilder = MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $T.hash(" + fields.stream().map(f -> f.getSimpleName().toString()).collect(Collectors.joining(", ")) + ")", Objects.class);

            dtoClassBuilder.addMethod(hashCodeBuilder.build());
        }

        // Add toString() if requested
        if (annotation.generateToString()) {
            String formatStr = dtoClassName + "{" +
                fields.stream()
                    .map(f -> f.getSimpleName() + "=%s")
                    .collect(Collectors.joining(", ")) +
                "}";

            String args = fields.stream()
                .map(f -> f.getSimpleName().toString())
                .collect(Collectors.joining(", "));

            MethodSpec.Builder toStringBuilder = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);

            if (fields.isEmpty()) {
                toStringBuilder.addStatement("return \"" + dtoClassName + "{}\"");
            } else {
                toStringBuilder.addStatement("return $T.format(\"" + formatStr + "\", " + args + ")", String.class);
            }

            dtoClassBuilder.addMethod(toStringBuilder.build());
        }

        // Write DTO class
        JavaFile javaFile = JavaFile.builder(packageName, dtoClassBuilder.build())
            .build();
        javaFile.writeTo(filer);
    }

    private boolean isPrimitive(VariableElement field) {
        return field.asType().getKind().isPrimitive();
    }
}
