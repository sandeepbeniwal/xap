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

package org.openspaces.persistency.cassandra.error;

/**
 * A runtime exception for exceptions that occur during queries or during iteration over a result
 * set.
 *
 * @author Dan Kilman
 * @since 9.1.1
 */
public class SpaceCassandraQueryExecutionException extends SpaceCassandraException {
    private static final long serialVersionUID = 1L;

    public SpaceCassandraQueryExecutionException(String message, Throwable t) {
        super(message, t);
    }

}
