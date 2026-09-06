package com.robinloom.classcraft.processor;

import com.robinloom.classcraft.annotations.GenerateBuilder;
import com.squareup.javapoet.ClassName;
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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@javax.annotation.processing.SupportedAnnotationTypes("com.robinloom.classcraft.annotations.GenerateBuilder")
@javax.annotation.processing.SupportedSourceVersion(SourceVersion.RELEASE_25)
public class BuilderProcessor extends AbstractProcessor {

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
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(GenerateBuilder.class);

        for (Element element : elements) {
            if (!(element instanceof ExecutableElement constructor)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateBuilder can only be applied to constructors");
                continue;
            }

            TypeElement enclosingClass = (TypeElement) constructor.getEnclosingElement();

            // Validate: not private
            if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateBuilder constructor cannot be private — generated builder needs to call it",
                    element);
                continue;
            }

            // Validate: only one per class
            long count = enclosingClass.getEnclosedElements().stream()
                .filter(e -> e instanceof ExecutableElement)
                .filter(e -> ((ExecutableElement) e).getSimpleName().toString().equals("<init>"))
                .filter(e -> e.getAnnotation(GenerateBuilder.class) != null)
                .count();
            if (count > 1) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateBuilder: only one constructor per class may be annotated",
                    element);
                continue;
            }

            try {
                generateBuilder(constructor, enclosingClass);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate builder: " + e.getMessage());
            }
        }

        return true;
    }

    private void generateBuilder(ExecutableElement constructor, TypeElement enclosingClass) throws IOException {
        List<VariableElement> params = new ArrayList<>(constructor.getParameters());
        List<VariableElement> required = new ArrayList<>();
        List<VariableElement> optional = new ArrayList<>();

        // Partition parameters
        for (VariableElement param : params) {
            javax.lang.model.type.TypeMirror paramType = param.asType();
            boolean isPrimitive = paramType.getKind().isPrimitive();

            // Check annotations on the type (type use annotations like @Nullable String)
            boolean hasNullableAnnotation = isNullableType(paramType);

            if (hasNullableAnnotation && !isPrimitive) {
                optional.add(param);
            } else {
                required.add(param);
            }
        }

        String builderClassName = enclosingClass.getSimpleName() + "Builder";
        ClassName targetClass = ClassName.get(enclosingClass);
        PackageElement pkg = elements.getPackageOf(enclosingClass);
        String packageName = pkg.getQualifiedName().toString();

        // Generate builder class
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Add static builder() method
        MethodSpec.Builder builderMethodBuilder = MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(required.isEmpty() ? getStageInterface(packageName, builderClassName, "BuildStage") :
                getStageInterface(packageName, builderClassName, getFirstStageName(required.getFirst())));

        builderMethodBuilder.addStatement("return new $L()", "Stages");
        builderClassBuilder.addMethod(builderMethodBuilder.build());

        // Generate stage interfaces
        List<String> stageNames = new ArrayList<>();
        for (int i = 0; i < required.size(); i++) {
            VariableElement param = required.get(i);
            String stageName = getStageName(param);
            stageNames.add(stageName);

            TypeSpec.Builder stageBuilder = TypeSpec.interfaceBuilder(stageName)
                .addModifiers(Modifier.PUBLIC);

            // Return type is next stage or BuildStage if last required
            TypeName returnType = i == required.size() - 1
                ? getStageInterface(packageName, builderClassName, "BuildStage")
                : getStageInterface(packageName, builderClassName, getStageName(required.get(i + 1)));

            stageBuilder.addMethod(MethodSpec.methodBuilder(param.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.get(param.asType()), param.getSimpleName().toString())
                .returns(returnType)
                .build());

            builderClassBuilder.addType(stageBuilder.build());
        }

        // Add BuildStage interface
        TypeSpec.Builder buildStageBuilder = TypeSpec.interfaceBuilder("BuildStage")
            .addModifiers(Modifier.PUBLIC);

        // Add optional field methods
        for (VariableElement param : optional) {
            buildStageBuilder.addMethod(MethodSpec.methodBuilder(param.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.get(param.asType()), param.getSimpleName().toString())
                .returns(getStageInterface(packageName, builderClassName, "BuildStage"))
                .build());
        }

        // Add build() method
        buildStageBuilder.addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(targetClass)
            .build());

        builderClassBuilder.addType(buildStageBuilder.build());

        // Generate Stages implementation class
        TypeSpec.Builder stagesClassBuilder = TypeSpec.classBuilder("Stages")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        // Add all stage interfaces to implements clause
        for (String stageName : stageNames) {
            stagesClassBuilder.addSuperinterface(getStageInterface(packageName, builderClassName, stageName));
        }
        stagesClassBuilder.addSuperinterface(getStageInterface(packageName, builderClassName, "BuildStage"));

        // Add fields for all parameters
        for (VariableElement param : params) {
            stagesClassBuilder.addField(TypeName.get(param.asType()), param.getSimpleName().toString(), Modifier.PRIVATE);
        }

        // Add methods for required parameters
        for (int i = 0; i < required.size(); i++) {
            VariableElement param = required.get(i);
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(param.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(param.asType()), param.getSimpleName().toString())
                .addStatement("this.$L = $L", param.getSimpleName(), param.getSimpleName());

            if (i == required.size() - 1) {
                // Last required, return BuildStage
                methodBuilder.returns(getStageInterface(packageName, builderClassName, "BuildStage"));
            } else {
                // Return next stage
                methodBuilder.returns(getStageInterface(packageName, builderClassName, getStageName(required.get(i + 1))));
            }

            methodBuilder.addStatement("return this");
            stagesClassBuilder.addMethod(methodBuilder.build());
        }

        // Add methods for optional parameters
        for (VariableElement param : optional) {
            MethodSpec method = MethodSpec.methodBuilder(param.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(param.asType()), param.getSimpleName().toString())
                .returns(getStageInterface(packageName, builderClassName, "BuildStage"))
                .addStatement("this.$L = $L", param.getSimpleName(), param.getSimpleName())
                .addStatement("return this")
                .build();
            stagesClassBuilder.addMethod(method);
        }

        // Add build() method
        StringBuilder constructorCall = new StringBuilder(targetClass.simpleName() + "(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) constructorCall.append(", ");
            constructorCall.append(params.get(i).getSimpleName());
        }
        constructorCall.append(")");

        stagesClassBuilder.addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(targetClass)
            .addStatement("return new $L", constructorCall.toString())
            .build());

        builderClassBuilder.addType(stagesClassBuilder.build());

        // Write builder class
        JavaFile javaFile = JavaFile.builder(packageName, builderClassBuilder.build())
            .build();
        javaFile.writeTo(filer);
    }

    private String getFirstStageName(VariableElement param) {
        return getStageName(param);
    }

    private String getStageName(VariableElement param) {
        return param.getSimpleName().toString().substring(0, 1).toUpperCase() +
            param.getSimpleName().toString().substring(1) + "Stage";
    }

    private TypeName getStageInterface(String packageName, String builderClassName, String interfaceName) {
        return ClassName.get(packageName, builderClassName, interfaceName);
    }

    private boolean isNullableType(javax.lang.model.type.TypeMirror type) {
        return type.getAnnotationMirrors().stream()
            .anyMatch(this::isNullableAnnotation);
    }

    private boolean isNullableAnnotation(javax.lang.model.element.AnnotationMirror annotation) {
        String annotationType = annotation.getAnnotationType().toString();

        // Supported nullability frameworks
        String[] nullableFrameworks = {
            "org.jspecify.annotations.Nullable",           // JSpecify
            "javax.annotation.Nullable",                    // Jakarta/old Java Standard
            "org.jetbrains.annotations.Nullable",           // IntelliJ IDEA
            "org.checkerframework.checker.nullness.qual.Nullable",  // Checker Framework
            "com.robinloom.classcraft.annotations.Nullable" // ClassCraft's own
        };

        for (String framework : nullableFrameworks) {
            if (annotationType.equals(framework)) {
                return true;
            }
        }
        return false;
    }
}
