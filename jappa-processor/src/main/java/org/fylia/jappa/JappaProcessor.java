package org.fylia.jappa;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import org.fylia.jappa.PropertyDetails.PropertyType;

/**
 * Annotiation Processor
 * @author fylia
 *
 */
@SupportedAnnotationTypes(
        {"javax.persistence.Entity", "javax.persistence.MappedSuperclass"}
        )
@SupportedOptions("destinationPackage")
public class JappaProcessor extends AbstractProcessor {
	private Messager messager;
	private Map<String,String> options;
	private Types typeUtil;
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
		options = processingEnv.getOptions();
		typeUtil = processingEnv.getTypeUtils();
	}

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
    	try {
    		Map<String, TypeDetails> entities = new HashMap<>();
			messager.printMessage(Kind.WARNING, "Option : "+options);
	        for (TypeElement te : annotations) {
	            for (Element e : roundEnv.getElementsAnnotatedWith(te)) {
	                if (e.getKind() == ElementKind.CLASS) {
	                    final TypeElement classElement = (TypeElement) e;
	                    TypeDetails td = new TypeDetails(classElement);
	                    entities.put(td.getName(), td);
	                    getPropertyDetails(td, classElement);
	                }
	            }
	        }
	        for (TypeDetails td : entities.values()) {
				messager.printMessage(Kind.WARNING, "Generating template for "+td.getName());
	        	if (td.isEntity()) {
	        		if (td.getParentType()!=null) {
	        			TypeDetails parent = entities.get(td.getParentType());
	        			if (parent!=null && parent.getProperties()!=null) {
	        				parent.getProperties().forEach(td.getProperties()::putIfAbsent);
	        			}
	        		}
	        		generatePropertyConstantsInterface(td, entities);
	        	}
	        }
    	} catch (RuntimeException e) {
    		for (StackTraceElement ste: e.getStackTrace()) {
    			messager.printMessage(Kind.ERROR, ste.getClassName()+":"+ste.getMethodName()+"("+ste.getLineNumber()+")");
    		} 
    		throw e;
    	}
        return false;
    }
    
    private void getPropertyDetails(TypeDetails td, TypeElement classElement) {
        for (Element enclosedEl : classElement
                .getEnclosedElements()) {
            final Name propertyName = enclosedEl.getSimpleName();
            Id idAnnotation = enclosedEl.getAnnotation(Id.class);
            EmbeddedId embeddedIdAnnotation = enclosedEl.getAnnotation(EmbeddedId.class);
            Column columnAnnotation = enclosedEl.getAnnotation(Column.class);
            ManyToOne manyToOneAnnotation = enclosedEl.getAnnotation(ManyToOne.class);
            JoinColumn joinColumnAnnotation = enclosedEl.getAnnotation(JoinColumn.class);
            GeneratedValue generatedValueAnnotation = enclosedEl.getAnnotation(GeneratedValue.class);
			if (enclosedEl.getKind() == ElementKind.FIELD
                    && !enclosedEl.getModifiers().contains(
                            Modifier.STATIC)
                    && columnAnnotation!=null)  {
            	PropertyDetails details = td.getProperty(propertyName.toString());
            	details.fillFromColumn(columnAnnotation);
            	if (details.getColumnName().isEmpty()) {
            		details.setColumnName(details.getName());
            	}
            	details.setType(enclosedEl.asType());
            	details.setId(idAnnotation!=null);
            	if (generatedValueAnnotation!=null) {
            		details.setGenerationType(generatedValueAnnotation.strategy());
            		details.setGenerator(generatedValueAnnotation.generator());
            	}
				messager.printMessage(Kind.WARNING, "Property found for type "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
			} else if (enclosedEl.getKind() == ElementKind.FIELD
						&& !enclosedEl.getModifiers().contains(
								Modifier.STATIC)
						&& idAnnotation!=null)  {
					PropertyDetails details = td.getProperty(propertyName.toString());
					details.fillFromId(idAnnotation);
					details.setColumnName(details.getName());
					details.setType(enclosedEl.asType());
	            	if (generatedValueAnnotation!=null) {
	            		details.setGenerationType(generatedValueAnnotation.strategy());
	            		details.setGenerator(generatedValueAnnotation.generator());
	            	}
					messager.printMessage(Kind.WARNING, "Property found for type "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
			} else if (enclosedEl.getKind() == ElementKind.FIELD
					&& !enclosedEl.getModifiers().contains(
							Modifier.STATIC)
					&& embeddedIdAnnotation!=null)  {
				PropertyDetails details = td.getProperty(propertyName.toString());
				details.fillFromEmbeddedId(embeddedIdAnnotation);
				details.setColumnName(details.getName());
				details.setType(enclosedEl.asType());
				getEmbeddedPropertyDetails(td, details, enclosedEl);
				messager.printMessage(Kind.WARNING, "Property found for type "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
			} else if (enclosedEl.getKind() == ElementKind.FIELD
                        && !enclosedEl.getModifiers().contains(Modifier.STATIC)
                        && joinColumnAnnotation!=null && manyToOneAnnotation!=null)  {
            	PropertyDetails details = td.getProperty(propertyName.toString());
            	details.fillFromManyToOne(manyToOneAnnotation, joinColumnAnnotation);
            	if (details.getColumnName().isEmpty()) {
            		details.setColumnName(details.getName());
            	}
            	details.setType(enclosedEl.asType());
				messager.printMessage(Kind.WARNING, "Property found "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
                    //writeNewProperty(bw, propertyName);
            } else if (enclosedEl.getKind() == ElementKind.METHOD) {
                final String methodName = propertyName.toString();
                if ((methodName.startsWith("get")
                		|| methodName.startsWith("is")
                        || methodName.startsWith("set"))
                        && enclosedEl.getModifiers().contains(Modifier.PUBLIC)
                        && !enclosedEl.getModifiers().contains(Modifier.STATIC)) {
                    ExecutableType emeth = (ExecutableType)enclosedEl.asType();
                	if (methodName.startsWith("set")) {
                		if(!emeth.getReturnType().getKind().equals(TypeKind.VOID)) {
                        	continue;
                        }
                        if(emeth.getParameterTypes().size() != 1) {
                          continue;
                        }                     		
                        final String propName = (methodName.substring(3, 4).toLowerCase()+methodName.substring(4));
                    	PropertyDetails details = td.getProperty(propName.toString());
                    	details.setSetterName(methodName);
        				messager.printMessage(Kind.WARNING, "Setter found for property "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
                	} else if (methodName.startsWith("get")) {
                        if(emeth.getParameterTypes().size() != 0) {
                            continue;
                        }                     		
                        final String propName = (methodName.substring(3, 4).toLowerCase()+methodName.substring(4));
                    	PropertyDetails details = td.getProperty(propName.toString());
                    	details.setGetterName(methodName);
        				messager.printMessage(Kind.WARNING, "Getter found for property "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
                	} else if (methodName.startsWith("is")) {
                        if(emeth.getParameterTypes().size() != 0) {
                            continue;
                        }                     		
                		if(!emeth.getReturnType().getKind().equals(TypeKind.BOOLEAN)) {
                        	continue;
                        }
                		final String propName = (methodName.substring(2, 3).toLowerCase()+methodName.substring(3));
                    	PropertyDetails details = td.getProperty(propName.toString());
                		details.setGetterName(methodName);
        				messager.printMessage(Kind.WARNING, "Getter found for property "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
                	}
                    //writeNewProperty(bw, propName);
                }
            }
        }
    }

    private void getEmbeddedPropertyDetails(TypeDetails td, PropertyDetails idDetails, Element parentEl) {
		if (idDetails.getType().getKind() != TypeKind.DECLARED) {
			throw new IllegalArgumentException("Property "+idDetails+" is not embeddable : "+idDetails.getType().toString());
		}
		Element classElement = typeUtil.asElement(idDetails.getType());

		if (classElement.getAnnotation(Embeddable.class)==null) {
			throw new IllegalArgumentException("Property "+idDetails+" is not embeddable : "+idDetails.getType().toString());
		}
		
        for (Element enclosedEl : classElement.getEnclosedElements()) {
            final Name propertyName = enclosedEl.getSimpleName();
            Column columnAnnotation = enclosedEl.getAnnotation(Column.class);
			if (enclosedEl.getKind() == ElementKind.FIELD
                    && !enclosedEl.getModifiers().contains(
                            Modifier.STATIC)
                    && columnAnnotation!=null)  {
				final String embeddedPropertyName = idDetails.getName()+"."+propertyName.toString();
            	PropertyDetails details = td.getProperty(embeddedPropertyName);
            	details.fillFromColumn(columnAnnotation);
            	AttributeOverride override = getAttributeOverride(propertyName.toString(), parentEl);
            	if (override!=null) {
            		details.fillFromColumn(override.column());
            	}
            	if (details.getColumnName().isEmpty()) {
            		details.setColumnName(propertyName.toString());
            	}
            	details.setType(enclosedEl.asType());
            	details.setNested(true);
            	details.setId(true);
            	details.setEmbedded(true);
				messager.printMessage(Kind.WARNING, "Property found for type "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
            } else if (enclosedEl.getKind() == ElementKind.METHOD) {
                final String methodName = propertyName.toString();
                if ((methodName.startsWith("get")
                		|| methodName.startsWith("is")
                        || methodName.startsWith("set"))
                        && enclosedEl.getModifiers().contains(Modifier.PUBLIC)
                        && !enclosedEl.getModifiers().contains(Modifier.STATIC)) {
                    ExecutableType emeth = (ExecutableType)enclosedEl.asType();
                	if (methodName.startsWith("set")) {
                		if(!emeth.getReturnType().getKind().equals(TypeKind.VOID)) {
                        	continue;
                        }
                        if(emeth.getParameterTypes().size() != 1) {
                          continue;
                        }                     		
                        final String embeddedPropertyName = idDetails.getName()+"."+(methodName.substring(3, 4).toLowerCase()+methodName.substring(4));
                    	PropertyDetails details = td.getProperty(embeddedPropertyName);
                    	details.setSetterName(methodName);
        				messager.printMessage(Kind.WARNING, "Setter found for property "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
                	} else if (methodName.startsWith("get")) {
                        if(emeth.getParameterTypes().size() != 0) {
                            continue;
                        }                     		
                        final String embeddedPropertyName = idDetails.getName()+"."+(methodName.substring(3, 4).toLowerCase()+methodName.substring(4));
                    	PropertyDetails details = td.getProperty(embeddedPropertyName);
                    	details.setGetterName(methodName);
        				messager.printMessage(Kind.WARNING, "Getter found for property "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
                	} else if (methodName.startsWith("is")) {
                        if(emeth.getParameterTypes().size() != 0) {
                            continue;
                        }                     		
                		if(!emeth.getReturnType().getKind().equals(TypeKind.BOOLEAN)) {
                        	continue;
                        }
                        final String embeddedPropertyName = idDetails.getName()+"."+(methodName.substring(2, 3).toLowerCase()+methodName.substring(3));
                    	PropertyDetails details = td.getProperty(embeddedPropertyName);
                		details.setGetterName(methodName);
        				messager.printMessage(Kind.WARNING, "Getter found for property "+td.getName()+"."+details.getName()+" (col:"+details.getColumnName()+" id?:"+details.isId()+")");
                	}
                    //writeNewProperty(bw, propName);
                }
            }
        }
    }
    
    private AttributeOverride getAttributeOverride(String property, Element parentEl) {
    	AttributeOverride ao = parentEl.getAnnotation(AttributeOverride.class);
    	if (ao!=null && ao.name().equals(property)) {
    		return ao;
    	}
    	AttributeOverrides aos = parentEl.getAnnotation(AttributeOverrides.class);
    	if (aos!=null) {
    		for (AttributeOverride ao2:aos.value()) {
    			if (ao2.name().equals(property)) {
    				return ao;
    			}
    		}
    	}
    	return null;
    }
    /**
     * Generate the propertyConstantsInterface for a given classElement
     * @param classElement the class element
     */
    private void generatePropertyConstantsInterface(
			final TypeDetails td,
			Map<String, TypeDetails> entities) {
        JavaFileObject jfo;
        try {
            jfo = processingEnv.getFiler().createSourceFile(
                    options.get("destinationPackage")
                    + "."+ td.getSimpleName()
                    + "JdbcTemplate");
            final BufferedWriter bw = new BufferedWriter(jfo.openWriter());
            bw.append("package ");
            bw.append(options.get("destinationPackage"));
            bw.append(";");
            bw.newLine();
            bw.append("// Generated by ");
            bw.append(getClass().getName());
            bw.newLine();
            bw.newLine();
            bw.append("import java.sql.ResultSet;");
            bw.newLine();
            bw.append("import java.sql.SQLException;");
            bw.newLine();
            bw.append("import java.util.Arrays;");
            bw.newLine();
            bw.append("import java.util.ArrayList;");
            bw.newLine();
            bw.append("import java.util.List;");
            bw.newLine();
            bw.append("import java.util.stream.Collectors;");
            bw.newLine();
            bw.append("import java.util.stream.IntStream;");
            bw.newLine();
            bw.append("import ").append(td.getName()).append(";");
            bw.newLine();
            bw.append("import org.fylia.jappa.core.AbstractSpringJdbcDao;");
            bw.newLine();
            bw.append("import org.fylia.jappa.core.PropertyDetail;");
            bw.newLine();
            
            bw.append("import org.springframework.dao.EmptyResultDataAccessException;");
            bw.newLine();
            bw.append("import org.springframework.jdbc.core.RowMapper;");
            bw.newLine();
            bw.append("import org.springframework.jdbc.support.GeneratedKeyHolder;");
            bw.newLine();
            bw.newLine();
            
            bw.append("/**");
            bw.newLine();
            bw.append(" * Generated Class with utilities for use with JdbcTemplate and ").append(td.getName());
            bw.newLine();
            // CHECKSTYLE:OFF:StringLiteralCheck FOR 1 LINE
            bw.append(" */");
            bw.newLine();
            bw.append("public class ");
            bw.append(td.getSimpleName());
            bw.append("JdbcTemplate extends AbstractSpringJdbcDao { ");
            bw.newLine();
            writeColumnConstants(bw, td);
        	bw.newLine();
            writeMapper(bw, td, entities);
            bw.newLine();
            writeInsert(bw, td, entities);
            bw.newLine();
            writeUpdate(bw, td, entities);
        	bw.newLine();
            writeMerge(bw, td, entities);
        	bw.newLine();
            writeSelectById(bw, td);
        	bw.newLine();
            writeSelectAll(bw, td);
            bw.newLine();
            writeHasId(bw, td, entities);
            bw.newLine();
            writeGetValue(bw, td, entities);
        	bw.newLine();
        	writeSetValue(bw, td, entities);
        	bw.newLine();
        	writeIdMapperDefinition(bw, td, entities);
        	bw.newLine();
            writeMapperDefinition(bw, td, entities);
            bw.newLine();
            bw.append("}");
            bw.newLine();
            bw.flush();
            bw.close();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }
    
    private void writeColumnConstants(BufferedWriter bw, TypeDetails td) throws IOException {
    	bw.append("    public static final String TABLENAME = \"").append(td.getTableName()).append(" \";");
    	bw.newLine();
    	bw.append("    public static final List<PropertyDetail> PROPERTIES_LIST = Arrays.asList(");
    	boolean [] first = {true};
    	td.getProperties().values().stream().forEachOrdered(pd->
    		{
				try {
					if (!first[0]) bw.append(","); else first[0]=false;
					bw.newLine();
					bw.append("            new PropertyDetail(\"")
						.append(pd.getName()).append("\",\"").append(pd.getColumnName()).append("\", ")
						.append(String.valueOf(pd.getType())).append(".class, ")
						.append(String.valueOf(pd.isId())).append(", ")
						.append(pd.getGenerationType()==null?"null":("javax.persistence.GenerationType."+pd.getGenerationType())).append(", ")
						.append(pd.getGenerator()==null?"null, ":("\""+pd.getGenerator()+"\", "))
						.append(String.valueOf(pd.isEmbedded())).append(", ")
						.append(String.valueOf(pd.isNested())).append(", ")
						.append(String.valueOf(pd.isUnique())).append(", ")
						.append(String.valueOf(pd.isNullable())).append(", ")
						.append(String.valueOf(pd.isInsertable())).append(", ")
						.append(String.valueOf(pd.isUpdatable())).append(", ")
						.append("\""+pd.getColumnDefinition()+"\", ")
						.append("\""+pd.getTable()+"\", ")
						.append(String.valueOf(pd.getLength())).append(", ")
						.append(String.valueOf(pd.getPrecision())).append(", ")
						.append(String.valueOf(pd.getScale())).append(", ")
						.append("\""+pd.getReferencedColumnName()+"\", ")
						.append("null, ")
						.append(pd.getPropertyType()==null?"null":"PropertyDetail.PropertyType.").append(pd.getPropertyType()==null?"":pd.getPropertyType().name()).append(")");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		);
    	bw.append(");");
    	bw.newLine();
    	bw.newLine();
    	bw.append("    public static final List<PropertyDetail> DETAIL_PROPERTIES_LIST = PROPERTIES_LIST.stream().filter(p->!p.isId()).collect(Collectors.toList());"); 
    	bw.newLine();
    	bw.append("    public static final List<PropertyDetail> ID_PROPERTIES_LIST = PROPERTIES_LIST.stream().filter(p->p.isId() && (!p.isEmbedded() || p.isNested())).collect(Collectors.toList());"); 
    	bw.newLine();
    	
    	bw.append("    public static final List<String> DETAIL_COLUMN_LIST = DETAIL_PROPERTIES_LIST.stream().map(PropertyDetail::getColumnName).collect(Collectors.toList());");
    	bw.newLine();
    	bw.append("    public static final List<String> ID_COLUMN_LIST = ID_PROPERTIES_LIST.stream().map(PropertyDetail::getColumnName).collect(Collectors.toList());");
    	bw.newLine();
    	
    	bw.append("    public static final String DETAIL_COLUMNS = DETAIL_COLUMN_LIST.stream().collect(Collectors.joining(\", \"));");
    	bw.newLine();
    	bw.append("    public static final String ID_COLUMNS = ID_COLUMN_LIST.stream().collect(Collectors.joining(\", \"));");
    	bw.newLine();
    	bw.append("    public static final String ALL_COLUMNS = ID_COLUMNS + \", \" + DETAIL_COLUMNS;");
    	bw.newLine();
    	bw.append("    public static final String ID_COLUMNS_QUERY = ID_COLUMN_LIST.stream().map(col-> col + \" = ? \").collect(Collectors.joining(\" AND \"));");
    	bw.newLine();
    	bw.newLine();
    	bw.append("    public static final String DETAIL_PARAMS = IntStream.range(0, DETAIL_PROPERTIES_LIST.size()).mapToObj(i->\"?\").collect(Collectors.joining(\", \"));");
    	bw.newLine();
    	bw.append("    public static final String ID_PARAMS = IntStream.range(0, ID_PROPERTIES_LIST.size()).mapToObj(i->\"?\").collect(Collectors.joining(\", \"));");
    }
    
    private void writeMapper(BufferedWriter bw, TypeDetails td, Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	bw.append("    public static final ").append(td.getSimpleName()).append("RowMapper MAPPER = new ").append(td.getSimpleName()).append("RowMapper();");
    	for (PropertyDetails prop: td.getProperties().values()) {
    		if (prop.getPropertyType() == PropertyType.MANY_TO_ONE) {
    			TypeDetails referenceType = entities.get(prop.getType().toString());
    			if (referenceType!=null) {
    				bw.newLine();
    				bw.append("    public static final ")
						.append(referenceType.getSimpleName()).append("JdbcTemplate.").append(referenceType.getSimpleName()).append("IdRowMapper ")
						.append(referenceType.getSimpleName().toUpperCase()).append("_ID_MAPPER = new ")
	    				.append(referenceType.getSimpleName()).append("JdbcTemplate.").append(referenceType.getSimpleName()).append("IdRowMapper(\"").append(prop.getColumnName()).append("\");");
    				
    			}
			}
    	}
    }

    private void writeMapperDefinition(BufferedWriter bw, TypeDetails td,
			Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	bw.append("    public static final class ");
    	bw.append(td.getSimpleName()).append("RowMapper implements RowMapper<").append(td.getSimpleName()).append("> {");
		bw.newLine();
		bw.append("        private final String columnPrefix;");
    	for (PropertyDetails prop: td.getProperties().values()) {
    		if (prop.isEmbedded() && !prop.isNested()) {
    			continue;
    		}
    		bw.newLine();
    		bw.append("        private final String "+prop.getName().replace('.', '_')+"ColName;");
    	}
    	bw.newLine();
    	bw.newLine();
    	bw.append("        public ").append(td.getSimpleName()).append("RowMapper() {");
    	bw.newLine();
    	bw.append("            this(\"\");");
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	
    	bw.newLine();
    	bw.append("        public ").append(td.getSimpleName()).append("RowMapper(String columnPrefix) {");
    	bw.newLine();
    	bw.append("            this.columnPrefix = columnPrefix;");
    	for (PropertyDetails prop: td.getProperties().values()) {
    		if (prop.isEmbedded() && !prop.isNested()) {
    			continue;
    		}
    		bw.newLine();
    		bw.append("            ").append(prop.getName().replace('.', '_')).append("ColName = columnPrefix + \"").append(prop.getColumnName()).append("\";");
    	}
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();

    	bw.newLine();
    	bw.append("        @Override");
    	bw.newLine();
    	bw.append("        public ").append(td.getSimpleName()).append(" mapRow(ResultSet rs, int rowNum) throws SQLException {");
    	bw.newLine();
    	bw.append("             final ").append(td.getSimpleName()).append(" o = new ").append(td.getSimpleName()).append("();");
    	bw.newLine();
    	for (PropertyDetails prop: td.getProperties().values()) {
    		bw.newLine();
    		if (prop.isEmbedded() && !prop.isNested()) {
    			bw.append("             ").append(prop.getType().toString()).append(" ")
    				.append(prop.getName()).append(" = new ")
    				.append(prop.getType().toString()).append("();");
    			bw.newLine();
        		bw.append("             o.").append(prop.getSetterName()).append("(").append(prop.getName()).append(");");
        		continue;
    		}
    		
    		if (prop.isEmbedded() && prop.isNested()) {
    			bw.append("             ").append(prop.getParentProperty()).append(".").append(prop.getSetterName()).append("(");
    		} else {
    			bw.append("             o.").append(prop.getSetterName()).append("(");
    		}
    		if (prop.getPropertyType() == PropertyType.MANY_TO_ONE) {
    			TypeDetails referenceType = entities.get(prop.getType().toString());
    			if (referenceType!=null) {
    				bw.append(referenceType.getSimpleName().toUpperCase()).append("_ID_MAPPER.mapRow(rs,rowNum));");
    				continue;
    			}
    		}
    		
    		String jdbcType = prop.getJdbcType();
    		if ("Object".equals(jdbcType)) {
    			bw.append("(").append(prop.getType().toString()).append(")");
    		}
    		bw.append("rs.get").append(jdbcType).append("(").append(prop.getName().replace('.', '_')).append("ColName));");
    	}
		bw.newLine();
    	bw.append("             return o;");
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	bw.newLine();
    	bw.append("        public String getColumnPrefix() {");
    	bw.newLine();
    	bw.append("            return columnPrefix;");
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	bw.append("    }");
	}

    private void writeIdMapperDefinition(BufferedWriter bw, TypeDetails td,
    		Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	bw.append("    public static final class ");
    	bw.append(td.getSimpleName()).append("IdRowMapper implements RowMapper<").append(td.getSimpleName()).append("> {");
		bw.newLine();
		List<PropertyDetails> idProperties = td.getProperties().values().stream().filter(prop->prop.isId() && (!prop.isEmbedded() || prop.isNested()))
				.collect(Collectors.toList());
    	for (PropertyDetails prop: idProperties) {
    		bw.newLine();
    		bw.append("        private final String "+prop.getName().replace('.', '_')+"ColName;");
    	}
    	bw.newLine();
    	bw.newLine();
    	bw.append("        public ").append(td.getSimpleName()).append("IdRowMapper() {");
    	bw.newLine();
    	bw.append("            this(");
    	boolean first = true;
    	for (PropertyDetails prop: idProperties) {
    		if (first) {
    			first = false;
    		} else {
    			bw.append(", ");
    		}
    		bw.append("\"").append(prop.getColumnName()).append("\"");
    	}
    	bw.append(");");
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	
    	bw.newLine();
    	bw.append("        public ").append(td.getSimpleName()).append("IdRowMapper(");
    	first = true;
    	for (PropertyDetails prop: idProperties) {
    		if (first) {
    			first = false;
    		} else {
    			bw.append(", ");
    		}
    		bw.append("String ").append(prop.getName().replace('.', '_')).append("ColName");
    	}
    	bw.append(") {");
    	for (PropertyDetails prop: idProperties) {
    		bw.newLine();
    		bw.append("            this.").append(prop.getName().replace('.', '_')).append("ColName = ").append(prop.getName().replace('.', '_')).append("ColName;");
    	}
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	
    	bw.newLine();
    	bw.append("        @Override");
    	bw.newLine();
    	bw.append("        public ").append(td.getSimpleName()).append(" mapRow(ResultSet rs, int rowNum) throws SQLException {");
    	bw.newLine();
    	bw.append("             final ").append(td.getSimpleName()).append(" o = new ").append(td.getSimpleName()).append("();");
    	bw.newLine();
    	for (PropertyDetails prop: td.getProperties().values()) {
    		if (!prop.isId()) {
    			continue;
    		}
    		bw.newLine();
    		if (prop.isEmbedded() && !prop.isNested()) {
    			bw.append("             ").append(prop.getType().toString()).append(" ")
    				.append(prop.getName()).append(" = new ")
    				.append(prop.getType().toString()).append("();");
    			bw.newLine();
        		bw.append("             o.").append(prop.getSetterName()).append("(").append(prop.getName()).append(");");
        		continue;
    		}
    		
    		if (prop.isEmbedded() && prop.isNested()) {
    			bw.append("             ").append(prop.getParentProperty()).append(".").append(prop.getSetterName()).append("(");
    		} else {
    			bw.append("             o.").append(prop.getSetterName()).append("(");
    		}
    		String jdbcType = prop.getJdbcType();
    		if ("Object".equals(jdbcType)) {
    			bw.append("(").append(prop.getType().toString()).append(")");
    		}
    		bw.append("rs.get").append(jdbcType).append("(").append(prop.getName().replace('.', '_')).append("ColName));");
    	}
    	bw.newLine();
    	bw.append("             if (hasId(o)) {");
    	bw.newLine();
    	bw.append("                 return o;");
    	bw.newLine();
    	bw.append("             } else {");
    	bw.newLine();
    	bw.append("                 return null;");
    	bw.newLine();
    	bw.append("             }");
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	bw.append("    }");
    }
    
    private void writeMerge(BufferedWriter bw, TypeDetails td,
			Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	String objectParam = td.getSimpleName().substring(0, 1).toLowerCase()+td.getSimpleName().substring(1);
		bw.append("    public ").append(td.getSimpleName()).append(" merge(").append(td.getSimpleName()).append(" ").append(objectParam).append(") {");
		bw.newLine();
        bw.append("        if (!hasId(").append(objectParam).append(")) {");
        bw.newLine();
        bw.append("            return insert(").append(objectParam).append(");");
        bw.newLine();
        bw.append("        } else {");
        bw.newLine();
        bw.append("            return update(").append(objectParam).append(");");
        bw.newLine();
        bw.append("        }");
        bw.newLine();
        bw.append("    }");
        bw.newLine();
    }
    
    private void writeInsert(BufferedWriter bw, TypeDetails td,
			Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	String objectParam = td.getSimpleName().substring(0, 1).toLowerCase()+td.getSimpleName().substring(1);
		bw.append("    public ").append(td.getSimpleName()).append(" insert(").append(td.getSimpleName()).append(" ").append(objectParam).append(") {");
		bw.newLine();
		bw.append("        if (ID_PROPERTIES_LIST.size()==1 && ID_PROPERTIES_LIST.get(0).getGenerationType()!=null) {");
		bw.newLine();
        bw.append("            final GeneratedKeyHolder holder = new GeneratedKeyHolder();");
        bw.newLine();
        bw.append("            getJdbcTemplate().update(new SimplePreparedStatementCreator(");
        bw.newLine();
        bw.append("                    \"insert into \"+TABLENAME+\"(\" + ALL_COLUMNS + \") values (default, \" + DETAIL_PARAMS+ \")\",");
        bw.newLine();
        bw.append("                prepareStatement -> {");
        bw.newLine();
        bw.append("                    for (int col = 0; col<DETAIL_PROPERTIES_LIST.size(); col++) {");
        bw.newLine();
        bw.append("                        prepareStatement.setObject(col+1, getPropertyValue(").append(objectParam).append(", DETAIL_PROPERTIES_LIST.get(col).getName()));");
        bw.newLine();
        bw.append("                    }");
        bw.newLine();
        bw.append("                }), holder);");
        bw.newLine();
        bw.append("            List<Object> idValues = new ArrayList<>(holder.getKeys().values());");
        bw.newLine();
        bw.append("            setPropertyValue(").append(objectParam).append(", ID_PROPERTIES_LIST.get(0).getName(), idValues.get(0));");
        bw.newLine();
        bw.append("        } else {");
        bw.newLine();
        bw.append("            getJdbcTemplate().update(\"insert into \"+TABLENAME+\" (\" + ALL_COLUMNS + \") values (\" + ID_PARAMS + \", \" + DETAIL_PARAMS + \")\",");
        bw.newLine();
        bw.append("                prepareStatement -> {");
        bw.newLine();
        bw.append("                    for (int col = 0; col<ID_PROPERTIES_LIST.size(); col++) {");
        bw.newLine();
        bw.append("                        prepareStatement.setObject(DETAIL_PROPERTIES_LIST.size()+col+1, getPropertyValue(").append(objectParam).append(", ID_PROPERTIES_LIST.get(col).getName()));");
        bw.newLine();
        bw.append("                    }");
        bw.newLine();
        bw.append("                    for (int col = 0; col<DETAIL_PROPERTIES_LIST.size(); col++) {");
        bw.newLine();
        bw.append("                        prepareStatement.setObject(col+1, getPropertyValue(").append(objectParam).append(", DETAIL_PROPERTIES_LIST.get(col).getName()));");
        bw.newLine();
        bw.append("                    }");
        bw.newLine();
        bw.append("                }");
        bw.newLine();
        bw.append("            );");
        bw.newLine();
        bw.append("        }");
        bw.newLine();
        bw.append("        return ").append(objectParam).append(";");
        bw.newLine();
        bw.append("    }");
        bw.newLine();
    }
    private void writeUpdate(BufferedWriter bw, TypeDetails td,
    		Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	String objectParam = td.getSimpleName().substring(0, 1).toLowerCase()+td.getSimpleName().substring(1);
    	bw.append("    public ").append(td.getSimpleName()).append(" update(").append(td.getSimpleName()).append(" ").append(objectParam).append(") {");
    	bw.newLine();
    	bw.append("        getJdbcTemplate().update(\"UPDATE \"+TABLENAME+\" SET (\" + DETAIL_COLUMNS + \") = (\" + DETAIL_PARAMS + \") WHERE (\"+ID_COLUMNS_QUERY+\")\",");
    	bw.newLine();
    	bw.append("            prepareStatement -> {");
    	bw.newLine();
    	bw.append("                for (int col = 0; col<DETAIL_PROPERTIES_LIST.size(); col++) {");
    	bw.newLine();
    	bw.append("                    prepareStatement.setObject(col+1, getPropertyValue(").append(objectParam).append(", DETAIL_PROPERTIES_LIST.get(col).getName()));");
    	bw.newLine();
    	bw.append("                }");
    	bw.newLine();
    	bw.append("                for (int col = 0; col<ID_PROPERTIES_LIST.size(); col++) {");
    	bw.newLine();
    	bw.append("                    prepareStatement.setObject(DETAIL_PROPERTIES_LIST.size()+col+1, getPropertyValue(").append(objectParam).append(", ID_PROPERTIES_LIST.get(col).getName()));");
    	bw.newLine();
    	bw.append("                }");
    	bw.newLine();
    	bw.append("            }");
    	bw.newLine();
    	bw.append("        );");
    	bw.newLine();
    	bw.append("        return ").append(objectParam).append(";");
    	bw.newLine();
    	bw.append("    }");
    	bw.newLine();
    }

    private void writeGetValue(BufferedWriter bw, TypeDetails td,
			Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	String objectParam = td.getSimpleName().substring(0, 1).toLowerCase()+td.getSimpleName().substring(1);
    	bw.append("    public static Object getPropertyValue(").append(td.getSimpleName()).append(" ").append(objectParam).append(", String prop) {");
    	bw.newLine();
    	bw.append("        switch(prop) {");
    	bw.newLine();
    	for (PropertyDetails prop: td.getProperties().values()) {
    		bw.append("            case \"").append(prop.getName()).append("\":");
    		if (prop.isNested()) {
    			bw.append("{");
    			bw.newLine();
    			PropertyDetails parentProperty = td.getProperties().get(prop.getParentProperty());
    			bw.append("                ")
    				.append(parentProperty.getType().toString()).append(" parent = (").append(parentProperty.getType().toString()).append(")getPropertyValue(")
    					.append(objectParam).append(",\"").append(parentProperty.getName()).append("\");");
	    		bw.newLine();
    			bw.append("                return parent==null?null:");
    			appendGetterCall("parent", prop, bw, entities);
	    		bw.append(";");
	    		bw.newLine();
	    		bw.append("            }");
	    		bw.newLine();
    		} else {
	    		bw.newLine();
	    		bw.append("                return ");
	    		appendGetterCall(objectParam, prop, bw, entities);
	    		bw.append(";");
	    		bw.newLine();
    		}
    	}
    	bw.append("            default:");
    	bw.newLine();
    	bw.append("                throw new IllegalArgumentException(\"Property \"+prop+\" unknown.\");");
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	bw.append("    }");
    }

    private void writeHasId(BufferedWriter bw, TypeDetails td,
    		Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	String objectParam = td.getSimpleName().substring(0, 1).toLowerCase()+td.getSimpleName().substring(1);
    	bw.append("    public static boolean hasId(").append(td.getSimpleName()).append(" ").append(objectParam).append(") {");
    	bw.newLine();
    	bw.append("        return ID_PROPERTIES_LIST.stream().anyMatch(p->getPropertyValue(").append(objectParam).append(", p.getName())!=null);");
    	bw.newLine();
    	bw.append("    }");
    }
    
    private void writeSetValue(BufferedWriter bw, TypeDetails td,
			Map<String, TypeDetails> entities) throws IOException {
    	bw.newLine();
    	String objectParam = td.getSimpleName().substring(0, 1).toLowerCase()+td.getSimpleName().substring(1);
    	bw.append("    public static void setPropertyValue(").append(td.getSimpleName()).append(" ").append(objectParam).append(", String prop, Object value) {");
    	bw.newLine();
    	bw.append("        switch(prop) {");
    	bw.newLine();
    	for (PropertyDetails prop: td.getProperties().values()) {
    		bw.append("            case \"").append(prop.getName()).append("\":");
    		if (prop.isNested()) {
    			bw.append("{");
    			bw.newLine();
    			PropertyDetails parentProperty = td.getProperties().get(prop.getParentProperty());
    			bw.append("                 ")
    				.append(parentProperty.getType().toString()).append(" parent = (").append(parentProperty.getType().toString()).append(")getPropertyValue(")
    					.append(objectParam).append(",\"").append(parentProperty.getName()).append("\");");
	    		bw.newLine();
	    		bw.append("                 if (parent!=null) {");
	    		bw.newLine();
	    		bw.append("                     parent = new ").append(parentProperty.getType().toString()).append("();");
	    		bw.newLine();
	    		bw.append("                     setPropertyValue(").append(objectParam).append(", \"").append(parentProperty.getName()).append("\", parent);");
	    		bw.newLine();
	    		bw.append("                 }");
	    		bw.newLine();
	    		bw.append("                 parent.").append(prop.getSetterName()).append("((").append(prop.getType().toString()).append(")value);");
	    		bw.newLine();
	    		bw.append("                 break;");
	    		bw.newLine();
	    		bw.append("            }");
	    		bw.newLine();
	    		continue;
    		} 
    		bw.newLine();
    		if (prop.getPropertyType() == PropertyType.MANY_TO_ONE) {
    			TypeDetails referenceType = entities.get(prop.getType().toString());
    			if (referenceType!=null) {
    				PropertyDetails referenceIdDetails = referenceType.getIdDetails();
    				if (referenceIdDetails==null) {
    					messager.printMessage(Kind.ERROR, "No id property found for type "+referenceType.getName());
    					throw new IllegalStateException("No id property found for type "+referenceType.getName());
    				}
    	    		bw.append("                 ").append(objectParam).append(".").append(prop.getSetterName()).append("((").append(referenceType.getName()).append(")value);");
    	    		bw.newLine();
    	    		bw.append("                 break;");
    	    		bw.newLine();
    				continue;
    			}
    		}
    		bw.append("                 ").append(objectParam).append(".").append(prop.getSetterName()).append("((").append(prop.getType().toString()).append(")value);");
    		bw.newLine();
    		bw.append("                 break;");
    		bw.newLine();
    	}
    	bw.append("            default:");
    	bw.newLine();
    	bw.append("                throw new IllegalArgumentException(\"Property \"+prop+\" unknown.\");");
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	bw.append("    }");
    }

    private void writeSelectById(BufferedWriter bw, TypeDetails td) throws IOException {
    	bw.newLine();
    	bw.append("    public ").append(td.getSimpleName()).append(" getById(");
    	boolean first = true;
    	for (PropertyDetails pd : td.getAllIdDetails()) {
    		if (pd.isNested()) {
    			continue;
    		}
    		if (first) {
    			first = false;
    		} else {
    			bw.append(", ");
    		}
    		bw.append(pd.getType().toString()).append(" ").append(pd.getName());
    	}
    	bw.append(") {");
    	bw.newLine();
    	bw.append("        try {");
    	bw.newLine();
    	bw.append("            return getJdbcTemplate().queryForObject(");
    	bw.newLine();
    	bw.append("                \"select \" + ALL_COLUMNS + \" from \" + TABLENAME + \" where \" + ID_COLUMNS_QUERY, new Object[] {");
    	first =true;
    	for (PropertyDetails pd : td.getAllIdDetails()) {
    		if (pd.isEmbedded() && !pd.isNested()) {
    			continue;
    		}
    		if (first) {
    			first = false;
    		} else {
    			bw.append(", ");
    		}
    		if (pd.isEmbedded()) {
    			appendGetterCall(pd.getParentProperty(), pd, bw, Collections.emptyMap());
    		} else {
    			bw.append(pd.getName());
    		}
    	}
    	bw.append("},");
    	bw.newLine();
    	bw.append("                MAPPER);");
    	bw.newLine();
    	bw.append("        } catch (EmptyResultDataAccessException e) {");
    	bw.newLine();
    	bw.append("            return null;");
    	bw.newLine();
    	bw.append("        }");
    	bw.newLine();
    	bw.append("    }");
    }

    private void writeSelectAll(BufferedWriter bw, TypeDetails td) throws IOException {
    	bw.newLine();
    	bw.append("    public List<").append(td.getSimpleName()).append("> findAll() {");
    	bw.newLine();
    	bw.append("        return getJdbcTemplate().query(");
    	bw.newLine();
    	bw.append("                \"select \" + ALL_COLUMNS + \" from \" + TABLENAME, new Object[] {},");
    	bw.newLine();
    	bw.append("                MAPPER);");
    	bw.newLine();
    	bw.append("    }");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
    
    private BufferedWriter appendGetterCall(String objectParam, PropertyDetails property, BufferedWriter bw, Map<String, TypeDetails> entities) throws IOException {
    	if (property==null || property.getType()==null) {
    		messager.printMessage(Kind.ERROR, "Problem writing property "+property+" - "+property==null?"null":property.getName());
    	}
		if (property.getType().getKind()==TypeKind.DECLARED && entities.get(property.getType().toString())!=null) {
			TypeDetails referenceType = entities.get(property.getType().toString());
			PropertyDetails referenceIdDetails = referenceType.getIdDetails();
			if (referenceIdDetails==null) {
				messager.printMessage(Kind.ERROR, "No id property found for type "+referenceType.getName());
				throw new IllegalStateException("No id property found for type "+referenceType.getName());
			}
			bw.append(objectParam).append(".").append(property.getGetterName()).append("()==null?null:").append(objectParam)
									.append(".").append(property.getGetterName()).append("().")
									.append(referenceIdDetails.getGetterName()).append("()");
		} else {
			bw.append(objectParam).append(".").append(property.getGetterName()).append("()");
		}
		return bw;
    }
    /*
   	private class MyObjectExtractor implements ResultSetExtractor{

        public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
            Map<Integer, MyObject> map = new HashMap<Integer, MyObject>();
            MyObject myObject = null;
            while (rs.next()) {
            	Integer id = rs.getInt("ID);
            	myObject = map.get(id);
              if(myObject == null){
                  String description = rs,getString("Description");
                  myObject = new MyObject(id, description);
                  map.put(id, myObject);
              }
	      MyFoo foo = new MyFoo(rs.getString("Foo"), rs.getString("Bar"));
	      myObject.add(myFoo);
            }
            return new ArrayList<MyObject>(map.values());;
        }
    }    
     */
}
