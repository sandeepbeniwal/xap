/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
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

package com.gigaspaces.internal.query;

import com.gigaspaces.internal.io.IOUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;

/**
 * AbstractCompoundIndexScanner defines an index scanner that scans more than one space index.
 *
 * @author anna
 * @since 8.0.1
 */
public abstract class AbstractCompoundIndexScanner implements IQueryIndexScanner {
    private static final long serialVersionUID = 1L;

    protected List<IQueryIndexScanner> indexScanners;

    public List<IQueryIndexScanner> getIndexScanners() {
        return indexScanners;
    }

    public AbstractCompoundIndexScanner() {
        indexScanners = new LinkedList<IQueryIndexScanner>();
    }

    public AbstractCompoundIndexScanner(List<IQueryIndexScanner> indexScanners) {
        super();
        this.indexScanners = indexScanners;
    }
    @Override
    public boolean  isUidsScanner() {return false;}

    public void writeExternal(ObjectOutput out) throws IOException {
        IOUtils.writeObject(out, indexScanners);
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        indexScanners = IOUtils.readObject(in);
    }

    @Override
    public boolean isExtendsAbstractQueryIndex() {
        return false;
    }
}