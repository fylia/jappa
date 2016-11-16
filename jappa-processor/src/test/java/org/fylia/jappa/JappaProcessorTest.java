package org.fylia.jappa;

import java.io.IOException;
import java.util.Arrays;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourceSubjectFactory;
import com.google.testing.compile.JavaSourcesSubjectFactory;

public class JappaProcessorTest {

	@Test
	public void testSupplier() throws IOException {
		JavaFileObject fileObject = JavaFileObjects.forResource("org/fylia/jappa/model/Supplier.java");
		JavaFileObject destinationObject = JavaFileObjects.forResource("org/fylia/jappa/dao/impl/SupplierJdbcTemplate.java");
		Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
		     .that(fileObject)
		     .withCompilerOptions("-AdestinationPackage=org.fylia.jappa.dao.impl")
		     .processedWith(new JappaProcessor())
		     .compilesWithoutError().and()
		     .generatesFileNamed(
		    		 javax.tools.StandardLocation.SOURCE_OUTPUT,"org.fylia.jappa.dao.impl", "SupplierJdbcTemplate.java")
		     /*
		     .and()
		     .generatesSources(destinationObject)*/;
	}

	@Test
	public void testArticle() throws IOException {
		JavaFileObject supplierFileObject = JavaFileObjects.forResource("org/fylia/jappa/model/Supplier.java");
		JavaFileObject supplierDestinationObject = JavaFileObjects.forResource("org/fylia/jappa/dao/impl/SupplierJdbcTemplate.java");
		JavaFileObject articleFileObject = JavaFileObjects.forResource("org/fylia/jappa/model/Article.java");
		JavaFileObject articleDestinationObject = JavaFileObjects.forResource("org/fylia/jappa/dao/impl/ArticleJdbcTemplate.java");
		
		Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
		     .that(Arrays.asList(supplierFileObject, articleFileObject))
		     .withCompilerOptions("-AdestinationPackage=org.fylia.jappa.dao.impl")
		     .processedWith(new JappaProcessor())
		     .compilesWithoutError().and()
		     .generatesFileNamed(
		    		 javax.tools.StandardLocation.SOURCE_OUTPUT,"org.fylia.jappa.dao.impl", "ArticleJdbcTemplate.java")
		     /*.and()
		     .generatesSources(supplierDestinationObject, articleDestinationObject)*/;
	}
}
