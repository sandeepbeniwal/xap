package com.gigaspaces.datasource;

import com.gigaspaces.document.DocumentObjectConverter;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.metadata.SpacePropertyDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptor;

public interface Patcher {
    SpaceDocument patchSpaceDocument(SpaceDocument spaceDocument);
    SpaceTypeDescriptor patchTypeDescriptor(SpaceTypeDescriptor spaceTypeDescriptor) throws ClassNotFoundException;

    default Object patch(Object object) throws ClassNotFoundException {
        if(object instanceof SpaceTypeDescriptor) {
            SpaceTypeDescriptor oldSchema = (SpaceTypeDescriptor) object;
            SpaceTypeDescriptor newSchema = patchTypeDescriptor(oldSchema);
            validateSchema(newSchema, oldSchema);
            return newSchema;
        }
        return patchSpaceDocument(new DocumentObjectConverter().toSpaceDocument(object));
    }

    default void validateSchema(SpaceTypeDescriptor newSchema, SpaceTypeDescriptor oldSchema){
        for(int i = 0; i< newSchema.getNumOfFixedProperties(); i++){
            SpacePropertyDescriptor newDescriptor = newSchema.getFixedProperty(i);
            SpacePropertyDescriptor oldDescriptor = oldSchema.getFixedProperty(newDescriptor.getName());
            if(oldDescriptor != null){
                if(!newDescriptor.getType().equals(oldDescriptor.getType()))
                    throw new RuntimeException("Data type mismatch in property " + newDescriptor.getName() + ". " + newDescriptor.getType() + " != " + oldDescriptor.getType());
            }
        }
    }
}
