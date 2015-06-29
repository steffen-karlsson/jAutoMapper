package com.sk.aj.json;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;

/**
 * Created by steffenkarlsson on 6/29/15.
 */
final class GetterSetter {

    private enum MethodType {
        GET, SET
    }

    public static MethodSpec generateSetter(String name, TypeName type) {
        return build(name, MethodType.SET)
                .addParameter(type, name)
                .addStatement("this.$L = $L", name, name)
                .build();
    }

    public static MethodSpec generateGetter(String name, TypeName type) {
        return build(name, MethodType.GET)
                .addStatement("return $L", name)
                .returns(type)
                .build();
    }

    private static MethodSpec.Builder build(String name, MethodType methodType) {
        return MethodSpec.methodBuilder(findName(name, methodType))
                .addModifiers(Modifier.PUBLIC);
    }

    private static String findName(String name, MethodType type) {
        String typeName = type.toString().toLowerCase();
        return name.startsWith("_")
                ? String.format("%s%s", typeName, name)
                : String.format("%s%s%s", typeName, name.substring(0, 1).toUpperCase(), name.substring(1));

    }
}
