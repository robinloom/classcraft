package com.robinloom.classcraft.processor;

import com.robinloom.classcraft.annotations.GenerateLogger;
import com.robinloom.classcraft.annotations.Ignore;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
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
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@javax.annotation.processing.SupportedAnnotationTypes("com.robinloom.classcraft.annotations.GenerateLogger")
@javax.annotation.processing.SupportedSourceVersion(SourceVersion.RELEASE_25)
public class LoggerProcessor extends AbstractProcessor {

    private static final List<String> LEVELS = List.of("trace", "debug", "info", "warn", "error");

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
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(GenerateLogger.class);

        for (Element element : elements) {
            if (!(element instanceof TypeElement classElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateLogger can only be applied to classes");
                continue;
            }

            if (classElement.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateLogger can only be applied to classes", classElement);
                continue;
            }

            try {
                generateLogger(classElement);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate logger: " + e.getMessage());
            }
        }

        return true;
    }

    private void generateLogger(TypeElement classElement) throws IOException {
        GenerateLogger annotation = classElement.getAnnotation(GenerateLogger.class);

        if (annotation == null) {
            return;
        }

        List<VariableElement> fields = classElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.FIELD)
            .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
            .filter(e -> e.getAnnotation(Ignore.class) == null)
            .map(e -> (VariableElement) e)
            .toList();

