# jappa
Java Annotation Processor for the Persistence API

Over the years I have found that hibernate and Ibatis/Mybatis are sometimes a bit too much: 
the eagerly of relations, the "org.hibernate.LazyInitializationException - could not initialize proxy - no Session", or the excessive reloading of data if you know it's not needed. 
In smaller projects I started to prefer spring JdbcTemplate. A small framework around pure jdbc. Fast and no fuss.
Unfortunately, the mapping of properties needs to be written manually, which is not really fun. I kinda like the annotations in the persistence API. 

So I started working on this annotation processor which uses the persistence API annotations and generates some base code and mappers to use with JdbcTemplate. 

The project consists of 3 parts:
* **jappa-core** which contains classes that will be used in the generated code. This is a dependency you'll need in your project and will be LGPL licensed.
* **jappa-processor** The actual Java annotation processor. (GPL3)
* **jappa-test** A project which uses the generated code and tests it. (GPL3)

The project is still in a "proof of concept" 0.0.1-SNAPSHOT stage:
* Testing is still limited
* The processor and generated code need quite some refactoring to make it more readable. 
* There is nothing forseen to load relations
* Everything is still subject to change.
* but **it already works and generates usefull, runnable code.**

