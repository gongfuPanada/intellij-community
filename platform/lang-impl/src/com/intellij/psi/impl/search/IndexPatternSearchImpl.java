/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.psi.impl.search;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.search.IndexPattern;
import com.intellij.psi.search.IndexPatternProvider;
import com.intellij.psi.search.searches.IndexPatternSearch;

/**
 * @author yole
 */
class IndexPatternSearchImpl extends IndexPatternSearch {
  IndexPatternSearchImpl() {
    registerExecutor(new IndexPatternSearcher());
  }

  protected int getOccurrencesCountImpl(PsiFile file, IndexPatternProvider provider) {
    int count = CacheManager.SERVICE.getInstance(file.getProject()).getTodoCount(file.getVirtualFile(), provider);
    if (count != -1) return count;
    return search(file, provider).findAll().size();
  }

  protected int getOccurrencesCountImpl(PsiFile file, IndexPattern pattern) {
    int count = CacheManager.SERVICE.getInstance(file.getProject()).getTodoCount(file.getVirtualFile(), pattern);
    if (count != -1) return count;
    return search(file, pattern).findAll().size();
  }
}
