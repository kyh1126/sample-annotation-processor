package me.jenny;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

// 얘를 Processor 로 등록해달라
// resources/META-INF/services/javax.annotation.processor.Processor
// 내부에 me.jenny.MagicMojaProcessor 써놓은 파일 생성하는걸
// 컴파일 시점에 애노테이션 프로세서를 사용하여 자동으로 대신 생성해준다.
@AutoService(Processor.class)
public class MagicMojaProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Magic.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // getElementsAnnotatedWith: 어노테이션 붙어있는 elements 가져온다.
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Magic.class);

        for (Element element : elements) {
            Name elementName = element.getSimpleName();
            if (element.getKind() != ElementKind.INTERFACE) {
                // 이렇게 하면 컴파일 하다가 에러난다.
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Magic annotation can not be used on " + elementName);
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing " + elementName);
            }

            // Javapoet: 소스코드 생성 유틸리티
            TypeElement typeElement = (TypeElement) element;
            ClassName className = ClassName.get(typeElement);               // 클래스 정보 갖고있다.

            // 메소드 만들자
            MethodSpec pullOut = MethodSpec.methodBuilder("pullOut")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addStatement("return $S", "Rabbit")                    // 메소드 안에 statement 정의 (return 'Rabbit';)
                    .build();

            // 타입을 만든다.
            TypeSpec magicMoja = TypeSpec.classBuilder("MagicMoja")    // 빌더 안에 simple name 에 해당하는 이름 주면 된다.
                    .addModifiers(Modifier.PUBLIC)
                    // 애노테이션 붙어있는 className 을 구현하는(implements) MagicMoja 클래스
                    .addSuperinterface(className)
                    .addMethod(pullOut)
                    .build();

            // 위는 메모리 상에 객체로만 클래스를 정의한거고,
            // 실제 소스코드를 만드는 부분
            // processingEnv: AbstractProcessor를 구현하면 이 ProcessingEnvironment 를 사용할 수 있다.
            // Filer: 소스코드, 클래스코드 및 리소스를 생성할 수 있는 인터페이스
            Filer filer = processingEnv.getFiler();
            // filer.create 종류로 소스파일, 컴파일파일 등 생성할 수 있다.

            try {
                // 자바 파일 만든다.
                JavaFile.builder(className.packageName(), magicMoja)        // magicMoja 라는 타입을 이 패키지 안에 만들겠다.
                        .build()                                            // 만들어달라
                        .writeTo(filer);                                    // processor 가 제공하는 filer 에 바로 쓸 수 있다.
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: " + e);
            }
        }
        return true;
    }
}
