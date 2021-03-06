package com.helospark.spark.converter.handlers.service.emitter;

import java.util.List;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.BadLocationException;

import com.helospark.spark.converter.handlers.domain.ConverterInputParameters;
import com.helospark.spark.converter.handlers.domain.ConverterMethodCodeGenerationRequest;
import com.helospark.spark.converter.handlers.domain.ConverterTypeCodeGenerationRequest;
import com.helospark.spark.converter.handlers.service.common.domain.CompilationUnitModificationDomain;
import com.helospark.spark.converter.handlers.service.emitter.helper.ClassTypeAppender;
import com.helospark.spark.converter.handlers.service.emitter.helper.ModifiableCompilationUnitCreator;
import com.helospark.spark.converter.handlers.service.emitter.helper.PackageRootFinder;
import com.helospark.spark.converter.handlers.service.emitter.helper.TypeDeclarationGenerator;

public class UnitTestCodeEmitter {
    private TypeDeclarationGenerator typeDeclarationGenerator;
    private ClassTypeAppender classTypeAppender;
    private ModifiableCompilationUnitCreator modifiableCompilationUnitCreator;
    private PackageRootFinder packageRootFinder;

    public UnitTestCodeEmitter(TypeDeclarationGenerator typeDeclarationGenerator, ClassTypeAppender classTypeAppender,
            ModifiableCompilationUnitCreator modifiableCompilationUnitCreator, PackageRootFinder packageRootFinder) {
        this.typeDeclarationGenerator = typeDeclarationGenerator;
        this.classTypeAppender = classTypeAppender;
        this.modifiableCompilationUnitCreator = modifiableCompilationUnitCreator;
        this.packageRootFinder = packageRootFinder;
    }

    public void emitUnitTest(ConverterInputParameters converterInputParameters, List<ConverterTypeCodeGenerationRequest> collectedConverters) {
        collectedConverters.stream()
                .forEach(converter -> emitUnitTestForConverter(converterInputParameters, converter));
    }

    private void emitUnitTestForConverter(ConverterInputParameters converterInputParameters, ConverterTypeCodeGenerationRequest request) {
        try {
            CompilationUnitModificationDomain compilationUnit = createCompilationUnit(converterInputParameters, request);
            TypeDeclaration unitTestType = createUnitTestClass(request.getClassName() + "Test", compilationUnit);

            MethodDeclaration initMethod = addMockInitializationMethod(compilationUnit);
            addDependenciesAsMocks(compilationUnit, request);
            addUnderTest(compilationUnit);

            request.getMethods()
                    .stream()
                    .forEach(method -> addTestMethods(unitTestType, compilationUnit, method));

            addTypeToCompilationUnit(compilationUnit, unitTestType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Block createInitializationBody(CompilationUnitModificationDomain compilationUnit, ConverterTypeCodeGenerationRequest generationRequest, TypeDeclaration unitTestType) {
        AST ast = compilationUnit.getAst();
        Block block = ast.newBlock();
        MethodInvocation setUpMethod = ast.newMethodInvocation();
        setUpMethod.setName(ast.newSimpleName("initMocks"));
        setUpMethod.arguments().add(ast.newThisExpression());
        block.statements().add(setUpMethod);
        block.statements().add();
        Object dummyInitializationMethod = createDummyCreationMethod(compilationUnit, generationRequest);
        unitTestType.bodyDeclarations().add(dummyInitializationMethod);
        return block;
    }

    private void addUnderTest(CompilationUnitModificationDomain compilationUnitModificationDomain, TypeDeclaration newType, ConverterTypeCodeGenerationRequest generationRequest) {
        FieldDeclaration underTest = fieldDeclarationBuilder.getFieldDeclaration(compilationUnitModificationDomain, "underTest", generationRequest.getClassName(),
                generationRequest.getFullyQualifiedName());
        newType.bodyDeclarations().add(underTest);
    }

    private MethodDeclaration addMockInitializationMethod(CompilationUnitModificationDomain compilationUnitModificationDomain) {
        AST ast = compilationUnitModificationDomain.getAst();
        MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setName(ast.newSimpleName("setUp"));
        methodDeclaration.setBody(createInitializationBody(ast));
        return methodDeclaration;
    }

    private void addDependenciesAsMocks(CompilationUnitModificationDomain compilationUnitModificationDomain, TypeDeclaration unitTestType,
            ConverterTypeCodeGenerationRequest generationRequest) {
        for (ConverterTypeCodeGenerationRequest dependency : generationRequest.getDependencies()) {
            FieldDeclaration fieldDeclaration = fieldDeclarationBuilder.getFieldDeclaration(compilationUnitModificationDomain, generationRequest.getUnitTestMockName(),
                    generationRequest.getClassName(),
                    generationRequest.getFullyQualifiedName());
            Annotation mockAnnotation = markerAnnotationBuilder.buildAnnotation(compilationUnitModificationDomain, "org.mockito.Mock");
            fieldDeclaration.modifiers().add(mockAnnotation);
            unitTestType.bodyDeclarations().add(fieldDeclaration);
        }
    }

    private void addTestMethods(TypeDeclaration unitTestType, CompilationUnitModificationDomain compilationUnit, ConverterMethodCodeGenerationRequest method) {

    }

    private CompilationUnitModificationDomain createCompilationUnit(ConverterInputParameters converterInputParameters, ConverterTypeCodeGenerationRequest generationRequest) {
        IPackageFragmentRoot testRootFolder = packageRootFinder.findTestPackageFragmentRoot(converterInputParameters.getJavaProject());
        CompilationUnitModificationDomain compilationUnit = modifiableCompilationUnitCreator.create(testRootFolder, generationRequest.getPackageName(),
                generationRequest.getClassName() + "Test");
        return compilationUnit;
    }

    private void addTypeToCompilationUnit(CompilationUnitModificationDomain compilationUnitModificationDomain, TypeDeclaration newType)
            throws JavaModelException, BadLocationException {
        classTypeAppender.addType(compilationUnitModificationDomain, newType);
    }

    private TypeDeclaration createUnitTestClass(String className, CompilationUnitModificationDomain compilationUnitModificationDomain) {
        return typeDeclarationGenerator.createConverter(compilationUnitModificationDomain, className);
    }
}
