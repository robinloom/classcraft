package com.robinloom.classcraft.processor;

import com.robinloom.classcraft.annotations.GenerateWither;
import com.robinloom.classcraft.annotations.Ignore;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
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

@javax.annotation.processing.SupportedAnnotationTypes("com.robinloom.classcraft.annotations.GenerateWither")
@javax.annotation.processing.SupportedSourceVersion(SourceVersion.RELEASE_25)
public class WitherProcessor extends AbstractProcessor {

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
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(GenerateWither.class);

        for (Element element : elements) {
            if (!(element instanceof TypeElement classElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateWither can only be applied to classes");
                continue;
            }

            if (classElement.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateWither can only be applied to classes", classElement);
                continue;
            }

            try {
                generateWither(classElement);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate wither: " + e.getMessage());
            }
        }

        return true;
    }

    private void generateWither(TypeElement classElement) throws IOException {
        GenerateWither annotation = classElement.getAnnotation(GenerateWither.class);

        if (annotation == null) {
            return;
        }

        List<VariableElement> fields = classElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.FIELD)
            .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
            .map(e -> (VariableElement) e)
            .toList();

        if (fields.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "@GenerateWither: class has no instance fields to generate wither methods for",
                classElement);
            return;
        }

        ExecutableElement constructor = findMatchingConstructor(classElement, fields);
        if (constructor == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "@GenerateWither requires exactly one constructor with parameters matching all fields by name",
                classElement);
            return;
        }

        for (VariableElement field : fields) {
            if (!hasGetter(classElement, field)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateWither requires a public getter get" + capitalize(field.getSimpleName().toString()) +
                        "() for field '" + field.getSimpleName() + "'",
                    classElement);
                return;
            }
        }

        List<VariableElement> witherableFields = fields.stream()
            .filter(f -> f.getAnnotation(Ignore.class) == null)
            .toList();

        if (witherableFields.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "@GenerateWither: all fields are @Ignore'd, nothing to generate",
                classElement);
            return;
        }

        ClassName targetClass = ClassName.get(classElement);
        PackageElement pkg = elements.getPackageOf(classElement);
        String packageName = pkg.getQualifiedName().toString();
        String witherClassName = classElement.getSimpleName() + annotation.suffix();

        TypeSpec.Builder witherBuilder = TypeSpec.classBuilder(witherClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addJavadoc("Auto-generated wither methods for $L.\n\n" +
                    "Assumes $L has a constructor with all fields as parameters\n" +
                    "and public getters for each field.\n",
                targetClass.simpleName(), targetClass.simpleName());

        // Private constructor to prevent instantiation
        witherBuilder.addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build());

        List<? extends VariableElement> ctorParams = constructor.getParameters();

        for (VariableElement field : witherableFields) {
            String fieldName = field.getSimpleName().toString();

            StringBuilder args = new StringBuilder();
            for (int i = 0; i < ctorParams.size(); i++) {
                if (i > 0) args.append(", ");
                String paramName = ctorParams.get(i).getSimpleName().toString();
                if (paramName.equals(fieldName)) {
                    args.append(fieldName);
                } else {
                    args.append("source.get").append(capitalize(paramName)).append("()");
                }
            }

            MethodSpec method = MethodSpec.methodBuilder("with" + capitalize(fieldName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(targetClass, "source")
                .addParameter(TypeName.get(field.asType()), fieldName)
                .returns(targetClass)
                .addStatement("return new $T($L)", targetClass, args.toString())
                .build();

            witherBuilder.addMethod(method);
        }

        JavaFile javaFile = JavaFile.builder(packageName, witherBuilder.build()).build();
        javaFile.writeTo(filer);
    }

    /**
     * Finds the single constructor whose parameters match all given fields by name
     * (order-independent). Returns null if there is no such constructor, or more than one.
     */
    private ExecutableElement findMatchingConstructor(TypeElement classElement, List<VariableElement> fields) {
        Set<String> fieldNames = fields.stream()
            .map(f -> f.getSimpleName().toString())
            .collect(Collectors.toSet());

        List<ExecutableElement> matches = classElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
            .map(e -> (ExecutableElement) e)
            .filter(ctor -> {
                List<? extends VariableElement> params = ctor.getParameters();
                if (params.size() != fieldNames.size()) {
                    return false;
                }
                Set<String> paramNames = params.stream()
                    .map(p -> p.getSimpleName().toString())
                    .collect(Collectors.toSet());
                return paramNames.equals(fieldNames);
            })
            .toList();

        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean hasGetter(TypeElement classElement, VariableElement field) {
        String getterName = "get" + capitalize(field.getSimpleName().toString());
        return classElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.METHOD)
            .map(e -> (ExecutableElement) e)
            .anyMatch(m -> m.getSimpleName().toString().equals(getterName)
                && m.getParameters().isEmpty()
                && m.getModifiers().contains(Modifier.PUBLIC));
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
