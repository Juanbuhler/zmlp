/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.zorroa.cluster.thrift;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2018-03-16")
public class TaskResultT implements org.apache.thrift.TBase<TaskResultT, TaskResultT._Fields>, java.io.Serializable, Cloneable, Comparable<TaskResultT> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TaskResultT");

  private static final org.apache.thrift.protocol.TField RESULT_FIELD_DESC = new org.apache.thrift.protocol.TField("result", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField ERRORS_FIELD_DESC = new org.apache.thrift.protocol.TField("errors", org.apache.thrift.protocol.TType.LIST, (short)2);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new TaskResultTStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new TaskResultTTupleSchemeFactory();

  public java.nio.ByteBuffer result; // required
  public java.util.List<TaskErrorT> errors; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    RESULT((short)1, "result"),
    ERRORS((short)2, "errors");

    private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // RESULT
          return RESULT;
        case 2: // ERRORS
          return ERRORS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(java.lang.String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final java.lang.String _fieldName;

    _Fields(short thriftId, java.lang.String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public java.lang.String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.RESULT, new org.apache.thrift.meta_data.FieldMetaData("result", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.ERRORS, new org.apache.thrift.meta_data.FieldMetaData("errors", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TaskErrorT.class))));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TaskResultT.class, metaDataMap);
  }

  public TaskResultT() {
    this.errors = new java.util.ArrayList<TaskErrorT>();

  }

  public TaskResultT(
    java.nio.ByteBuffer result,
    java.util.List<TaskErrorT> errors)
  {
    this();
    this.result = org.apache.thrift.TBaseHelper.copyBinary(result);
    this.errors = errors;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TaskResultT(TaskResultT other) {
    if (other.isSetResult()) {
      this.result = org.apache.thrift.TBaseHelper.copyBinary(other.result);
    }
    if (other.isSetErrors()) {
      java.util.List<TaskErrorT> __this__errors = new java.util.ArrayList<TaskErrorT>(other.errors.size());
      for (TaskErrorT other_element : other.errors) {
        __this__errors.add(new TaskErrorT(other_element));
      }
      this.errors = __this__errors;
    }
  }

  public TaskResultT deepCopy() {
    return new TaskResultT(this);
  }

  @Override
  public void clear() {
    this.result = null;
    this.errors = new java.util.ArrayList<TaskErrorT>();

  }

  public byte[] getResult() {
    setResult(org.apache.thrift.TBaseHelper.rightSize(result));
    return result == null ? null : result.array();
  }

  public java.nio.ByteBuffer bufferForResult() {
    return org.apache.thrift.TBaseHelper.copyBinary(result);
  }

  public TaskResultT setResult(byte[] result) {
    this.result = result == null ? (java.nio.ByteBuffer)null : java.nio.ByteBuffer.wrap(result.clone());
    return this;
  }

  public TaskResultT setResult(java.nio.ByteBuffer result) {
    this.result = org.apache.thrift.TBaseHelper.copyBinary(result);
    return this;
  }

  public void unsetResult() {
    this.result = null;
  }

  /** Returns true if field result is set (has been assigned a value) and false otherwise */
  public boolean isSetResult() {
    return this.result != null;
  }

  public void setResultIsSet(boolean value) {
    if (!value) {
      this.result = null;
    }
  }

  public int getErrorsSize() {
    return (this.errors == null) ? 0 : this.errors.size();
  }

  public java.util.Iterator<TaskErrorT> getErrorsIterator() {
    return (this.errors == null) ? null : this.errors.iterator();
  }

  public void addToErrors(TaskErrorT elem) {
    if (this.errors == null) {
      this.errors = new java.util.ArrayList<TaskErrorT>();
    }
    this.errors.add(elem);
  }

  public java.util.List<TaskErrorT> getErrors() {
    return this.errors;
  }

  public TaskResultT setErrors(java.util.List<TaskErrorT> errors) {
    this.errors = errors;
    return this;
  }

  public void unsetErrors() {
    this.errors = null;
  }

  /** Returns true if field errors is set (has been assigned a value) and false otherwise */
  public boolean isSetErrors() {
    return this.errors != null;
  }

  public void setErrorsIsSet(boolean value) {
    if (!value) {
      this.errors = null;
    }
  }

  public void setFieldValue(_Fields field, java.lang.Object value) {
    switch (field) {
    case RESULT:
      if (value == null) {
        unsetResult();
      } else {
        if (value instanceof byte[]) {
          setResult((byte[])value);
        } else {
          setResult((java.nio.ByteBuffer)value);
        }
      }
      break;

    case ERRORS:
      if (value == null) {
        unsetErrors();
      } else {
        setErrors((java.util.List<TaskErrorT>)value);
      }
      break;

    }
  }

  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case RESULT:
      return getResult();

    case ERRORS:
      return getErrors();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case RESULT:
      return isSetResult();
    case ERRORS:
      return isSetErrors();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof TaskResultT)
      return this.equals((TaskResultT)that);
    return false;
  }

  public boolean equals(TaskResultT that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_result = true && this.isSetResult();
    boolean that_present_result = true && that.isSetResult();
    if (this_present_result || that_present_result) {
      if (!(this_present_result && that_present_result))
        return false;
      if (!this.result.equals(that.result))
        return false;
    }

    boolean this_present_errors = true && this.isSetErrors();
    boolean that_present_errors = true && that.isSetErrors();
    if (this_present_errors || that_present_errors) {
      if (!(this_present_errors && that_present_errors))
        return false;
      if (!this.errors.equals(that.errors))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetResult()) ? 131071 : 524287);
    if (isSetResult())
      hashCode = hashCode * 8191 + result.hashCode();

    hashCode = hashCode * 8191 + ((isSetErrors()) ? 131071 : 524287);
    if (isSetErrors())
      hashCode = hashCode * 8191 + errors.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(TaskResultT other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetResult()).compareTo(other.isSetResult());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetResult()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.result, other.result);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetErrors()).compareTo(other.isSetErrors());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetErrors()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.errors, other.errors);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    scheme(iprot).read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public java.lang.String toString() {
    java.lang.StringBuilder sb = new java.lang.StringBuilder("TaskResultT(");
    boolean first = true;

    sb.append("result:");
    if (this.result == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.result, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("errors:");
    if (this.errors == null) {
      sb.append("null");
    } else {
      sb.append(this.errors);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TaskResultTStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public TaskResultTStandardScheme getScheme() {
      return new TaskResultTStandardScheme();
    }
  }

  private static class TaskResultTStandardScheme extends org.apache.thrift.scheme.StandardScheme<TaskResultT> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TaskResultT struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // RESULT
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.result = iprot.readBinary();
              struct.setResultIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // ERRORS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list18 = iprot.readListBegin();
                struct.errors = new java.util.ArrayList<TaskErrorT>(_list18.size);
                TaskErrorT _elem19;
                for (int _i20 = 0; _i20 < _list18.size; ++_i20)
                {
                  _elem19 = new TaskErrorT();
                  _elem19.read(iprot);
                  struct.errors.add(_elem19);
                }
                iprot.readListEnd();
              }
              struct.setErrorsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TaskResultT struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.result != null) {
        oprot.writeFieldBegin(RESULT_FIELD_DESC);
        oprot.writeBinary(struct.result);
        oprot.writeFieldEnd();
      }
      if (struct.errors != null) {
        oprot.writeFieldBegin(ERRORS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.errors.size()));
          for (TaskErrorT _iter21 : struct.errors)
          {
            _iter21.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TaskResultTTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public TaskResultTTupleScheme getScheme() {
      return new TaskResultTTupleScheme();
    }
  }

  private static class TaskResultTTupleScheme extends org.apache.thrift.scheme.TupleScheme<TaskResultT> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TaskResultT struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetResult()) {
        optionals.set(0);
      }
      if (struct.isSetErrors()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetResult()) {
        oprot.writeBinary(struct.result);
      }
      if (struct.isSetErrors()) {
        {
          oprot.writeI32(struct.errors.size());
          for (TaskErrorT _iter22 : struct.errors)
          {
            _iter22.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TaskResultT struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.result = iprot.readBinary();
        struct.setResultIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list23 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.errors = new java.util.ArrayList<TaskErrorT>(_list23.size);
          TaskErrorT _elem24;
          for (int _i25 = 0; _i25 < _list23.size; ++_i25)
          {
            _elem24 = new TaskErrorT();
            _elem24.read(iprot);
            struct.errors.add(_elem24);
          }
        }
        struct.setErrorsIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

