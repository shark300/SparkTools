package com.helospark.spark.converter.handlers.service.emitter.codegenerator.impl;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.helospark.spark.converter.handlers.domain.ConverterMethodCodeGenerationRequest;
import com.helospark.spark.converter.handlers.domain.ConverterTypeCodeGenerationRequest;
import com.helospark.spark.converter.handlers.service.domain.CompilationUnitModificationDomain;
import com.helospark.spark.converter.handlers.service.domain.ConvertType;
import com.helospark.spark.converter.handlers.service.domain.ConvertableDomainParameter;
import com.helospark.spark.converter.handlers.service.domain.SourceDestinationType;
import com.helospark.spark.converter.handlers.service.emitter.codegenerator.ConverterTypeCodeGenerator;
import com.helospark.spark.converter.handlers.service.emitter.codegenerator.impl.helper.CallableMethodGenerator;

public class ConvertCopyCodeGenerator implements ConverterTypeCodeGenerator {
    private CallableMethodGenerator callableMethodGenerator;

    public ConvertCopyCodeGenerator(CallableMethodGenerator callableMethodGenerator) {
        this.callableMethodGenerator = callableMethodGenerator;
    }

    @Override
    public Expression generate(CompilationUnitModificationDomain compilationUnitModificationDomain, Block body, Expression expression, Expression sourceObject,
            ConvertableDomainParameter parameter, ConverterTypeCodeGenerationRequest generationRequest, ConverterMethodCodeGenerationRequest method) {
        AST ast = compilationUnitModificationDomain.getAst();
        MethodInvocation newMethodInvocation = ast.newMethodInvocation();
        newMethodInvocation.setExpression(expression);
        newMethodInvocation.setName(ast.newSimpleName(parameter.getDestinationMethodName()));

        SourceDestinationType sourceDestinationType = new SourceDestinationType(parameter.getSourceType(), parameter.getDestinationType());
        MethodInvocation convertMethodCall = callableMethodGenerator.generateMethodCall(ast, generationRequest, method, sourceDestinationType);

        MethodInvocation getMethodInvocation = ast.newMethodInvocation();
        getMethodInvocation.setExpression(sourceObject);
        getMethodInvocation.setName(ast.newSimpleName(parameter.getSourceMethodName()));
        convertMethodCall.arguments().add(getMethodInvocation);

        newMethodInvocation.arguments().add(convertMethodCall);

        return newMethodInvocation;
    }

    @Override
    public boolean canHandle(ConvertType type) {
        return ConvertType.CONVERT.equals(type) || ConvertType.COLLECTION_CONVERT.equals(type);
    }

}
