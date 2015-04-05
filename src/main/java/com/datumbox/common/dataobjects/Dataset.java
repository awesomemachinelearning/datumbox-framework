/**
 * Copyright (C) 2013-2015 Vasilis Vryniotis <bbriniotis at datumbox.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.datumbox.common.dataobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
/**
 *
 * @author bbriniotis
 */
public final class Dataset implements Serializable, Iterable<Record> {
    
    private final Map<Integer, Record> recordList;
    
    /* Stores columnName=> Class (ie Type) */
    private final Map<Object, ColumnType> columns;
    
    public static final Object constantColumnName = "~constant";
    public static final Object YColumnName = "~Y";
    
    public enum ColumnType {
        ORDINAL, //ordinal - the key is a String, Integer or List<Object> and the value is short
        NUMERICAL, //number field - the key is a String, Integer or List<Object> and the value is double
        DUMMYVAR, //dummy variable - the key is a List<Object> and the value is true false
        CATEGORICAL; //variable with multiple levels
    }
    
    public static ColumnType value2ColumnType(Object o) {
        if(o instanceof Double || 
           o instanceof Integer ||
           o instanceof Long ||
           o instanceof Float) {
            return ColumnType.NUMERICAL;
        }
        else if(o instanceof Boolean) {
            return ColumnType.DUMMYVAR;
        }
        else if(o instanceof Short) {
            return ColumnType.ORDINAL;
        }
        else if(o instanceof Number) {
            return ColumnType.NUMERICAL;
        }
        else { //string
            return ColumnType.CATEGORICAL;
        }
    }
    
    
    
    public Dataset() {
        recordList = new TreeMap<>();
        columns = new HashMap<>();
    }
    
    /**
     * Returns an Map with columns as keys and types are values.
     * 
     * @return 
     */
    public Map<Object, ColumnType> getColumns() {
        return Collections.unmodifiableMap(columns);
    }
    
    /**
     * Returns the number of columns of the internalDataset.
     * 
     * @return 
     */
    public int getColumnSize() {
        return columns.size();
    }
    
    /**
     * Returns the number of Records in the internalDataset.
     * 
     * @return 
     */
    public int size() {
        return recordList.size();
    }
    
    /**
     * Checks if the Dataset is empty.
     * 
     * @return 
     */
    public boolean isEmpty() {
        return recordList.isEmpty();
    }
    
    /**
     * It extracts the values of a particular column from all observations and
     * stores them into an array. It basically extracts the "flatDataCollection".
     * 
     * @param column
     * @return 
     */
    public FlatDataList extractColumnValues(Object column) {
        FlatDataList flatDataList = new FlatDataList();
        
        for(Record r : recordList.values()) {
            flatDataList.add(r.getX().get(column));
        }
        
        return flatDataList;
    }
    
    /**
     * It extracts the values of a Y values from all observations and
     * stores them into an array. It basically extracts the "flatDataCollection".
     * 
     * @return 
     */
    public FlatDataList extractYValues() {
        FlatDataList flatDataList = new FlatDataList();
        
        for(Record r : recordList.values()) {
            flatDataList.add(r.getY());
        }
        
        return flatDataList;
    }
    
    /**
     * For each Response variable Y found in the internalDataset, it extracts the 
     * values of column and stores it in a list. This method is used usually when we 
     * have categories in Y and we want the values of a particular column to be
     * extracted for each category. It basically extracts the "transposeDataList".
     * 
     * @param column
     * @return 
     */
    public TransposeDataList extractColumnValuesByY(Object column) {
        TransposeDataList transposeDataList = new TransposeDataList();
        
        for(Record r : recordList.values()) {    
            if(!transposeDataList.containsKey(r.getY())) {
                transposeDataList.put(r.getY(), new FlatDataList(new ArrayList<>()) );
            }
            
            transposeDataList.get(r.getY()).add(r.getX().get(column));
        }
        
        return transposeDataList;
    }
    
    /**
     * Returns a subset of the Dataset. It is used for k-fold cross validation
     * and sampling and he Records in the new Dataset have DIFFERENT ids from the
     * original.
     * 
     * @param idsCollection
     * @return 
     */
    public Dataset generateNewSubset(FlatDataList idsCollection) {
        Dataset d = new Dataset();
        
        for(Object id : idsCollection) {
            d.add(recordList.get((Integer)id)); 
        }        
        return d;
    }
    
    /**
     * Retrieves from the Dataset a particular Record by its id.
     * 
     * @param id
     * @return 
     */
    public Record get(Integer id) {
        return recordList.get(id);
    }
    
    /**
     * Remove completely a column from the dataset.
     * 
     * @param column 
     * @return  
     */
    public boolean removeColumn(Object column) {        
        if(columns.remove(column)!=null) { //try to remove it from the columns and it if it removed remove it from the list too
            for(Record r : recordList.values()) {
                r.getX().remove(column); //TODO: do we need to store the record again in the map?
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Updates the meta information of the Dataset such as the supported columns.
     * 
     * @param r 
     */
    private void updateMeta(Record r) {
        for(Map.Entry<Object, Object> entry : r.getX().entrySet()) {
            Object column = entry.getKey();
            Object value = entry.getValue();
            
            if(columns.get(column) == null) {
                columns.put(column, value2ColumnType(value));
            }
        }
    }
    
    public void resetMeta() {
        columns.clear();
        for(Record r: this) {
            updateMeta(r);
        }
    }

    /**
     * Merge the d dataset to the current one.
     * 
     * @param d 
     */
    public void merge(Dataset d) {
        //does not modify the ids of the records of the Dataset d
        for(Record r : d) {
            this.add(r);
        }
    } 
    
    /**
     * Adds the record in the dataset. The original record is shallow copied 
     * and its id is updated (this does not affect the id of the original record).
     * The add method returns the id of the new record.
     * 
     * @param original
     * @return 
     */
    public Integer add(Record original) {
        Record newRecord = original.quickCopy();
        
        Integer newId=(Integer) recordList.size();
        newRecord.setId(newId);
        recordList.put(newId, newRecord);
        updateMeta(newRecord);
        
        return newRecord.getId();
    }
    
    /**
     * Updates the record in the dataset. The add method returns the id of the 
     * updated record.
     * 
     * @param original
     * @return 
     */
    public Integer update(Record r) {
        Integer id = r.getId();
        if (id == null) {
            throw new IllegalArgumentException();
        }
        recordList.put(id, r);
        updateMeta(r);
        
        return id;
    }
    
    /**
     * Clears the Dataset and removes the internal variables.
     */
    public void clear() {
        //TODO: delete the bigdata appropriately
        recordList.clear();
        columns.clear();
    }
    
    /**
     * Implementing read-only iterator on Dataset to use it in loops.
     * 
     * @return 
     */
    @Override
    public Iterator<Record> iterator() {
        return new Iterator<Record>() {
            private Iterator<Integer> it = recordList.keySet().iterator();
            
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Record next() {
                return recordList.get(it.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
