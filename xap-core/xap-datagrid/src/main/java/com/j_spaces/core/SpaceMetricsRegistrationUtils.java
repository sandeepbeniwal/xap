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
package com.j_spaces.core;

import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.server.metadata.IServerTypeDesc;
import com.gigaspaces.internal.server.space.SpaceEngine;
import com.gigaspaces.metadata.index.SpaceIndex;
import com.gigaspaces.metrics.Gauge;
import com.gigaspaces.metrics.MetricRegistrator;
import com.j_spaces.core.cache.context.IndexMetricsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @since 15.5.0
 */
public class SpaceMetricsRegistrationUtils {

    private Map<String, Map<String, LongAdder>> dataTypesIndexesHits = new ConcurrentHashMap<>();
    private static final Logger _logger = LoggerFactory.getLogger(com.gigaspaces.logger.Constants.LOGGER_CACHE);

    private final SpaceEngine spaceEngine;

    public SpaceMetricsRegistrationUtils(SpaceEngine spaceEngine){
        this.spaceEngine = spaceEngine;
    }

    public void registerTypeIndexMetrics(IServerTypeDesc serverTypeDesc) {

        String typeName = serverTypeDesc.getTypeName();
        ITypeDesc typeDesc = serverTypeDesc.getTypeDesc();
        Map<String, SpaceIndex> indexes = typeDesc.getIndexes();
        _logger.info( "TypeName:"  + typeName + ", indexes=" + indexes);
        if( indexes != null && !indexes.isEmpty() ) {
            Set<String> keys = indexes.keySet();
            //add type to datatTypes indexes hits map
            dataTypesIndexesHits.put( typeName, new ConcurrentHashMap<>() );
            MetricRegistrator registrator = spaceEngine.getMetricRegistrator();
            for (String index : keys) {
                _logger.info(">>> index=" + index);

                registerIndexRelatedMetrics(registrator, typeName, index);
            }
        }
    }

    private void registerIndexRelatedMetrics( final MetricRegistrator registrator, String typeName, String index ){

        spaceEngine.getDataTypeMetricRegistrator(typeName, index).register(registrator.toPath("data", "index-hit-total"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                Map<String, LongAdder> indexesMap = dataTypesIndexesHits.get(typeName);
                LongAdder indexHits = indexesMap.get(index);
                return indexHits == null ? 0 : indexHits.longValue();
            }
        });
    }

    public void unregisterTypeIndexMetrics( String typeName ){
        IServerTypeDesc serverTypeDesc = spaceEngine.getTypeTableEntry(typeName);
        if( serverTypeDesc != null ){
            //Map<String, SpaceIndex> indexes = classTypeInfo.getIndexes();
            ITypeDesc typeDesc = serverTypeDesc.getTypeDesc();
            Map<String, SpaceIndex> indexes = typeDesc.getIndexes();
            if( indexes != null && !indexes.isEmpty() ) {
                for (String key : indexes.keySet()) {
                    _logger.info(">>> before unregister key=" + key );
                    spaceEngine.getDataTypeMetricRegistrator( typeName, key ).unregisterByPrefix( spaceEngine.getMetricRegistrator().toPath("data", "index-hit-total"));
                }
            }
        }
    }

    public void updateDataTypeIndexUsage(IndexMetricsContext indexMetricsContext) {

        Set<String> indexHits = indexMetricsContext.getIndexesHits();

        if( indexHits.isEmpty() ){
            return;
        }

        String typeName = indexMetricsContext.getDataTypeName();

        Map<String, LongAdder> indexesMap = dataTypesIndexesHits.get( typeName );
        Set<String> indexesNames = indexMetricsContext.getIndexesHits();

        _logger.info( ">>> Total index hits for data type [" + typeName + "] " +
                Arrays.toString( indexHits.toArray( new String[ indexHits.size() ] ) ) );

        if( indexesMap != null ) {
            for( String indexName : indexesNames ) {
                LongAdder currentIndexHitCount = indexesMap.get(indexName);
                if (currentIndexHitCount == null) {
                    currentIndexHitCount = new LongAdder();
                    LongAdder prev = indexesMap.putIfAbsent(indexName, currentIndexHitCount);
                    if (prev != null) {
                        currentIndexHitCount = prev;
                    }
                }
                currentIndexHitCount.add(1);
                _logger.info("Type name:" + typeName + ", index name:" + indexName + ", hits:" + currentIndexHitCount.longValue());
            }
        }
    }

}