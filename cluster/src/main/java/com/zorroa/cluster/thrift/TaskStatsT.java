/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.zorroa.cluster.thrift;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2018-03-16")
public class TaskStatsT implements org.apache.thrift.TBase<TaskStatsT, TaskStatsT._Fields>, java.io.Serializable, Cloneable, Comparable<TaskStatsT> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TaskStatsT");

  private static final org.apache.thrift.protocol.TField WARNING_COUNT_FIELD_DESC = new org.apache.thrift.protocol.TField("warningCount", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField ERROR_COUNT_FIELD_DESC = new org.apache.thrift.protocol.TField("errorCount", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField SUCCESS_COUNT_FIELD_DESC = new org.apache.thrift.protocol.TField("successCount", org.apache.thrift.protocol.TType.I32, (short)3);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new TaskStatsTStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new TaskStatsTTupleSchemeFactory();

  public int warningCount; // required
  public int errorCount; // required
  public int successCount; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    WARNING_COUNT((short)1, "warningCount"),
    ERROR_COUNT((short)2, "errorCount"),
    SUCCESS_COUNT((short)3, "successCount");

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
        case 1: // WARNING_COUNT
          return WARNING_COUNT;
        case 2: // ERROR_COUNT
          return ERROR_COUNT;
        case 3: // SUCCESS_COUNT
          return SUCCESS_COUNT;
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
  private static final int __WARNINGCOUNT_ISSET_ID = 0;
  private static final int __ERRORCOUNT_ISSET_ID = 1;
  private static final int __SUCCESSCOUNT_ISSET_ID = 2;
  private byte __isset_bitfield = 0;
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.WARNING_COUNT, new org.apache.thrift.meta_data.FieldMetaData("warningCount", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.ERROR_COUNT, new org.apache.thrift.meta_data.FieldMetaData("errorCount", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.SUCCESS_COUNT, new org.apache.thrift.meta_data.FieldMetaData("successCount", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TaskStatsT.class, metaDataMap);
  }

  public TaskStatsT() {
    this.warningCount = 0;

    this.errorCount = 0;

    this.successCount = 0;

  }

  public TaskStatsT(
    int warningCount,
    int errorCount,
    int successCount)
  {
    this();
    this.warningCount = warningCount;
    setWarningCountIsSet(true);
    this.errorCount = errorCount;
    setErrorCountIsSet(true);
    this.successCount = successCount;
    setSuccessCountIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TaskStatsT(TaskStatsT other) {
    __isset_bitfield = other.__isset_bitfield;
    this.warningCount = other.warningCount;
    this.errorCount = other.errorCount;
    this.successCount = other.successCount;
  }

  public TaskStatsT deepCopy() {
    return new TaskStatsT(this);
  }

  @Override
  public void clear() {
    this.warningCount = 0;

    this.errorCount = 0;

    this.successCount = 0;

  }

  public int getWarningCount() {
    return this.warningCount;
  }

  public TaskStatsT setWarningCount(int warningCount) {
    this.warningCount = warningCount;
    setWarningCountIsSet(true);
    return this;
  }

  public void unsetWarningCount() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __WARNINGCOUNT_ISSET_ID);
  }

  /** Returns true if field warningCount is set (has been assigned a value) and false otherwise */
  public boolean isSetWarningCount() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __WARNINGCOUNT_ISSET_ID);
  }

  public void setWarningCountIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __WARNINGCOUNT_ISSET_ID, value);
  }

  public int getErrorCount() {
    return this.errorCount;
  }

  public TaskStatsT setErrorCount(int errorCount) {
    this.errorCount = errorCount;
    setErrorCountIsSet(true);
    return this;
  }

  public void unsetErrorCount() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __ERRORCOUNT_ISSET_ID);
  }

  /** Returns true if field errorCount is set (has been assigned a value) and false otherwise */
  public boolean isSetErrorCount() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __ERRORCOUNT_ISSET_ID);
  }

  public void setErrorCountIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __ERRORCOUNT_ISSET_ID, value);
  }

  public int getSuccessCount() {
    return this.successCount;
  }

  public TaskStatsT setSuccessCount(int successCount) {
    this.successCount = successCount;
    setSuccessCountIsSet(true);
    return this;
  }

  public void unsetSuccessCount() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __SUCCESSCOUNT_ISSET_ID);
  }

  /** Returns true if field successCount is set (has been assigned a value) and false otherwise */
  public boolean isSetSuccessCount() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __SUCCESSCOUNT_ISSET_ID);
  }

  public void setSuccessCountIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __SUCCESSCOUNT_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, java.lang.Object value) {
    switch (field) {
    case WARNING_COUNT:
      if (value == null) {
        unsetWarningCount();
      } else {
        setWarningCount((java.lang.Integer)value);
      }
      break;

    case ERROR_COUNT:
      if (value == null) {
        unsetErrorCount();
      } else {
        setErrorCount((java.lang.Integer)value);
      }
      break;

    case SUCCESS_COUNT:
      if (value == null) {
        unsetSuccessCount();
      } else {
        setSuccessCount((java.lang.Integer)value);
      }
      break;

    }
  }

  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case WARNING_COUNT:
      return getWarningCount();

    case ERROR_COUNT:
      return getErrorCount();

    case SUCCESS_COUNT:
      return getSuccessCount();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case WARNING_COUNT:
      return isSetWarningCount();
    case ERROR_COUNT:
      return isSetErrorCount();
    case SUCCESS_COUNT:
      return isSetSuccessCount();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof TaskStatsT)
      return this.equals((TaskStatsT)that);
    return false;
  }

  public boolean equals(TaskStatsT that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_warningCount = true;
    boolean that_present_warningCount = true;
    if (this_present_warningCount || that_present_warningCount) {
      if (!(this_present_warningCount && that_present_warningCount))
        return false;
      if (this.warningCount != that.warningCount)
        return false;
    }

    boolean this_present_errorCount = true;
    boolean that_present_errorCount = true;
    if (this_present_errorCount || that_present_errorCount) {
      if (!(this_present_errorCount && that_present_errorCount))
        return false;
      if (this.errorCount != that.errorCount)
        return false;
    }

    boolean this_present_successCount = true;
    boolean that_present_successCount = true;
    if (this_present_successCount || that_present_successCount) {
      if (!(this_present_successCount && that_present_successCount))
        return false;
      if (this.successCount != that.successCount)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + warningCount;

    hashCode = hashCode * 8191 + errorCount;

    hashCode = hashCode * 8191 + successCount;

    return hashCode;
  }

  @Override
  public int compareTo(TaskStatsT other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetWarningCount()).compareTo(other.isSetWarningCount());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetWarningCount()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.warningCount, other.warningCount);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetErrorCount()).compareTo(other.isSetErrorCount());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetErrorCount()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.errorCount, other.errorCount);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetSuccessCount()).compareTo(other.isSetSuccessCount());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSuccessCount()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.successCount, other.successCount);
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
    java.lang.StringBuilder sb = new java.lang.StringBuilder("TaskStatsT(");
    boolean first = true;

    sb.append("warningCount:");
    sb.append(this.warningCount);
    first = false;
    if (!first) sb.append(", ");
    sb.append("errorCount:");
    sb.append(this.errorCount);
    first = false;
    if (!first) sb.append(", ");
    sb.append("successCount:");
    sb.append(this.successCount);
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
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TaskStatsTStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public TaskStatsTStandardScheme getScheme() {
      return new TaskStatsTStandardScheme();
    }
  }

  private static class TaskStatsTStandardScheme extends org.apache.thrift.scheme.StandardScheme<TaskStatsT> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TaskStatsT struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // WARNING_COUNT
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.warningCount = iprot.readI32();
              struct.setWarningCountIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // ERROR_COUNT
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.errorCount = iprot.readI32();
              struct.setErrorCountIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // SUCCESS_COUNT
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.successCount = iprot.readI32();
              struct.setSuccessCountIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, TaskStatsT struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(WARNING_COUNT_FIELD_DESC);
      oprot.writeI32(struct.warningCount);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(ERROR_COUNT_FIELD_DESC);
      oprot.writeI32(struct.errorCount);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(SUCCESS_COUNT_FIELD_DESC);
      oprot.writeI32(struct.successCount);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TaskStatsTTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public TaskStatsTTupleScheme getScheme() {
      return new TaskStatsTTupleScheme();
    }
  }

  private static class TaskStatsTTupleScheme extends org.apache.thrift.scheme.TupleScheme<TaskStatsT> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TaskStatsT struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetWarningCount()) {
        optionals.set(0);
      }
      if (struct.isSetErrorCount()) {
        optionals.set(1);
      }
      if (struct.isSetSuccessCount()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetWarningCount()) {
        oprot.writeI32(struct.warningCount);
      }
      if (struct.isSetErrorCount()) {
        oprot.writeI32(struct.errorCount);
      }
      if (struct.isSetSuccessCount()) {
        oprot.writeI32(struct.successCount);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TaskStatsT struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.warningCount = iprot.readI32();
        struct.setWarningCountIsSet(true);
      }
      if (incoming.get(1)) {
        struct.errorCount = iprot.readI32();
        struct.setErrorCountIsSet(true);
      }
      if (incoming.get(2)) {
        struct.successCount = iprot.readI32();
        struct.setSuccessCountIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

