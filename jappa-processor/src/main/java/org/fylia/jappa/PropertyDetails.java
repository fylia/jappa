package org.fylia.jappa;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

public class PropertyDetails {
	public enum PropertyType { SIMPLE, MANY_TO_ONE, MANY_TO_MANY, ONE_TO_MANY };
	private boolean isId;
	private GenerationType generationType;
	private String generator;
	private boolean embedded;
	private boolean nested;
	private String name;
	private TypeMirror type;
	private String columnName;
	private String getterName;
	private String setterName;
	private boolean unique;
	private boolean nullable;
	private boolean insertable;
	private boolean updatable;
	private String columnDefinition="";
	private String table="";
	private int length;
	private int precision;
	private int scale;
	private PropertyType propertyType;
	private String referencedColumnName="";
	private TypeDetails referenceType;
	
	private String typeAsString() {
		return type.toString();
	}
	
	public boolean isId() {
		return isId;
	}
	public void setId(boolean isId) {
		this.isId = isId;
	}
	public GenerationType getGenerationType() {
		return generationType;
	}
	public void setGenerationType(GenerationType generationType) {
		this.generationType = generationType;
	}
	public String getGenerator() {
		return generator;
	}
	public void setGenerator(String generator) {
		this.generator = generator;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public PropertyDetails withName(String name) {
		setName(name);
		return this;
	}
	public TypeMirror getType() {
		return type;
	}
	public void setType(TypeMirror type) {
		this.type = type;
	}
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public String getGetterName() {
		return getterName;
	}
	public void setGetterName(String getterName) {
		this.getterName = getterName;
	}
	public String getSetterName() {
		return setterName;
	}
	public void setSetterName(String setterName) {
		this.setterName = setterName;
	}
	public boolean isNullable() {
		return nullable;
	}
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	public boolean isUnique() {
		return unique;
	}
	public void setUnique(boolean unique) {
		this.unique = unique;
	}
	public boolean isInsertable() {
		return insertable;
	}
	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}
	public boolean isUpdatable() {
		return updatable;
	}
	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}
	public String getColumnDefinition() {
		return columnDefinition;
	}
	public void setColumnDefinition(String columnDefinition) {
		this.columnDefinition = columnDefinition;
	}
	public String getTable() {
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public int getPrecision() {
		return precision;
	}
	public void setPrecision(int precision) {
		this.precision = precision;
	}
	public int getScale() {
		return scale;
	}
	public void setScale(int scale) {
		this.scale = scale;
	}
	public PropertyType getPropertyType() {
		return propertyType;
	}
	public void setPropertyType(PropertyType propertyType) {
		this.propertyType = propertyType;
	}
	public String getReferencedColumnName() {
		return referencedColumnName;
	}
	public void setReferencedColumnName(String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
	}
	public boolean isEmbedded() {
		return embedded;
	}
	public boolean isNested() {
		return nested;
	}
	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}
	public void setNested(boolean nested) {
		this.nested = nested;
	}
	public void setReferenceType(TypeDetails referenceType) {
        this.referenceType = referenceType;
    }
	public TypeDetails getReferenceType() {
        return referenceType;
    }
	public void fillFromColumn(Column columnAnnotation) {
		propertyType = PropertyType.SIMPLE;
		columnName = columnAnnotation.name();
		nullable = columnAnnotation.nullable();
		unique = columnAnnotation.unique();
		insertable = columnAnnotation.insertable();
		updatable = columnAnnotation.updatable();
		columnDefinition = columnAnnotation.columnDefinition();
		table = columnAnnotation.table();
		length = columnAnnotation.length();
		precision = columnAnnotation.precision();
		scale = columnAnnotation.scale();
		embedded = false;
	}
	public void fillFromId(Id idAnnotation) {
		propertyType = PropertyType.SIMPLE;
		isId=true;
		nullable = false;
		unique = true;
		insertable = true;
		updatable = false;
		embedded = false;
	}
	public void fillFromEmbeddedId(EmbeddedId idAnnotation) {
		propertyType = PropertyType.SIMPLE;
		isId=true;
		nullable = false;
		unique = true;
		insertable = true;
		updatable = false;
		embedded = true;
	}
	public void fillFromManyToOne(ManyToOne manyToOne, JoinColumn joinColumn) {
		propertyType = PropertyType.MANY_TO_ONE;
		columnName = joinColumn.name();
		columnDefinition = joinColumn.columnDefinition();
		nullable = joinColumn.nullable();
		unique = joinColumn.unique();
		insertable = joinColumn.insertable();
		updatable = joinColumn.updatable();
		table = joinColumn.table();
		referencedColumnName = joinColumn.referencedColumnName();
		embedded = false;
		
	}
	public String getJdbcType() {
		if (type.getKind()==TypeKind.DECLARED) {
			String typeAsString = type.toString();
			if (!typeAsString.equals("java.lang.String") && !typeAsString.equals("java.math.BigDecimal")) {
				return "Object";
			}
			return typeAsString.replaceAll(".*\\.", "");
		} else {
			return type.getKind().name().substring(0, 1)+type.getKind().name().substring(1).toLowerCase();
		}
	}
	public String getParentProperty() {
		if (!isNested()) {
			throw new UnsupportedOperationException("parent property for non nested property unsupported");
		}
		return name.replaceAll("\\..*$", ""); 
	}
	
}
