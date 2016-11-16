package org.fylia.jappa.core;

import java.util.Collection;

import javax.persistence.GenerationType;

/**
 * Collection Type
 * @author fylia
 */
public class PropertyDetail {
	public enum PropertyType { SIMPLE, MANY_TO_ONE, MANY_TO_MANY, ONE_TO_MANY };
	private final String name;
	private final Class<?> type;
	private final Class<? extends Collection<?>> collectionType;
	private final boolean id;
	private final GenerationType generationType;
	private final String generator;
	
	private final boolean embedded;
	private final boolean nested;
	private final String columnName;
	private final boolean unique;
	private final boolean nullable;
	private final boolean insertable;
	private final boolean updatable;
	private final String columnDefinition;
	private final String table;
	private final int length;
	private final int precision;
	private final int scale;
	private final PropertyType propertyType;
	private final String referencedColumnName;
	
	public PropertyDetail(String name, String columnName, Class<?> type, 
			boolean id, GenerationType generationType, String generator, 
			boolean embedded, boolean nested, 
			boolean unique, boolean nullable,
			boolean insertable, boolean updatable, 
			String columnDefinition, 
			String table, 
			int length, 
			int precision,
			int scale, 
			String referencedColumnName, 
			Class<? extends Collection<?>> collectionType,
			PropertyType propertyType) {
		this.name = name;
		this.columnName = columnName;
		this.type = type;
		this.id = id;
		this.generationType = generationType;
		this.generator = generator;
		this.embedded = embedded;
		this.nested = nested;
		this.unique = unique;
		this.nullable = nullable;
		this.insertable = insertable;
		this.updatable = updatable;
		this.columnDefinition = columnDefinition;
		this.table = table;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
		this.referencedColumnName = referencedColumnName;
		this.collectionType = collectionType;
		this.propertyType = propertyType;
	}
	public boolean isId() {
		return id;
	}
	public GenerationType getGenerationType() {
		return generationType;
	}
	public String getGenerator() {
		return generator;
	}
	public boolean isNullable() {
		return nullable;
	}
	public String getColumnName() {
		return columnName;
	}
	public boolean isUnique() {
		return unique;
	}
	public boolean isInsertable() {
		return insertable;
	}
	public boolean isUpdatable() {
		return updatable;
	}
	public String getColumnDefinition() {
		return columnDefinition;
	}
	public String getTable() {
		return table;
	}
	public int getLength() {
		return length;
	}
	public int getPrecision() {
		return precision;
	}
	public int getScale() {
		return scale;
	}
	public PropertyType getPropertyType() {
		return propertyType;
	}
	public String getReferencedColumnName() {
		return referencedColumnName;
	}
	public String getName() {
		return name;
	}
	public Class<?> getType() {
		return type;
	}
	public Class<? extends Collection<?>> getCollectionType() {
		return collectionType;
	}
	public boolean isEmbedded() {
		return embedded;
	}
	public boolean isNested() {
		return nested;
	}
	public String getParentProperty() {
		if (!isNested()) {
			throw new UnsupportedOperationException("parent property for non nested property unsupported");
		}
		return name.replaceAll("\\..*$", ""); 
	}
}
