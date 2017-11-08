package com.permmision;


import com.annotation.ActivityPermission;
import com.annotation.FragmentPermission;
import com.annotation.OnDenied;
import com.annotation.OnGranted;
import com.annotation.OnGrantedListener;
import com.annotation.OnNeverAsk;
import com.annotation.OnShowRationale;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
/**
 * Created by Harry.Kong.
 * Time 2017/11/8.
 * Description:
 */
@AutoService(Processor.class)
public class PermissionProcessor extends AbstractProcessor {

    private Elements elementUtils;

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        annotations.add(OnDenied.class);
        annotations.add(OnGranted.class);
        annotations.add(OnNeverAsk.class);
        annotations.add(OnShowRationale.class);
        return annotations;
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elementUtils = env.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!checkIntegrity(roundEnv))
            return false;
        Set<? extends Element> elementActivities = roundEnv.getElementsAnnotatedWith(ActivityPermission.class);
        Set<? extends Element> elementFragments = roundEnv.getElementsAnnotatedWith(FragmentPermission.class);
        return makeListenerJavaFile(elementActivities) && makeListenerJavaFile(elementFragments);
    }

    /**
     * 动态构建Java类
     */
    private boolean makeListenerJavaFile(Set<? extends Element> elements) {
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            List<? extends Element> members = elementUtils.getAllMembers(typeElement);

            TypeSpec.Builder builder = TypeSpec.classBuilder(element.getSimpleName() + "$OnGrantedListener")//创建类名
                    .addSuperinterface(ParameterizedTypeName.get(ClassName.bestGuess(OnGrantedListener.class.getTypeName()), ClassName.bestGuess(element.getSimpleName().toString())))//创建类实现的方法
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addField(Arrays.class, "mArrays");//创建  Arrays mArrays;

            MethodSpec.Builder grantedMethodSpecBuilder = MethodSpec.methodBuilder("onGranted")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.VOID)
                    .addParameter(ClassName.get(typeElement.asType()), "target")
                    .addParameter(String[].class, "permissions");

            MethodSpec.Builder denyMethodSpecBuilder = MethodSpec.methodBuilder("onDenied")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.VOID)
                    .addParameter(ClassName.get(typeElement.asType()), "target")
                    .addParameter(String[].class, "permissions");

            MethodSpec.Builder neverAskMethodSpecBuilder = MethodSpec.methodBuilder("onNeverAsk")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.VOID)
                    .addParameter(ClassName.get(typeElement.asType()), "target")
                    .addParameter(String[].class, "permissions");

            MethodSpec.Builder showRationaleMethodSpecBuilder = MethodSpec.methodBuilder("onShowRationale")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.VOID)
                    .addParameter(ClassName.get(typeElement.asType()), "target")
                    .addParameter(String[].class, "permissions");

            for (Element item : members) {
                OnGranted onGranted = item.getAnnotation(OnGranted.class);
                OnDenied onDenied = item.getAnnotation(OnDenied.class);
                OnNeverAsk onNeverAsk = item.getAnnotation(OnNeverAsk.class);
                OnShowRationale onShowRationale = item.getAnnotation(OnShowRationale.class);
                if (onGranted != null) {
                    String[] params = onGranted.value();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("{");
                    for (int i = 0; i < params.length; i++) {
                        stringBuilder.append("\"")
                                .append(params[i])
                                .append("\"")
                                .append(",");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    stringBuilder.append("}");
                    grantedMethodSpecBuilder.addStatement(String.format("if(Arrays.equals(permissions,new String[] %s)){ \ntarget.%s() ; \nreturn ;\n}", stringBuilder.toString(), item.getSimpleName().toString()));
                } else if (onDenied != null) {
                    String[] params = onDenied.value();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("{");
                    for (int i = 0; i < params.length; i++) {
                        stringBuilder.append("\"")
                                .append(params[i])
                                .append("\"")
                                .append(",");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    stringBuilder.append("}");
                    denyMethodSpecBuilder.addStatement(String.format("if(Arrays.equals(permissions,new String[] %s)) {\ntarget.%s() ;\n return ;\n} ", stringBuilder.toString(), item.getSimpleName().toString()));
                } else if (onNeverAsk != null) {
                    String[] params = onNeverAsk.value();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("{");
                    for (int i = 0; i < params.length; i++) {
                        stringBuilder.append("\"")
                                .append(params[i])
                                .append("\"")
                                .append(",");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    stringBuilder.append("}");
                    neverAskMethodSpecBuilder.addStatement(String.format("if(Arrays.equals(permissions,new String[] %s)) {\ntarget.%s() ;\n return ; \n} ", stringBuilder.toString(), item.getSimpleName().toString()));
                } else if (onShowRationale != null) {
                    String[] params = onShowRationale.value();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("{");
                    for (int i = 0; i < params.length; i++) {
                        stringBuilder.append("\"")
                                .append(params[i])
                                .append("\"")
                                .append(",");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    stringBuilder.append("}");
                    showRationaleMethodSpecBuilder.addStatement(String.format("if(Arrays.equals(permissions,new String[] %s)) {\ntarget.%s() ;\nreturn;\n} ", stringBuilder.toString(), item.getSimpleName().toString()));
                }
            }
            grantedMethodSpecBuilder.addStatement("throw new RuntimeException(String.format(\"Unable to find callbacks for permissions %s\",Arrays.toString(permissions)))");
            builder.addMethod(grantedMethodSpecBuilder.build());
            neverAskMethodSpecBuilder.addStatement("throw new RuntimeException(String.format(\"Unable to find callbacks for permissions %s\",Arrays.toString(permissions)))");
            builder.addMethod(neverAskMethodSpecBuilder.build());
            showRationaleMethodSpecBuilder.addStatement("throw new RuntimeException(String.format(\"Unable to find callbacks for permissions %s\",Arrays.toString(permissions)))");
            builder.addMethod(showRationaleMethodSpecBuilder.build());
            denyMethodSpecBuilder.addStatement("throw new RuntimeException(String.format(\"Unable to find callbacks for permissions %s\",Arrays.toString(permissions)))");
            builder.addMethod(denyMethodSpecBuilder.build());
            TypeSpec typeSpec = builder.build();
            JavaFile javaFile = JavaFile.builder(getPackageName(typeElement), typeSpec)
                    .build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private String[] getValues(Element item) {

        OnGranted tmpOnGranted = item.getAnnotation(OnGranted.class);
        if (tmpOnGranted != null) {
            return tmpOnGranted.value();
        }
        OnDenied tmpOnDenied = item.getAnnotation(OnDenied.class);
        if (tmpOnDenied != null) {
            return tmpOnDenied.value();
        }
        OnNeverAsk tmpNeverAsk = item.getAnnotation(OnNeverAsk.class);
        if (tmpNeverAsk != null) {
            return tmpNeverAsk.value();
        }
        OnShowRationale tmpShowRationale = item.getAnnotation(OnShowRationale.class);
        if (tmpShowRationale != null) {
            return tmpShowRationale.value();
        }
        return null;
    }

    /**
     * 安全性检查，ActivityPermission与FragmentPermission声明的类，四个函数必须完整
     */
    private boolean checkIntegrity(RoundEnvironment roundEnv) {

        Set<? extends Element> elementActivities = roundEnv.getElementsAnnotatedWith(ActivityPermission.class);
        Set<? extends Element> elementFragments = roundEnv.getElementsAnnotatedWith(FragmentPermission.class);
        return matchCallBacks(elementActivities) && matchCallBacks(elementFragments);
    }

    /**
     * 四个函数必须完整
     */
    private boolean matchCallBacks(Set<? extends Element> elements) {

        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            List<? extends Element> members = elementUtils.getAllMembers(typeElement);
            for (Element item : members) {
                String[] params = getValues(item);
                if (params != null) {
                    Element deny = null;
                    Element neverAsk = null;
                    Element showRationale = null;
                    Element granted = null;
                    for (Element other : members) {

                        OnGranted tmpOnGranted = other.getAnnotation(OnGranted.class);
                        if (tmpOnGranted != null && Arrays.equals(tmpOnGranted.value(), params)) {
                            granted = other;
                            continue;
                        }
                        OnDenied tmpOnDenied = other.getAnnotation(OnDenied.class);
                        if (tmpOnDenied != null && Arrays.equals(tmpOnDenied.value(), params)) {
                            deny = other;
                            continue;
                        }
                        OnNeverAsk tmpNeverAsk = other.getAnnotation(OnNeverAsk.class);
                        if (tmpNeverAsk != null && Arrays.equals(tmpNeverAsk.value(), params)) {
                            neverAsk = other;
                            continue;
                        }
                        OnShowRationale tmpShowRationale = other.getAnnotation(OnShowRationale.class);
                        if (tmpShowRationale != null && Arrays.equals(tmpShowRationale.value(), params)) {
                            showRationale = other;
                        }
                    }

                    if (granted == null) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "need OnGranted func  " + Arrays.toString(params), element);
                        return false;
                    }
                    if (deny == null) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "need OnDenied func " + Arrays.toString(params), element);
                        return false;
                    }
                    if (neverAsk == null) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "need OnNeverAsk func " + Arrays.toString(params), element);
                        return false;
                    }
                    if (showRationale == null) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "need OnShowRationale func " + Arrays.toString(params), element);
                        return false;
                    }
                }
            }

        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();

    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }
}