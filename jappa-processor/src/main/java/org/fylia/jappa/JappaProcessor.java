package org.fylia.jappa;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

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
    private Configuration configuration;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        
        messager = processingEnv.getMessager();
        options = processingEnv.getOptions();
        typeUtil = processingEnv.getTypeUtils();
        
        configuration = new Configuration(Configuration.VERSION_2_3_25);
        configuration.setClassForTemplateLoading(JappaProcessor.class, "/templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);

         
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
                    td.getProperties().forEach((s,pd)->pd.setReferenceType(entities.get(pd.getType().toString())));
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
            Map<String, Object> root = new HashMap<String, Object>();
            root.put("options",  options);
            root.put("type", td);
            root.put("entities", entities);
            Template temp = configuration.getTemplate("JdbcTemplate.ftl");
            temp.process(root, bw);
            bw.flush();
            bw.close();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
    }
    

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
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
