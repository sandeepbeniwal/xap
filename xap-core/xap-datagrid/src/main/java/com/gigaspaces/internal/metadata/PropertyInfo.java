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

package com.gigaspaces.internal.metadata;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;
import com.gigaspaces.client.storage_adapters.internal.PropertyStorageAdapterRegistry;
import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.internal.utils.ClassUtils;
import com.gigaspaces.internal.utils.ReflectionUtils;
import com.gigaspaces.internal.version.PlatformLogicalVersion;
import com.gigaspaces.metadata.SpaceDocumentSupport;
import com.gigaspaces.metadata.SpacePropertyDescriptor;
import com.gigaspaces.metadata.StorageType;
import com.j_spaces.kernel.ClassLoaderHelper;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry property.
 *
 * @author Niv Ingberg
 * @since 7.0 NOTE: Starting 8.0 this class is not serialized
 */
@com.gigaspaces.api.InternalApi
public class PropertyInfo implements SpacePropertyDescriptor{

    private final String _name;
    private final String _typeName;
    private final boolean _primitive;
    private final boolean _spacePrimitive;
    private final Class<?> _type;
    private final SpaceDocumentSupport _documentSupport;
    private StorageType _storageType;
    private PropertyStorageAdapter _storageAdapter;
    private final byte _dotnetStorageType;

    private PropertyInfo(Builder builder) {
        this._name = builder.name;
        this._typeName = builder.typeName;
        this._type = (builder.type == null) ? getTypeFromName(_typeName) : builder.type;
        this._primitive = ReflectionUtils.isPrimitive(_typeName);
        this._spacePrimitive = ReflectionUtils.isSpacePrimitive(_typeName);
        this._documentSupport = builder.documentSupport != SpaceDocumentSupport.DEFAULT
                ? builder.documentSupport
                : SpaceDocumentSupportHelper.getDefaultDocumentSupport(_type);
        initStorageAdapter(builder.storageAdapterClass);
        initStorageType(builder.storageType);
        this._dotnetStorageType = builder.dotnetStorageType;
    }

    private void initStorageType(StorageType storageType) {
        this._storageType = storageType;
        if (storageType.getStorageAdapterClass() != null && !_spacePrimitive) {
            if (_storageAdapter != null && !_storageAdapter.getClass().equals(storageType.getStorageAdapterClass()))
                throw new IllegalStateException("Ambiguous storage settings: storageAdapterClass=" + _storageAdapter.getClass() + ", storageType=" + storageType);
            initStorageAdapter(storageType.getStorageAdapterClass());
        }
    }

