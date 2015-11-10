TameStaJ
===

Copyright 2015 Maximilian Scherr

A prototype implementation of a framework for the *tame staging* of EDSL (*embedded domain-specific language*) programs. It is an exploration of a semi-linguistic abstraction (here the `@Stage` annotation, `Language` classes, and the associated background machinery) to more directly support (a guided form of) language embedding in Java.

Dependencies
---

* Javassist 3 (3.20.0-GA)
* JDK 8


How to Build the Java Agent
---

After compilation use the supplied `MANIFEST.MF` file to build a JAR file with name `tamestaj.jar` or any other suitable name (with JVM start-up options adjusted accordingly).


Usage
---

Applications are to be started using:

```
java -javaagent:tamestaj.jar ...
```

The agent will automatically detect `@Stage` annotations on methods and fields and transforms code that refers (i.e. uses, calls, reads, or accesses) to them. Reified terms are automatically plumbed to their respective `Language` classes' `make...Closure(...)` methods when *materialization* is triggered by language boundaries, i.e. when terms are consumed externally.


Reference
---

Maximilian Scherr and Shigeru Chiba. [Almost First-Class Language Embedding: Taming Staged Embedded DSLs](http://dl.acm.org/citation.cfm?id=2814217). In *Proceedings of the 2015 ACM SIGPLAN International Conference on Generative Programming: Concepts and Experiences*, pages 21-30, New York, NY, USA, 2015. ACM.
