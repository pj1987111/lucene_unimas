package org.apache.lucene.util.unimas.index;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by zhy on 2018/4/18.
 */
public class FieldBplusTree {
    public enum FieldType {
        INTEGER,
        DOUBLE,
        STRING
    }

    private FieldType fieldType;
    private IndexOutput indexOutput;
    private BplusTree<String, Integer> stringBplusTree;
    private BplusTree<Long, Integer> integerBplusTree;
    private BplusTree<Double, Integer> doubleBplusTree;

    //lucene写时使用
    public FieldBplusTree(FieldType fieldType, IndexOutput indexOutput) throws IOException {
        this.fieldType = fieldType;
        this.indexOutput = indexOutput;
        if (fieldType == FieldType.INTEGER) {
            indexOutput.writeByte((byte) 0);
        } else if (fieldType == FieldType.DOUBLE) {
            indexOutput.writeByte((byte) 2);
        } else {
            indexOutput.writeByte((byte) 1);
        }
    }

    //lucene读时使用
    public FieldBplusTree() {
    }

    public FieldBplusTree initData(IndexInput indexInput, int indexOrder) throws IOException {
        byte startCode = indexInput.readByte();
        long length = indexInput.length();
        long pointer;
        if (startCode == (byte) 0) {
            this.fieldType = FieldType.INTEGER;
            integerBplusTree = new BplusTree<>(indexOrder);
            for (pointer = 0L; pointer < length; ) {
                long val = indexInput.readLong();
                int docid = indexInput.readInt();
                pointer = indexInput.getFilePointer();
                try {
                    integerBplusTree.insertOrUpdate(val, docid);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        } else if (startCode == (byte) 1) {
            this.fieldType = FieldType.STRING;
            stringBplusTree = new BplusTree<>(indexOrder);
            for (pointer = 0L; pointer < length; ) {
                int valLength = indexInput.readInt();
                byte[] val = new byte[valLength];
                indexInput.readBytes(val, 0, valLength);
                int docid = indexInput.readInt();
                pointer = indexInput.getFilePointer();
                stringBplusTree.insertOrUpdate(new String(val), docid);
            }
        } else if (startCode == (byte) 2) {
            this.fieldType = FieldType.DOUBLE;
            doubleBplusTree = new BplusTree<>(indexOrder);
            for (pointer = 0L; pointer < length; ) {
                int valLength = indexInput.readInt();
                byte[] val = new byte[valLength];
                indexInput.readBytes(val, 0, valLength);
                int docid = indexInput.readInt();
                pointer = indexInput.getFilePointer();
                try {
                    doubleBplusTree.insertOrUpdate(Double.parseDouble(new String(val)), docid);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
        return this;
    }

    //todo 异步
    protected void add(Object val, int docid) throws IOException {
        if (fieldType == FieldType.INTEGER) {
            if (val instanceof Number) {
                indexOutput.writeLong(Long.parseLong("" + val));
                indexOutput.writeInt(docid);
            } else
                throw new IOException("val : " + val + " is not INTEGER TYPE but bplus tree's type is integer");
        } else if (fieldType == FieldType.DOUBLE) {
            if (val instanceof Number) {
                indexOutput.writeInt(("" + val).length());
                indexOutput.writeBytes(("" + val).getBytes(), 0, ("" + val).length());
                indexOutput.writeInt(docid);
            } else
                throw new IOException("val : " + val + " is not DOUBLE TYPE but bplus tree's type is double");
        } else {
            indexOutput.writeInt(("" + val).length());
            indexOutput.writeBytes(("" + val).getBytes(), 0, ("" + val).length());
            indexOutput.writeInt(docid);
        }
    }

    public IndexOutput getIndexOutput() {
        return indexOutput;
    }

    public List<Integer> getRange(Object start, Object end) {
        if (fieldType == FieldType.INTEGER) {
            try {
                return integerBplusTree.getRange(Long.parseLong("" + start), Long.parseLong("" + end));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else if (fieldType == FieldType.DOUBLE) {
            try {
                return doubleBplusTree.getRange(Double.parseDouble("" + start), Double.parseDouble("" + end));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else if (fieldType == FieldType.STRING) {
            return stringBplusTree.getRange("" + start, "" + end);
        }
        return null;
    }

    public List<Integer> getRangeStart(Object start) {
        if (fieldType == FieldType.INTEGER) {
            try {
                return integerBplusTree.getRangeStart(Long.parseLong("" + start));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else if (fieldType == FieldType.DOUBLE) {
            try {
                return doubleBplusTree.getRangeStart(Double.parseDouble("" + start));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else if (fieldType == FieldType.STRING) {
            return stringBplusTree.getRangeStart("" + start);
        }
        return null;
    }

    public List<Integer> getRangeEnd(Object end) {
        if (fieldType == FieldType.INTEGER) {
            try {
                return integerBplusTree.getRangeEnd(Long.parseLong("" + end));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else if (fieldType == FieldType.DOUBLE) {
            try {
                return doubleBplusTree.getRangeEnd(Double.parseDouble("" + end));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else if (fieldType == FieldType.STRING) {
            return stringBplusTree.getRangeEnd("" + end);
        }
        return null;
    }

    public Set<Integer> get(Object val) {
        if (fieldType == FieldType.INTEGER) {
            try {
                return integerBplusTree.get(Long.parseLong("" + val));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else if (fieldType == FieldType.DOUBLE) {
            try {
                return doubleBplusTree.get(Double.parseDouble("" + val));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else if (fieldType == FieldType.STRING) {
            return stringBplusTree.get("" + val);
        }
        return null;
    }
}
