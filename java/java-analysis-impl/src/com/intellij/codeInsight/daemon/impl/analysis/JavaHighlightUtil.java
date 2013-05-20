/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.daemon.impl.analysis;

import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaHighlightUtil {
  public static boolean isSerializable(@NotNull PsiClass aClass) {
    PsiManager manager = aClass.getManager();
    PsiClass serializableClass = JavaPsiFacade.getInstance(manager.getProject()).findClass("java.io.Serializable", aClass.getResolveScope());
    return serializableClass != null && aClass.isInheritor(serializableClass, true);
  }

  public static boolean isSerializationRelatedMethod(PsiMethod method, PsiClass containingClass) {
    if (containingClass == null || method.isConstructor()) return false;
    if (method.hasModifierProperty(PsiModifier.STATIC)) return false;
    @NonNls String name = method.getName();
    PsiParameter[] parameters = method.getParameterList().getParameters();
    PsiType returnType = method.getReturnType();
    if ("readObjectNoData".equals(name)) {
      return parameters.length == 0 && TypeConversionUtil.isVoidType(returnType) && isSerializable(containingClass);
    }
    if ("readObject".equals(name)) {
      return parameters.length == 1
             && parameters[0].getType().equalsToText("java.io.ObjectInputStream")
             && TypeConversionUtil.isVoidType(returnType) && method.hasModifierProperty(PsiModifier.PRIVATE)
             && isSerializable(containingClass);
    }
    if ("readResolve".equals(name)) {
      return parameters.length == 0
             && returnType != null
             && returnType.equalsToText(CommonClassNames.JAVA_LANG_OBJECT)
             && (containingClass.hasModifierProperty(PsiModifier.ABSTRACT) || isSerializable(containingClass));
    }
    if ("writeReplace".equals(name)) {
      return parameters.length == 0
             && returnType != null
             && returnType.equalsToText(CommonClassNames.JAVA_LANG_OBJECT)
             && (containingClass.hasModifierProperty(PsiModifier.ABSTRACT) || isSerializable(containingClass));
    }
    if ("writeObject".equals(name)) {
      return parameters.length == 1
             && TypeConversionUtil.isVoidType(returnType)
             && parameters[0].getType().equalsToText("java.io.ObjectOutputStream")
             && method.hasModifierProperty(PsiModifier.PRIVATE)
             && isSerializable(containingClass);
    }
    return false;
  }

  @NotNull
  public static String formatType(@Nullable PsiType type) {
    if (type == null) return PsiKeyword.NULL;
    String text = type.getInternalCanonicalText();
    return text == null ? PsiKeyword.NULL : text;
  }
}