        Map<VariableElement, String> getterNames = new LinkedHashMap<>();
        for (VariableElement field : fields) {
            String getterName = findGetterName(classElement, field);
            if (getterName == null) {
                String capitalized = capitalize(field.getSimpleName().toString());
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@GenerateLogger requires a public getter get" + capitalized +
                        "() (or is" + capitalized + "() for boolean fields) for field '" + field.getSimpleName() + "'",
                    classElement);
                return;
            }
            getterNames.put(field, getterName);
        }

        ClassName targetClass = ClassName.get(classElement);
        ClassName slf4jLogger = ClassName.get("org.slf4j", "Logger");
        ClassName slf4jLoggerFactory = ClassName.get("org.slf4j", "LoggerFactory");
        ParameterizedTypeName stringConsumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), ClassName.get(String.class));

        PackageElement pkg = elements.getPackageOf(classElement);
        String packageName = pkg.getQualifiedName().toString();
        String loggerClassName = classElement.getSimpleName() + annotation.suffix();
        String varName = decapitalize(targetClass.simpleName());

        // Shared getter-call placeholders/args, reused by both line() and tree()
        String placeholders = fields.stream().map(f -> "$L.$L()").collect(Collectors.joining(", "));
        List<Object> getterCallArgs = new ArrayList<>();
        for (VariableElement field : fields) {
            getterCallArgs.add(varName);
            getterCallArgs.add(getterNames.get(field));
        }

        TypeSpec.Builder loggerBuilder = TypeSpec.classBuilder(loggerClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addJavadoc("Auto-generated renderer/logger for $L.\n\n" +
                    "line($L) is a single-line, compact rendering — safe for log\n" +
                    "pipelines that treat one line as one entry. tree($L) is a\n" +
                    "JWeaver-style multi-line tree — for console/debug output. Both\n" +
                    "read fields through their getter, no toString(), no reflection.\n" +
                    "The (message, $L, Consumer<String>) overloads hand either\n" +
                    "rendering to any consumer (a logger method reference,\n" +
                    "System.out::println, a file writer, ...). trace/debug/info/warn/error\n" +
                    "are SLF4J convenience wrappers built on line().\n",
                targetClass.simpleName(), targetClass.simpleName(), targetClass.simpleName(), targetClass.simpleName())
            .addField(FieldSpec.builder(slf4jLogger, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.getLogger($T.class)", slf4jLoggerFactory, targetClass)
                .build());

        // Private constructor to prevent instantiation
        loggerBuilder.addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build());

        // line(T instance) -> single-line, compact: ClassName{field="value", ...}
        StringBuilder lineFormat = new StringBuilder(targetClass.simpleName()).append("{");
        for (int i = 0; i < fields.size(); i++) {
            VariableElement field = fields.get(i);
            if (i > 0) {
                lineFormat.append(", ");
            }
            boolean isString = field.asType().toString().equals("java.lang.String");
            lineFormat.append(field.getSimpleName()).append(isString ? "=\"%s\"" : "=%s");
        }
        lineFormat.append("}");

        loggerBuilder.addMethod(buildFormatMethod("line", targetClass, varName, lineFormat.toString(), placeholders, getterCallArgs, fields.isEmpty()));

        // tree(T instance) -> JWeaver-style multi-line tree of the direct fields
        StringBuilder treeFormat = new StringBuilder(targetClass.simpleName());
        for (int i = 0; i < fields.size(); i++) {
            VariableElement field = fields.get(i);
            boolean isLast = i == fields.size() - 1;
            String connector = isLast ? "`-- " : "|-- ";
            boolean isString = field.asType().toString().equals("java.lang.String");
            String valueFormat = isString ? "\"%s\"" : "%s";
            treeFormat.append("\n").append(connector).append(field.getSimpleName()).append("=").append(valueFormat);
        }

        loggerBuilder.addMethod(buildFormatMethod("tree", targetClass, varName, treeFormat.toString(), placeholders, getterCallArgs, fields.isEmpty()));

        // line(String message, T instance, Consumer<String> consumer) -> stays on ONE line
        loggerBuilder.addMethod(MethodSpec.methodBuilder("line")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(String.class, "message")
            .addParameter(targetClass, varName)
            .addParameter(stringConsumer, "consumer")
            .addStatement("consumer.accept(message + $S + line($L))", " ", varName)
            .build());

        // tree(String message, T instance, Consumer<String> consumer) -> message, then the tree
        loggerBuilder.addMethod(MethodSpec.methodBuilder("tree")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(String.class, "message")
            .addParameter(targetClass, varName)
            .addParameter(stringConsumer, "consumer")
            .addStatement("consumer.accept(message + $S + tree($L))", "\n", varName)
            .build());

        // trace/debug/info/warn/error(String message, T instance) -> SLF4J sugar, built on line()
        for (String level : LEVELS) {
            loggerBuilder.addMethod(MethodSpec.methodBuilder(level)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String.class, "message")
                .addParameter(targetClass, varName)
                .addStatement("line(message, $L, log::$L)", varName, level)
                .build());
        }

        JavaFile javaFile = JavaFile.builder(packageName, loggerBuilder.build()).build();
        javaFile.writeTo(filer);
    }

    private MethodSpec buildFormatMethod(String methodName, ClassName targetClass, String varName,
                                          String format, String placeholders, List<Object> getterCallArgs,
                                          boolean noFields) {
        List<Object> args = new ArrayList<>();
        args.add(String.class);
        args.add(format);
        args.addAll(getterCallArgs);

        String statement = "return $T.format($S" + (noFields ? "" : ", " + placeholders) + ")";

        return MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(targetClass, varName)
            .returns(String.class)
            .addStatement(statement, args.toArray())
            .build();
    }

    /**
     * Resolves the getter to call for a field: prefers isX() for boolean fields
     * when that method actually exists on the class, otherwise getX().
     * Returns null if neither is found.
     */
    private String findGetterName(TypeElement classElement, VariableElement field) {
        String capitalized = capitalize(field.getSimpleName().toString());
        List<String> candidates = field.asType().getKind() == TypeKind.BOOLEAN
            ? List.of("is" + capitalized, "get" + capitalized)
            : List.of("get" + capitalized);

        for (String candidate : candidates) {
            boolean exists = classElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> (ExecutableElement) e)
                .anyMatch(m -> m.getSimpleName().toString().equals(candidate)
                    && m.getParameters().isEmpty()
                    && m.getModifiers().contains(Modifier.PUBLIC));
            if (exists) {
                return candidate;
            }
        }
        return null;
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String decapitalize(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