    private void initStorageAdapter(Class<? extends PropertyStorageAdapter> storageAdapterClass) {
        this._storageAdapter = storageAdapterClass == null ? null : PropertyStorageAdapterRegistry.getInstance().getOrCreate(storageAdapterClass);
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getTypeName() {
        return _typeName;
    }

    @Override
    public String getTypeDisplayName() {
        return ClassUtils.getTypeDisplayName(_typeName);
    }

    @Override
    public Class<?> getType() {
        return _type;
    }

    @Override
    public SpaceDocumentSupport getDocumentSupport() {
        return _documentSupport;
    }

    @Override
    public StorageType getStorageType() {
        return _storageType;
    }

    public void setDefaultStorageType(StorageType defaultStorageType) {
        initStorageType(_spacePrimitive ? StorageType.OBJECT : defaultStorageType);
    }

    @Override
    public String getStorageAdapterName() {
        return _storageAdapter == null ? "" : _storageAdapter.getName();
    }

    public PropertyStorageAdapter getStorageAdapter() {
        return _storageAdapter;
    }

    public boolean supportsEqualsMatching() {
        return _storageAdapter == null || _storageAdapter.supportsEqualsMatching();
    }

    public boolean supportsOrderedMatching() {
        return _storageAdapter == null || _storageAdapter.supportsOrderedMatching();
    }

    public byte getDotnetStorageType() {
        return _dotnetStorageType;
    }

    public boolean isPrimitive() {
        return _primitive;
    }

    public boolean isSpacePrimitive() {
        return _spacePrimitive;
    }

    @Override
    public String toString() {
        return "Property[name=" + _name + ", type=" + _typeName + "]";
    }

    Object beforeSerialize(Object value) throws IOException {
        return value == null || _storageAdapter == null ? value : _storageAdapter.toSpace(value);
    }

    Object afterDeserialize(Object value) throws IOException, ClassNotFoundException {
        return value == null || _storageAdapter == null ? value : _storageAdapter.fromSpace(value);
    }

    private static Class<?> getTypeFromName(String typeName) {
        if (typeName == null || typeName.length() == 0)
            return Object.class;

        try {
            return ClassLoaderHelper.loadClass(typeName);
        } catch (ClassNotFoundException e) {
            return Object.class;
        }
    }

    public boolean isCommonJavaType() {
        return ReflectionUtils.isCommonJavaType(_typeName);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    void serialize(ObjectOutput out, PlatformLogicalVersion version) throws IOException {
        IOUtils.writeString(out, _name);
        IOUtils.writeString(out, _typeName);
        IOUtils.writeObject(out, _type);
        // Removed in 8.0.4: primitive is calculated from typename.
        //out.writeBoolean(property.isPrimitive());
        // New in 8.0.1: write SpaceDocumentSupport code.
        out.writeByte(SpaceDocumentSupportHelper.toCode(_documentSupport));
        // New in 9.0.0: write storage type as code.
        out.writeInt(_storageType.getCode());
        // Changed in 8.0.4: write dotnet storage type as code instead of object
        out.writeByte(_dotnetStorageType);
        // New in 15.2.0: property storage adapter
        if (version.greaterOrEquals(PlatformLogicalVersion.v15_2_0)) {
            IOUtils.writeString(out, _storageAdapter != null ? _storageAdapter.getClass().getName() : null);
        }
    }

    static PropertyInfo deserialize(ObjectInput in, PlatformLogicalVersion version) throws IOException, ClassNotFoundException {
        Builder builder = new Builder(IOUtils.readString(in));
        builder.typeName = IOUtils.readString(in);
        builder.type = IOUtils.readObject(in);
        // Removed in 8.0.4: primitive is calculated from typename.
        //boolean isPrimitive = in.readBoolean();
        // New in 8.0.1: read SpaceDocumentSupport code
        builder.documentSupport = SpaceDocumentSupportHelper.fromCode(in.readByte());
        // New in 9.0.0: read storage type code
        builder.storageType = StorageType.fromCode(in.readInt());
        // Changed in 8.0.4: read dotnet storage type as code instead of object.
        builder.dotnetStorageType = in.readByte();
        // New in 15.2.0: property storage adapter
        if (version.greaterOrEquals(PlatformLogicalVersion.v15_2_0)) {
            String storageAdapterClassName = IOUtils.readString(in);
            if (storageAdapterClassName != null)
                builder.storageAdapter(ClassLoaderHelper.loadClass(storageAdapterClassName));
        }
        return builder.build();
    }

    public static class Builder {
        private final String name;
        private Class<?> type;
        private String typeName;
        private SpaceDocumentSupport documentSupport = SpaceDocumentSupport.DEFAULT;
        private StorageType storageType = StorageType.DEFAULT;
        private byte dotnetStorageType = DotNetStorageType.NULL;
        private Class<? extends PropertyStorageAdapter> storageAdapterClass;

        public Builder(String name) {
            this.name = name;
        }

        public PropertyInfo build() {
            return new PropertyInfo(this);
        }

        public Builder type(Class<?> type) {
            this.type = type;
            this.typeName = type.getName();
            return this;
        }

        public Builder type(String typeName) {
            this.type = null;
            this.typeName = typeName;
            return this;
        }

        public Builder documentSupport(SpaceDocumentSupport documentSupport) {
            this.documentSupport = documentSupport;
            return this;
        }

        public Builder storageType(StorageType storageType) {
            this.storageType = storageType;
            return this;
        }

        public Builder storageAdapter(Class<? extends PropertyStorageAdapter> storageAdapterClass) {
            this.storageAdapterClass = storageAdapterClass;
            return this;
        }

        public Builder dotNetStorageType(byte dotnetStorageType) {
            this.dotnetStorageType = dotnetStorageType;
            return this;
        }
    }
}
