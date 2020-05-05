package com.gigaspaces.datasource;

import com.gigaspaces.metadata.SpaceTypeDescriptor;

public class PatcherSpaceDataSource extends SpaceDataSource{
    private final SpaceDataSource spaceDataSource;
    private final Patcher patcher;

    public PatcherSpaceDataSource(SpaceDataSource spaceDataSource, Patcher patcher) {
        this.spaceDataSource = spaceDataSource;
        this.patcher = patcher;
    }

    @Override
    public DataIterator<SpaceTypeDescriptor> initialMetadataLoad() {
        return new PatcherDataIterator<>(spaceDataSource.initialMetadataLoad());
    }

    @Override
    public DataIterator<Object> initialDataLoad() {
        return new PatcherDataIterator<>(spaceDataSource.initialDataLoad());
    }

    @Override
    public DataIterator<Object> getDataIterator(DataSourceQuery query) {
        return new PatcherDataIterator<>(spaceDataSource.getDataIterator(query));
    }

    @Override
    public Object getById(DataSourceIdQuery idQuery) {
        try {
            return patcher.patch(spaceDataSource.getById(idQuery));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DataIterator<Object> getDataIteratorByIds(DataSourceIdsQuery idsQuery) {
        return new PatcherDataIterator<>(spaceDataSource.getDataIteratorByIds(idsQuery));
    }

    @Override
    public boolean supportsInheritance() {
        return spaceDataSource.supportsInheritance();
    }

    public void close() {
        spaceDataSource.close();
    }

    class PatcherDataIterator<T> implements DataIterator<T>{
        DataIterator<T> dataIterator;

        public PatcherDataIterator(DataIterator<T> dataIterator) {
            this.dataIterator = dataIterator;
        }

        @Override
        public void close() {
            dataIterator.close();
        }

        @Override
        public boolean hasNext() {
            return dataIterator.hasNext();
        }

        @Override
        public T next() {
            try {
                return (T) patcher.patch(dataIterator.next());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
