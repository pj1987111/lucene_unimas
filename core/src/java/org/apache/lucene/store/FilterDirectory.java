/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.store;


import org.apache.lucene.codecs.lucene54.Lucene54DocValuesFormat;
import org.apache.lucene.util.unimas.PathUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Directory implementation that delegates calls to another directory.
 * This class can be used to add limitations on top of an existing
 * {@link Directory} implementation such as
 * {@link NRTCachingDirectory} or to add additional
 * sanity checks for tests. However, if you plan to write your own
 * {@link Directory} implementation, you should consider extending directly
 * {@link Directory} or {@link BaseDirectory} rather than try to reuse
 * functionality of existing {@link Directory}s by extending this class.
 *
 * @lucene.internal
 */
public abstract class FilterDirectory extends Directory {

    /**
     * Get the wrapped instance by <code>dir</code> as long as this reader is
     * an instance of {@link FilterDirectory}.
     */
    public static Directory unwrap(Directory dir) {
        while (dir instanceof FilterDirectory) {
            dir = ((FilterDirectory) dir).in;
        }
        return dir;
    }

    protected final Directory in;

    /**
     * Sole constructor, typically called from sub-classes.
     */
    protected FilterDirectory(Directory in) {
        this.in = in;
    }

    /**
     * Return the wrapped {@link Directory}.
     */
    public final Directory getDelegate() {
        return in;
    }

    @Override
    public String[] listAll() throws IOException {
        return in.listAll();
    }

    @Override
    public void deleteFile(String name) throws IOException {
        Directory tempDir = in;
        if (PathUtil.newPath != null && PathUtil.newPath.length() > 0 &&
                (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION) || name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                        || name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION))) {
            tempDir = PathUtil.reCreatePath(in);
        }
        tempDir.deleteFile(name);
    }

    @Override
    public long fileLength(String name) throws IOException {
        Directory tempDir = in;
        if (PathUtil.newPath != null && PathUtil.newPath.length() > 0 &&
                (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION) || name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                        || name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION))) {
            tempDir = PathUtil.reCreatePath(in);
        }
        return tempDir.fileLength(name);
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context)
            throws IOException {
        Directory tempDir = in;
        if (PathUtil.newPath != null && PathUtil.newPath.length() > 0 &&
                (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION) || name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                        || name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION))) {
            tempDir = PathUtil.reCreatePath(in);
        }
        return tempDir.createOutput(name, context);
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        return in.createTempOutput(prefix, suffix, context);
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        List<String> newPathDocValues = null;
        List<String> others = new ArrayList<>();
        if (PathUtil.newPath != null && PathUtil.newPath.length() > 0) {
            newPathDocValues = new ArrayList<>();
        }
        for (String name : names) {
            if(newPathDocValues!=null &&
                    (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION) || name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                            || name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION))) {
                newPathDocValues.add(name);
            } else
                others.add(name);
        }
//        in.sync(names);
        in.sync(others);
        if(newPathDocValues!=null)
            PathUtil.reCreatePath(in).sync(newPathDocValues);
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        in.rename(source, dest);
    }

    @Override
    public void syncMetaData() throws IOException {
        in.syncMetaData();
    }

    @Override
    public IndexInput openInput(String name, IOContext context)
            throws IOException {
        Directory tempDir = in;
        if (PathUtil.newPath != null && PathUtil.newPath.length() > 0 &&
                (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION) || name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                        || name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION))) {
            tempDir = PathUtil.reCreatePath(in);
        }
        return tempDir.openInput(name, context);
    }

    @Override
    public Lock obtainLock(String name) throws IOException {
        return in.obtainLock(name);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + in.toString() + ")";
    }

}
