/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.streams.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.streams.core.StreamsDatum;
import org.apache.streams.core.StreamsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 *
 */
public class TypeConverterProcessor implements StreamsProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(TypeConverterProcessor.class);

    private List<String> formats = Lists.newArrayList();

    private ObjectMapper mapper;

    private Class inClass;
    private Class outClass;

    public TypeConverterProcessor(Class inClass, Class outClass, ObjectMapper mapper) {
        this.inClass = inClass;
        this.outClass = outClass;
        this.mapper = mapper;
    }

    public TypeConverterProcessor(Class inClass, Class outClass, List<String> formats) {
        this.inClass = inClass;
        this.outClass = outClass;
        this.formats = formats;
    }

    public TypeConverterProcessor(Class inClass, Class outClass) {
        this.inClass = inClass;
        this.outClass = outClass;
    }

    @Override
    public List<StreamsDatum> process(StreamsDatum entry) {
        List<StreamsDatum> result = Lists.newLinkedList();
        Object inDoc = entry.getDocument();
        ObjectNode node = null;
        if( inClass == String.class ||
            inDoc instanceof String ) {
            try {
                node = this.mapper.readValue((String)entry.getDocument(), ObjectNode.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            node = this.mapper.convertValue(inDoc, ObjectNode.class);
        }

        if(node != null) {
            Object outDoc;
            try {
                if( outClass == String.class )
                    outDoc = this.mapper.writeValueAsString(node);
                else
                    outDoc = this.mapper.convertValue(node, outClass);

                StreamsDatum outDatum = new StreamsDatum(outDoc, entry.getId(), entry.getTimestamp(), entry.getSequenceid());
                outDatum.setMetadata(entry.getMetadata());
                result.add(outDatum);
            } catch (Throwable e) {
                LOGGER.warn(e.getMessage());
                LOGGER.warn(node.toString());
            }
        }

        return result;
    }

    @Override
    public void prepare(Object configurationObject) {
        if( formats.size() > 0 )
            this.mapper = StreamsJacksonMapper.getInstance(formats);
        else
            this.mapper = StreamsJacksonMapper.getInstance();
    }

    @Override
    public void cleanUp() {

    }
};
