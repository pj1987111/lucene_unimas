package org.apache.lucene.util.unimas.index;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import java.io.IOException;
import java.util.List;

import static org.apache.lucene.util.unimas.UnimasConstant.order;

/**
 * Created by zhy on 2018/4/18.
 */
public class FieldBplusTree {
    public enum FieldType {
        INTEGER,
        STRING
    }

    private FieldType fieldType;
    private IndexOutput indexOutput;
    private BplusTree<String, Long> stringBplusTree;
    private BplusTree<Long, Long> integerBplusTree;

    public FieldBplusTree(FieldType fieldType, IndexOutput indexOutput) throws IOException {
        this.fieldType = fieldType;
        this.indexOutput = indexOutput;
        if (fieldType == FieldType.INTEGER) {
            integerBplusTree = new BplusTree<>(order);
            indexOutput.writeByte((byte)0);
        } else {
            stringBplusTree = new BplusTree<>(order);
            indexOutput.writeByte((byte)1);
        }
    }

    public FieldBplusTree() {
    }

    public FieldBplusTree initData(IndexInput indexInput) throws IOException {
        byte startCode = indexInput.readByte();
        long length = indexInput.length();
        long pointer;
        if(startCode == (byte)0) {
            this.fieldType = FieldType.INTEGER;
            integerBplusTree = new BplusTree<>(order);
            for(pointer = 0L; pointer<length;) {
                long val = indexInput.readLong();
                long docid = indexInput.readLong();
                pointer = indexInput.getFilePointer();
                integerBplusTree.insertOrUpdate(val, docid);
            }
        } else if(startCode == (byte)1) {
            this.fieldType = FieldType.STRING;
            stringBplusTree = new BplusTree<>(order);
            for(pointer = 0L; pointer<length;) {
                int valLength = indexInput.readInt();
                byte[] val = new byte[valLength];
                indexInput.readBytes(val, 0, valLength);
                long docid = indexInput.readLong();
                pointer = indexInput.getFilePointer();
                stringBplusTree.insertOrUpdate(new String(val), docid);
            }
        }
        return this;
    }

    //todo 异步
    protected void add(Object val, long docid) throws IOException {
        if (fieldType == FieldType.INTEGER) {
            if (val instanceof Number) {
                integerBplusTree.insertOrUpdate(Long.parseLong("" + val), docid);
                indexOutput.writeLong(Long.parseLong("" + val));
                indexOutput.writeLong(docid);
            } else
                throw new IOException("val : " + val + " is not NUMBER TYPE but bplus tree's type is number");
        } else {
            stringBplusTree.insertOrUpdate("" + val, docid);
            indexOutput.writeInt(("" + val).length());
            indexOutput.writeBytes(("" + val).getBytes(), 0, ("" + val).length());
            indexOutput.writeLong(docid);
        }
    }

    public IndexOutput getIndexOutput() {
        return indexOutput;
    }

    public List<Long> getRange(Object start, Object end) {
        if(start instanceof Number && end instanceof Number && fieldType == FieldType.INTEGER) {
            return integerBplusTree.getRange(Long.parseLong(""+start), Long.parseLong(""+end));
        } else if(fieldType == FieldType.STRING) {
            return stringBplusTree.getRange(""+start, ""+end);
        }
        return null;
    }

    public List<Long> getRangeStart(Object start) {
        if(start instanceof Number && fieldType == FieldType.INTEGER) {
            return integerBplusTree.getRangeStart(Long.parseLong(""+start));
        } else if(fieldType == FieldType.STRING) {
            return stringBplusTree.getRangeStart(""+start);
        }
        return null;
    }

    public List<Long> getRangeEnd(Object end) {
        if(end instanceof Number && fieldType == FieldType.INTEGER) {
            return integerBplusTree.getRangeEnd(Long.parseLong(""+end));
        } else if(fieldType == FieldType.STRING) {
            return stringBplusTree.getRangeEnd(""+end);
        }
        return null;
    }

    public Long get(Object val) {
        if(val instanceof Number && fieldType == FieldType.INTEGER) {
            return integerBplusTree.get(Long.parseLong(""+val));
        } else if(fieldType == FieldType.STRING) {
            return stringBplusTree.get(""+val);
        }
        return null;
    }
}
