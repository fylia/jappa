package org.fylia.jappa;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Table;

public class TypeDetails {
	private final boolean entity;
	private final boolean embeddable;
	private final String name;
	private final String simpleName;
	private final String packageName;
	private final String tableName;
	private final String parentType;
	private final TypeMirror type;
	private final Map<String, PropertyDetails> properties = new LinkedHashMap<>();
	public TypeDetails(TypeElement classElement) {
		super();
		entity = classElement.getAnnotation(Entity.class)!=null;
		embeddable = classElement.getAnnotation(Embeddable.class)!=null;
		name=classElement.getQualifiedName().toString();
		simpleName = classElement.getSimpleName().toString();
		parentType = classElement.getSuperclass().toString();
		final PackageElement packageElement = (PackageElement) classElement
                .getEnclosingElement();
		packageName = packageElement.getQualifiedName().toString();
		this.type = classElement.asType();
		Table tableAnnotation = classElement.getAnnotation(Table.class);
		if (tableAnnotation!=null) {
			StringBuilder tableName = new StringBuilder();
			if (tableAnnotation.catalog()!=null && !tableAnnotation.catalog().isEmpty()) {
				tableName.append(tableAnnotation.catalog()).append(".");
			} else if (tableAnnotation.schema()!=null && !tableAnnotation.schema().isEmpty()) {
				tableName.append(tableAnnotation.schema()).append(".");
			} 
			tableName.append(tableAnnotation.name());
			this.tableName = tableName.toString();
		} else {
			this.tableName = simpleName;
		}
	}
	public String getName() {
		return name;
	}
	public String getSimpleName() {
		return simpleName;
	}
	public Map<String, PropertyDetails> getProperties() {
		return properties;
	}
	public TypeMirror getType() {
		return type;
	}
	public String getPackageName() {
		return packageName;
	}
	public PropertyDetails getProperty(String propertyName) {
		return properties.computeIfAbsent(propertyName.toString(), pn->new PropertyDetails().withName(pn));
	}
	public List<PropertyDetails> getAllIdDetails() {
		return properties.values().stream().filter(PropertyDetails::isId).collect(Collectors.toList());
	}
	public PropertyDetails getIdDetails() {
		return properties.values().stream().filter(PropertyDetails::isId).findFirst().orElse(null);
	}
	public String getTableName() {
		return tableName;
	}
	public boolean isEmbeddable() {
		return embeddable;
	}
	public boolean isEntity() {
		return entity;
	}
	public String getParentType() {
		return parentType;
	}
}
