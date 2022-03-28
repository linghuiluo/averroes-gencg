/**
 * ***************************************************************************** Copyright (c) 2015
 * Karim Ali and Ondřej Lhoták. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Karim Ali - initial API and implementation and/or initial documentation
 * *****************************************************************************
 */
package averroes.soot;

import averroes.FrameworkType;
import averroes.gencg.AnnotationEntryPointMethodDetector;
import averroes.gencg.EntryPointConfigurationReader;
import averroes.gencg.EntryPointTypeTag;
import averroes.gencg.SpringEntryPointMethodDetector;
import averroes.options.AverroesOptions;
import averroes.tamiflex.TamiFlexFactsDatabase;
import averroes.util.io.Paths;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LocalGenerator;
import soot.LongType;
import soot.Modifier;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.VoidType;
import soot.baf.Baf;
import soot.baf.BafASMBackend;
import soot.baf.SpecialInvokeInst;
import soot.dava.internal.javaRep.DIntConstant;
import soot.javaToJimple.DefaultLocalGenerator;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.CmpExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.EqExpr;
import soot.jimple.FloatConstant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LongConstant;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.scalar.NopEliminator;
import soot.options.Options;

/**
 * The master-mind of Averroes. That's where the magic of generating code for library classes
 * happen.
 *
 * @author karim
 * @author Linghui Luo
 */
public class CodeGenerator {

  private static CodeGenerator instance = new CodeGenerator();

  private HashMap<SootClass, SootClass> libraryInterfaceToConcreteImplementationClass;
  private HashMap<SootClass, SootClass> abstractLibraryClassToConcreteImplementationClass;

  private int generatedMethodCount;
  private int generatedClassCount;

  private SootClass averroesLibraryClass = null;
  private SootClass averroesAbstractLibraryClass = null;
  private AverroesJimpleBody doItAllBody = null;
  private Map<SootClass, SootClass> entryPointClasses = null;

  private HashMap<SootClass, SootClass>
      instrumentedInterfaces; // (original class, its instrumented interface)
  private Map<SootClass, Set<SootMethod>> objectProviders = null;
  public static Set<String> instrumentedClasses;

  /** Create a new code generator with the given class Cleanup.v(). */
  private CodeGenerator() {
    libraryInterfaceToConcreteImplementationClass = new HashMap<SootClass, SootClass>();
    abstractLibraryClassToConcreteImplementationClass = new HashMap<SootClass, SootClass>();
    instrumentedInterfaces = new HashMap<>();
    entryPointClasses = new HashMap<>();
    objectProviders = new HashMap<>();
    instrumentedClasses = new HashSet<>();
    generatedMethodCount = 0;
    generatedClassCount = 0;
    initialize();
  }

  /**
   * Get the CodeGenerator singleton.
   *
   * @return
   */
  public static CodeGenerator v() {
    return instance;
  }

  /**
   * Write the class file for the given library class.
   *
   * @param cls
   * @throws IOException
   */
  public static void writeClassFile(String outputDir, SootClass cls) throws IOException {
    Options.v().set_output_dir(outputDir);
    int java_version = Options.v().java_version();
    String fileName = SourceLocator.v().getFileNameFor(cls, Options.output_format_class);
    File file = new File(fileName);
    file.getParentFile().mkdirs();

    OutputStream streamOut = new FileOutputStream(fileName);
    try {
      BafASMBackend backend = new BafASMBackend(cls, java_version);
      backend.generateClassFile(streamOut);

    } catch (Exception e) {
      streamOut.close();
      Files.deleteIfExists(file.toPath());
      System.out.println("Could not generate class " + cls.getName());
      e.printStackTrace();
    }
  }

  /**
   * Get the number of generated methods.
   *
   * @return
   */
  public int getGeneratedMethodCount() {
    return generatedMethodCount;
  }

  /**
   * Get the number of generated classes.
   *
   * @return
   */
  public int getGeneratedClassCount() {
    return generatedClassCount;
  }

  /**
   * Get the concrete implementation class for the given library interface.
   *
   * @param libraryInterface
   * @return
   */
  public SootClass getConcreteImplementationClassOfLibraryInterface(SootClass libraryInterface) {
    return libraryInterfaceToConcreteImplementationClass.get(libraryInterface);
  }

  /**
   * Get the Averroes library class where all the fun takes place ;)
   *
   * @return
   */
  public SootClass getAverroesLibraryClass() {
    return averroesLibraryClass;
  }

  /**
   * Get the Averroes abstract library class
   *
   * @return
   */
  public SootClass getAverroesAbstractLibraryClass() {
    return averroesAbstractLibraryClass;
  }

  /**
   * Get the doItAll method.
   *
   * @return
   */
  public SootMethod getAverroesAbstractDoItAll() {
    return averroesAbstractLibraryClass.getMethodByName(Names.AVERROES_DO_IT_ALL_METHOD_NAME);
  }

  /**
   * Get the libraryPointsTo field.
   *
   * @return
   */
  public SootField getAverroesLibraryPointsTo() {
    return averroesAbstractLibraryClass.getField(
        Hierarchy.signatureToSubsignature(Names.LIBRARY_POINTS_TO_FIELD_SIGNATURE));
  }

  /**
   * Get the finalizePointsTo field.
   *
   * @return
   */
  public SootField getAverroesFinalizePointsTo() {
    return averroesAbstractLibraryClass.getField(
        Hierarchy.signatureToSubsignature(Names.FINALIZE_POINTS_TO_FIELD_SIGNATURE));
  }

  /**
   * Get the instance field.
   *
   * @return
   */
  public SootField getAverroesInstanceField() {
    return averroesAbstractLibraryClass.getField(
        Hierarchy.signatureToSubsignature(Names.INSTANCE_FIELD_SIGNATURE));
  }

  /**
   * Get the guard field.
   *
   * @return
   */
  public SootField getAverroesGuardField() {
    return averroesAbstractLibraryClass.getField(
        Hierarchy.signatureToSubsignature(Names.AVE_GUARD_FIELD_SIGNATURE));
  }

  /**
   * Create the Averroes library class where all the fun takes place ;)
   *
   * @throws IOException
   */
  public void createAverroesLibraryClass() throws IOException {
    // Create the abstract library class (specifically to be compatible with
    // WALA/Java).
    // This class represents the interface of the AverroesLibraryClass and
    // will be part of the primordial library that is added to WALA.
    if (averroesAbstractLibraryClass == null) {
      // Create the class
      averroesAbstractLibraryClass =
          new SootClass(Names.AVERROES_ABSTRACT_LIBRARY_CLASS, Modifier.PUBLIC);
      averroesAbstractLibraryClass.setSuperclass(Hierarchy.v().getJavaLangObject());

      // Create the constructor that calls the JavaLangObject constructor
      createAverroesAbstractLibraryInit();

      // Create the LPT field, FIN field, instance field, and the Guard
      createAverroesAbstractLibraryFields();

      // Create the abstract doItAll method
      createAverroesAbstractLibraryDoItAll();

      // Write the class file to disk
      writeClassFile(Paths.libraryClassesOutputDirectory().getPath(), averroesAbstractLibraryClass);
    }

    // Now create the AverroesLibraryClass which basically implements the
    // doItAll method and assigns itself to AbstractLibrary.instance
    // This class could then be treated as part of the application, which
    // leads to (in the future) just regenerating this class instead of
    // regenerating the whole placeholder library
    if (averroesLibraryClass == null) {
      // Create the class
      averroesLibraryClass = new SootClass(Names.AVERROES_LIBRARY_CLASS, Modifier.PUBLIC);
      averroesLibraryClass.setSuperclass(averroesAbstractLibraryClass);

      // Add a default empty constructor
      createAverroesLibraryInit();

      // Create its static initalizer
      createAverroesLibraryClinit();

      createMainMethod();

      // Create the dotItAll method
      createAverroesLibraryDoItAll();

      // Write the class file to disk
      writeClassFile(Paths.libraryClassesOutputDirectory().getPath(), averroesLibraryClass);
    }
  }

  public void createObjects(Map<SootClass, Set<SootField>> createObjects) {
    for (Entry<SootClass, Set<SootField>> e : createObjects.entrySet()) {
      SootClass c = e.getKey();
      SootMethod defaultInit = c.getMethodUnsafe(Names.DEFAULT_CONSTRUCTOR_SUBSIG);
      if (defaultInit == null) {
        defaultInit = Hierarchy.getNewDefaultConstructor();
        c.addMethod(defaultInit);
        JimpleBody b = new JimpleBody(defaultInit);
        b.insertIdentityStmts();
        InvokeStmt specialInvoke =
            Jimple.v()
                .newInvokeStmt(
                    Jimple.v()
                        .newSpecialInvokeExpr(
                            b.getThisLocal(),
                            Scene.v()
                                .makeConstructorRef(
                                    Scene.v().getSootClass(Names.JAVA_LANG_OBJECT),
                                    Collections.EMPTY_LIST)));
        b.getUnits().add(specialInvoke);
        b.getUnits().add(Jimple.v().newReturnVoidStmt());
        defaultInit.setActiveBody(b);
      }
      Body body = defaultInit.getActiveBody();
      UnitPatchingChain units = body.getUnits();
      for (SootField field : e.getValue()) {

        for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext(); ) {
          final Unit u = iter.next();
          u.apply(
              new AbstractStmtSwitch() {

                public void caseReturnVoidStmt(ReturnVoidStmt returnStmt) {

                  RefType t = (RefType) field.getType();
                  SootClass fieldClass = t.getSootClass();
                  LinkedHashSet<SootClass> fieldClasses = new LinkedHashSet<>();
                  if (!fieldClass.isConcrete()) {
                    // the field class is interface or abstract;
                    fieldClasses.addAll(Hierarchy.v().getConcreteSubclassesOf(fieldClass));
                    fieldClasses.addAll(Hierarchy.v().getConcreteImplementersOf(fieldClass));
                  } else {
                    fieldClasses.add(fieldClass);
                  }
                  boolean addPredicate = fieldClasses.size() > 1;
                  for (SootClass fc : fieldClasses) {
                    int localCount = body.getLocalCount() + 1;
                    Local local = Jimple.v().newLocal("temp" + localCount, field.getType());
                    body.getLocals().add(local);
                    createObject(body, units, field, returnStmt, local, fc, addPredicate);
                  }
                }

                private void createObject(
                    Body body,
                    UnitPatchingChain units,
                    SootField f,
                    ReturnVoidStmt returnStmt,
                    Local local,
                    SootClass fieldClass,
                    boolean addPredicate) {
                  Unit nopStmt = Jimple.v().newNopStmt(); // placeholder for if target
                  if (addPredicate) {
                    SootClass randomClass =
                        Scene.v().forceResolve("java.lang.Math", SootClass.BODIES);
                    InvokeExpr invoke =
                        Jimple.v()
                            .newStaticInvokeExpr(
                                randomClass.getMethod("random", new ArrayList<>()).makeRef());
                    Local v = Jimple.v().newLocal("v" + local.getName(), DoubleType.v());
                    body.getLocals().add(v);
                    units.insertBefore(Jimple.v().newAssignStmt(v, invoke), returnStmt);
                    CmpExpr compare = Jimple.v().newCmpExpr(v, DoubleConstant.v(0.5));
                    Local p = Jimple.v().newLocal("p" + local.getName(), BooleanType.v());
                    body.getLocals().add(p);
                    units.insertBefore(Jimple.v().newAssignStmt(p, compare), returnStmt);
                    EqExpr cond = Jimple.v().newEqExpr(p, IntConstant.v(0));
                    units.insertBefore(Jimple.v().newIfStmt(cond, nopStmt), returnStmt);
                  }
                  AssignStmt newStmt =
                      Jimple.v()
                          .newAssignStmt(
                              local, Jimple.v().newNewExpr((RefType) fieldClass.getType()));
                  units.insertBefore(newStmt, returnStmt);
                  SootMethod fieldInit = null;
                  if (Hierarchy.hasDefaultConstructor(fieldClass)) {
                    fieldInit = Hierarchy.getDefaultConstructor(fieldClass);
                  } else {
                    fieldInit = Hierarchy.getAnyPublicConstructor(fieldClass);
                  }
                  SootMethodRef constructorRef =
                      Scene.v().makeConstructorRef(fieldClass, fieldInit.getParameterTypes());
                  List<Value> args = new ArrayList<>();
                  for (Type p : constructorRef.getParameterTypes()) {
                    if (isSimpleType(p.toString())) args.add(getSimpleDefaultValue(p));
                    else {
                      args.add(NullConstant.v());
                    }
                  }

                  InvokeStmt specialInvoke =
                      Jimple.v()
                          .newInvokeStmt(
                              Jimple.v().newSpecialInvokeExpr(local, constructorRef, args));
                  units.insertBefore(specialInvoke, returnStmt);
                  InstanceFieldRef fieldRef =
                      Jimple.v().newInstanceFieldRef(body.getThisLocal(), f.makeRef());
                  AssignStmt assign = Jimple.v().newAssignStmt(fieldRef, local);
                  units.insertBefore(assign, returnStmt);
                  if (addPredicate)
                    units.insertBefore(nopStmt, returnStmt); // add the nop placeholder
                }
              });
        }
      }
      body.validate();
    }
  }

  private void createMainMethod() {
    Type type = ArrayType.v(RefType.v("java.lang.String"), 1);
    SootMethod mainMethod =
        new SootMethod(
            "main",
            Arrays.asList(new Type[] {type}),
            VoidType.v(),
            Modifier.PUBLIC | Modifier.STATIC);
    averroesLibraryClass.addMethod(mainMethod);
    AverroesJimpleBody body = new AverroesJimpleBody(mainMethod);

    LocalGenerator localGenerator = new DefaultLocalGenerator(body.getJimpleBody());

    // load AbstractLibrary instance
    SootField aInstance = CodeGenerator.v().getAverroesInstanceField();
    Value field = Jimple.v().newStaticFieldRef(aInstance.makeRef());
    Local ins = localGenerator.generateLocal(aInstance.getType());
    addUnit(body.getJimpleBody(), Jimple.v().newAssignStmt(ins, field));

    HashMap<SootClass, Local> localForClasses = new HashMap<>();
    for (SootClass klass : entryPointClasses.keySet()) {
      Local local = localGenerator.generateLocal(klass.getType());
      localForClasses.put(klass, local);
    }

    // make sure outer class comes first in the list.
    List<SootClass> epClasses = new ArrayList<>(entryPointClasses.keySet());
    Collections.sort(
        epClasses,
        new Comparator<SootClass>() {
          @Override
          public int compare(SootClass o1, SootClass o2) {

            if (o1.getName().startsWith(o2.getName()) || o2.getName().startsWith(o1.getName()))
              return o1.getName().startsWith(o2.getName()) ? 1 : -1;
            else return o1.getName().compareTo(o2.getName());
          }
        });

    for (SootClass klass : epClasses) {
      boolean isInnerClass = klass.getName().contains("$");
      SootClass outerClass =
          isInnerClass
              ? Scene.v()
                  .getSootClassUnsafe(
                      klass.getName().substring(0, klass.getName().lastIndexOf("$")))
              : null;
      // TODO: replace addUnit with methods from AverroesJimpleBody directly
      addUnit(
          body.getJimpleBody(),
          Jimple.v()
              .newAssignStmt(localForClasses.get(klass), Jimple.v().newNewExpr(klass.getType())));

      for (SootMethod method : klass.getMethods()) {
        if (method.getName().equals(SootMethod.constructorName)) {
          SootMethod construtor = method;
          SootMethodRef constructorRef =
              Scene.v().makeConstructorRef(klass, construtor.getParameterTypes());
          List<Value> args = new ArrayList<>();

          for (Type p : constructorRef.getParameterTypes()) {
            boolean added = false;
            if (isInnerClass) {
              if (outerClass != null && outerClass.getType().equals(p)) {
                if (localForClasses.containsKey(outerClass)) {
                  args.add(localForClasses.get(outerClass));
                  added = true;
                }
              }
            }
            if (!added)
              if (isSimpleType(p.toString())) args.add(getSimpleDefaultValue(p));
              else {
                args.add(NullConstant.v());
              }
          }
          addUnit(
              body.getJimpleBody(),
              Jimple.v()
                  .newInvokeStmt(
                      Jimple.v()
                          .newSpecialInvokeExpr(localForClasses.get(klass), constructorRef, args)));
          Local classLocal = localForClasses.get(klass);
          Set<SootClass> parents = new HashSet<>();
          parents.add(entryPointClasses.get(klass));
          parents.addAll(Hierarchy.v().getSuperclassesOf(klass));
          parents.addAll(Hierarchy.v().getSuperinterfacesOf(klass));
          for (SootClass parent : parents) {
            if (parent.getName().equals(Names.JAVA_LANG_OBJECT)) continue;
            SootField typedLPT =
                CodeGenerator.v().createAverroesTypedLibraryPointsToField(parent.getType());
            AssignStmt storeStmt =
                Jimple.v()
                    .newAssignStmt(
                        Jimple.v().newInstanceFieldRef(ins, typedLPT.makeRef()), classLocal);
            addUnit(body.getJimpleBody(), storeStmt);
          }
        }
      }
    }
    for (SootClass cls : Hierarchy.v().getApplicationClasses()) {
      if (skipClass(cls.getName())) continue;
      if (!Hierarchy.isAbstractClass(cls)
          && !cls.isInterface()
          && !entryPointClasses.containsKey(cls)) {
        List<SootClass> allDeclareTypes = new ArrayList<>();
        allDeclareTypes.addAll(Hierarchy.v().getSuperclassesOf(cls));
        allDeclareTypes.addAll(Hierarchy.v().getSuperinterfacesOf(cls));
        allDeclareTypes.add(cls);
        if (allDeclareTypes.size() > 0) {
          List<Stmt> stmts = new ArrayList<>();
          Local classLocal = localGenerator.generateLocal(cls.getType());
          // new stmt
          stmts.add(Jimple.v().newAssignStmt(classLocal, Jimple.v().newNewExpr(cls.getType())));
          SootMethod init = Hierarchy.v().getAnyPublicConstructor(cls);
          if (init != null && init.getName().equals(SootMethod.constructorName)) {
            SootMethodRef constructorRef =
                Scene.v().makeConstructorRef(cls, init.getParameterTypes());
            List<Value> args = new ArrayList<>();
            for (Type p : constructorRef.getParameterTypes()) {
              if (isSimpleType(p.toString())) args.add(getSimpleDefaultValue(p));
              else {

                args.add(NullConstant.v());
              }
            }
            // invoke init
            stmts.add(
                Jimple.v()
                    .newInvokeStmt(
                        Jimple.v().newSpecialInvokeExpr(classLocal, constructorRef, args)));
            for (SootClass p : allDeclareTypes) {
              if (p.getName().equals(Names.JAVA_LANG_OBJECT)) continue;
              SootField typedLPT =
                  CodeGenerator.v().createAverroesTypedLibraryPointsToField(p.getType());
              stmts.add(
                  Jimple.v()
                      .newAssignStmt(
                          Jimple.v().newInstanceFieldRef(ins, typedLPT.makeRef()), classLocal));
            }
          }
          if (stmts.size() >= 3) {
            for (Unit s : stmts) addUnit(body.getJimpleBody(), s);
          }
        }
      }
    }
    // 1. The library can point to any concrete (i.e., not an interface nor
    // abstract) library class
    for (SootClass cls : getConcreteLibraryClasses()) {
      List<Stmt> stmts = initializeClass(cls, localGenerator, ins);
      if (stmts.size() == 3) {
        for (Unit s : stmts) addUnit(body.getJimpleBody(), s);
      }
    }
    // create beans
    for (SootClass cls : objectProviders.keySet()) {

      for (SootMethod provider : objectProviders.get(cls)) {
        SootClass providerCls = provider.getDeclaringClass();
        List<Stmt> stmts = initializeClass(providerCls, localGenerator, ins);
        Stmt last = stmts.get(stmts.size() - 1);
        Local base = localGenerator.generateLocal(cls.getType());
        stmts.add(Jimple.v().newAssignStmt(base, last.getDefBoxes().get(0).getValue()));
        for (Stmt s : stmts) {
          addUnit(body.getJimpleBody(), s);
        }
        // TODO. add predicate around the invoke statement
        body.insertRandomAssignment();
        NopStmt ifStmt = body.insertGuardCondition();
        SootMethodRef methodRef = provider.makeRef();
        List<Value> args = body.prepareActualArguments(provider);
        InvokeExpr invokeExpr = Jimple.v().newVirtualInvokeExpr(base, methodRef, args);
        Local ret = body.newLocal(provider.getReturnType());
        body.getInvokeReturnVariables().add(ret);
        body.insertAssignmentStatement(ret, invokeExpr, false);
        SootField typedLPT =
            CodeGenerator.v().createAverroesTypedLibraryPointsToField((RefLikeType) ret.getType());
        addUnit(
            body.getJimpleBody(),
            Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(ins, typedLPT.makeRef()), ret));
        body.getJimpleBody().getUnits().add(ifStmt);
      }
    }

    // Add return statement
    addUnit(body.getJimpleBody(), Jimple.v().newReturnVoidStmt());
    // Finally validate the Jimple body
    body.getJimpleBody().validate();

    mainMethod.setActiveBody(body.getJimpleBody());
  }

  private List<Stmt> initializeClass(
      SootClass cls, LocalGenerator localGenerator, Local abstractlibraryInstance) {
    List<Stmt> stmts = new ArrayList<>();
    Local classLocal = localGenerator.generateLocal(cls.getType());
    stmts.add(Jimple.v().newAssignStmt(classLocal, Jimple.v().newNewExpr(cls.getType())));
    SootMethod init = Hierarchy.v().getAnyPublicConstructor(cls);
    if (init != null && init.getName().equals(SootMethod.constructorName)) {
      SootMethodRef constructorRef = Scene.v().makeConstructorRef(cls, init.getParameterTypes());
      List<Value> args = new ArrayList<>();
      for (Type p : constructorRef.getParameterTypes()) {
        if (isSimpleType(p.toString())) args.add(getSimpleDefaultValue(p));
        else {
          args.add(NullConstant.v());
        }
      }
      stmts.add(
          Jimple.v()
              .newInvokeStmt(Jimple.v().newSpecialInvokeExpr(classLocal, constructorRef, args)));
      SootField typedLPT = CodeGenerator.v().createAverroesTypedLibraryPointsToField(cls.getType());
      stmts.add(
          Jimple.v()
              .newAssignStmt(
                  Jimple.v().newInstanceFieldRef(abstractlibraryInstance, typedLPT.makeRef()),
                  classLocal));
    }
    return stmts;
  }

  private void addUnit(JimpleBody body, Unit unit) {
    // System.out.println(unit.toString());
    body.getUnits().add(unit);
  }

  protected boolean isSimpleType(String t) {
    if (t.equals("java.lang.String")
        || t.equals("void")
        || t.equals("char")
        || t.equals("byte")
        || t.equals("short")
        || t.equals("int")
        || t.equals("float")
        || t.equals("long")
        || t.equals("double")
        || t.equals("boolean")) {
      return true;
    } else {
      return false;
    }
  }

  protected Value getSimpleDefaultValue(Type t) {
    if (t == RefType.v("java.lang.String")) return StringConstant.v("");
    if (t instanceof CharType) return IntConstant.v(0);
    if (t instanceof ByteType) return IntConstant.v(0);
    if (t instanceof ShortType) return IntConstant.v(0);
    if (t instanceof IntType) return IntConstant.v(0);
    if (t instanceof FloatType) return FloatConstant.v(0);
    if (t instanceof LongType) return LongConstant.v(0);
    if (t instanceof DoubleType) return DoubleConstant.v(0);
    if (t instanceof BooleanType) return DIntConstant.v(0, BooleanType.v());
    // also for arrays etc.
    return NullConstant.v();
  }

  /**
   * Create the bodies of library methods.
   *
   * @throws IOException
   */
  public void createLibraryMethodBodies() throws IOException {
    for (SootClass libraryClass : getLibraryClasses()) {
      for (SootMethod method : libraryClass.getMethods()) {
        // Create our Jimple body for concrete methods only
        if (method.isConcrete()) {
          createJimpleBody(method);
          method.setPhantom(false);
        }
      }
      if (AverroesOptions.includeJavaLibraryClass()) {
        writeClassFile(Paths.libraryClassesOutputDirectory().getPath(), libraryClass);
      } else {
        if (!libraryClass.isJavaLibraryClass()) { // ingore java classes from rt.jar
          writeClassFile(Paths.libraryClassesOutputDirectory().getPath(), libraryClass);
        }
      }
    }
  }

  /**
   * Create the Jimple body for the given library method. If it's a constructor, then we need to
   * initialize all the fields in the class with objects compatible from the LPT. If it's the static
   * initializer, then we need to initialize all the static fields of the class with objects
   * compatible from the LPT. method.
   *
   * @param method
   * @return
   */
  private JimpleBody createJimpleBody(SootMethod method) {
    if (method.getDeclaringClass().getName().equals(Names.AVERROES_ABSTRACT_LIBRARY_CLASS)
        || method.getDeclaringClass().getName().equals(Names.AVERROES_LIBRARY_CLASS)) {
      throw new IllegalArgumentException(
          "Creating Jimple body for "
              + method.getSignature()
              + ". We should never enter createJimpleBody() for the Averroes library classes.");
    }

    // Create a basic Jimple body
    AverroesJimpleBody body = new AverroesJimpleBody(method);

    // Insert the appropriate method body
    if (body.isConstructor()) {
      body.initializeInstanceFields();
    } else if (body.isStaticInitializer()) {
      body.initializeStaticFields();
    } else {
      // the standard library method will have nothing more in its body
      body.assignParametersToReturnValuesField();
    }

    // Insert the standard Jimple body footer
    body.insertStandardJimpleBodyFooter();

    // Eliminate Nops
    NopEliminator.v().transform(body.getJimpleBody());

    // Validate the Jimple body
    body.validate();

    return (JimpleBody) method.getActiveBody();
  }

  /**
   * Generate the name of any class that Averroes creates.
   *
   * @param cls
   * @return
   */
  private String generateCraftedClassName(SootClass cls) {
    // return cls.getName();
    return cls.getName().concat("__Averroes");
  }

  /** Initialize the code generator by creating the concrete implementation classes. */
  private void initialize() {
    implementLibraryInterfacesNotImplementedInLibrary();
    implementAbstractLibraryClassesNotImplementedInLibrary();
  }

  /** Implement any library interface that is not implemented in the library. */
  private void implementLibraryInterfacesNotImplementedInLibrary() {
    for (SootClass iface : Hierarchy.v().getLibraryInterfacesNotImplementedInLibrary()) {
      SootClass cls = createLibraryClassImplementsInterface(iface);
      libraryInterfaceToConcreteImplementationClass.put(iface, cls);
    }
  }

  /**
   * Create a concrete library subclass for any abstract library class that is not implemented in
   * the library.
   */
  private void implementAbstractLibraryClassesNotImplementedInLibrary() {
    for (SootClass cls : Hierarchy.v().getAbstractLibraryClassesNotImplementedInLibrary()) {
      SootClass concrete = createConcreteLibrarySubclassFor(cls);
      abstractLibraryClassToConcreteImplementationClass.put(cls, concrete);
    }
  }

  /** Create the default constructor for the Averroes library class. */
  private void createAverroesAbstractLibraryInit() {
    SootMethod init = Hierarchy.getNewDefaultConstructor();

    JimpleBody body = Jimple.v().newBody(init);
    init.setActiveBody(body);
    averroesAbstractLibraryClass.addMethod(init);

    // Call superclass constructor
    body.insertIdentityStmts();
    addUnit(
        body,
        Jimple.v()
            .newInvokeStmt(
                Jimple.v()
                    .newSpecialInvokeExpr(
                        body.getThisLocal(),
                        Hierarchy.getDefaultConstructor(Hierarchy.v().getJavaLangObject())
                            .makeRef(),
                        Collections.emptyList())));

    // Add return statement
    addUnit(body, Jimple.v().newReturnVoidStmt());

    // Finally validate the Jimple body
    body.validate();
  }

  /**
   * Add a field to given Soot class.
   *
   * @param cls
   * @param fieldName
   * @param fieldType
   * @param modifiers
   * @return addedField
   */
  private SootField createField(SootClass cls, String fieldName, Type fieldType, int modifiers) {
    SootField field = new SootField(fieldName, fieldType, modifiers);
    if (cls.getFieldUnsafe(fieldName, fieldType) != null) {
      return cls.getField(fieldName, fieldType);
    }
    cls.addField(field);
    return field;
  }

  /** Add the main 2 fields to the AverroesAbstractLibrary class. */
  private void createAverroesAbstractLibraryFields() {
    createField(
        averroesAbstractLibraryClass,
        Names.FINALIZE_POINTS_TO,
        Hierarchy.v().getJavaLangObject().getType(),
        Modifier.PUBLIC);
    createField(
        averroesAbstractLibraryClass,
        Names.INSTANCE,
        averroesAbstractLibraryClass.getType(),
        Modifier.PUBLIC | Modifier.STATIC);
  }

  protected SootField createAverroesTypedLibraryPointsToField(RefLikeType fieldType) {
    return createField(
        averroesAbstractLibraryClass,
        Names.getTypedLibraryPointsToName(fieldType),
        fieldType,
        Modifier.PUBLIC);
  }

  /** Add the abstract doItAll method to the AverroesAbstractLibrary class. */
  private void createAverroesAbstractLibraryDoItAll() {
    SootMethod doItAll =
        new SootMethod(
            Names.AVERROES_DO_IT_ALL_METHOD_NAME,
            Collections.emptyList(),
            VoidType.v(),
            Modifier.PUBLIC | Modifier.ABSTRACT);
    averroesAbstractLibraryClass.addMethod(doItAll);
  }

  /** Create the static initializer for the Averroes library class. */
  private void createAverroesLibraryInit() {
    SootMethod init = Hierarchy.getNewDefaultConstructor();

    JimpleBody body = Jimple.v().newBody(init);
    init.setActiveBody(body);
    averroesLibraryClass.addMethod(init);

    // Call superclass constructor
    body.insertIdentityStmts();
    addUnit(
        body,
        Jimple.v()
            .newInvokeStmt(
                Jimple.v()
                    .newSpecialInvokeExpr(
                        body.getThisLocal(),
                        Hierarchy.getDefaultConstructor(averroesAbstractLibraryClass).makeRef(),
                        Collections.emptyList())));

    // Add return statement
    addUnit(body, Jimple.v().newReturnVoidStmt());

    // Eliminate Nops
    NopEliminator.v().transform(body);

    // Finally validate the Jimple body
    body.validate();
  }

  /** Create the default constructor for the Averroes library class. */
  private void createAverroesLibraryClinit() {
    SootMethod clinit =
        new SootMethod(
            SootMethod.staticInitializerName,
            Collections.emptyList(),
            VoidType.v(),
            Modifier.PUBLIC | Modifier.STATIC);

    clinit.setDeclaringClass(averroesLibraryClass);
    averroesLibraryClass.addMethod(clinit);
    AverroesJimpleBody body = new AverroesJimpleBody(clinit);

    // Create instance and call the constructor
    Local instance =
        body.insertSpecialInvokeNewStmt(
            RefType.v(averroesLibraryClass),
            averroesLibraryClass.getMethod(Names.DEFAULT_CONSTRUCTOR_SUBSIG));

    // Now assign this instance to AverroesAbstractLibrary.instance
    body.storeStaticField(CodeGenerator.v().getAverroesInstanceField(), instance, true);

    // Add return statement
    body.insertReturnStmt();

    // Eliminate Nops
    NopEliminator.v().transform(body.getJimpleBody());

    // System.out.println(clinitBody.getJimpleBody());

    // Finally validate the Jimple body
    body.validate();
  }

  /**
   * Create the doItAll method for the Averroes library class. It includes creating objects, calling
   * methods, writing to array elements, throwing exceptions and all the stuff that the library
   * could do.
   */
  private void createAverroesLibraryDoItAll() {
    SootMethod doItAll =
        new SootMethod(
            Names.AVERROES_DO_IT_ALL_METHOD_NAME,
            Collections.emptyList(),
            VoidType.v(),
            Modifier.PUBLIC);

    averroesLibraryClass.addMethod(doItAll);
    doItAllBody = new AverroesJimpleBody(doItAll);

    // Load the Averroes instance field
    doItAllBody.getInstance();

    // Insert object creation statements
    // createObjects();

    // Call finalize on all the objects in FPT
    // callFinalize();

    // Call all the application methods that the library could call
    // reflectively
    callApplicationMethods();

    // Handle array indices: cast lpt to object[] then assign it lpt
    // handleArrayIndices();

    // Create, reflectively, the application objects, and assign them to lpt
    if (AverroesOptions.isTamiflexEnabled()) {
      createObjectsFromApplicationClassNames();
    }

    // Now we need to throw all the exceptions the library has access to
    // (via lpt)
    // throwThrowables();

    // Add return statement
    // NOTE: We should ignore the return statement in this method as the
    // last statement will be the "throw"
    // statement, and the return type is void.
    doItAllBody.insertReturnStmt();

    // Eliminate Nops
    NopEliminator.v().transform(doItAllBody.getJimpleBody());

    // Finally validate the Jimple body
    doItAllBody.validate();
  }

  /**
   * Test if c is entry point class type.
   *
   * @param c
   * @return
   */
  private boolean isEntryPointSuperClass(SootClass c) {
    boolean isEntryPointClass = false;
    for (SootClass e : entryPointClasses.keySet()) {
      // FIXME. Spring entry point class always returns false
      if (!c.getName().startsWith("java."))
        isEntryPointClass = Hierarchy.v().isConcreteSubclassOf(e, c);
      if (isEntryPointClass) {
        return true;
      }
    }
    return isEntryPointClass;
  }

  private boolean skipClass(String classSignature) {
    if (classSignature.startsWith("java.") || classSignature.startsWith("android.support."))
      return true;
    return false;
  }
  /**
   * Call the application methods that the library could call, i.e. callbacks, calls via reflection.
   */
  private void callApplicationMethods() {
    Map<SootClass, Set<SootMethod>> allMethodsToCall = getAllMethodsToCallReflectively();
    NopStmt loop = doItAllBody.insertOuterLoopStartStmt();
    for (SootClass c : allMethodsToCall.keySet()) {
      if (skipClass(c.getName())) continue;
      boolean addGuard = false;
      addGuard = isEntryPointSuperClass(c);
      NopStmt outerIfStmt = null;
      if (addGuard) {
        doItAllBody.insertRandomAssignment();
        outerIfStmt = doItAllBody.insertGuardCondition();
      }
      NopStmt outerLoopStartStmt = null;
      // if the class is an entryp point class type such as an Activity, add a loop
      if (addGuard) {
        outerLoopStartStmt = doItAllBody.insertOuterLoopStartStmt();
      }
      // Prepare the method base
      Local base = (Local) doItAllBody.getCompatibleValue(c.getType());
      boolean alreadyAdded = true;
      for (SootMethod toCall : allMethodsToCall.get(c)) {
        SootMethodRef methodRef = toCall.makeRef();
        if (!alreadyAdded) {
          doItAllBody.AddAssignToLPT(base);
        }
        alreadyAdded = false;
        // prepare actual args
        List<Value> args = doItAllBody.prepareActualArguments(toCall);
        InvokeExpr invokeExpr;
        // Call the method
        if (c.isInterface()) {
          invokeExpr = Jimple.v().newInterfaceInvokeExpr(base, methodRef, args);
        } else if (toCall.isStatic()) {
          invokeExpr = Jimple.v().newStaticInvokeExpr(methodRef, args);
        } else {
          invokeExpr = Jimple.v().newVirtualInvokeExpr(base, methodRef, args);
        }

        // Assign the return of the call to the return variable only if it
        // holds an object.
        // If not, then just call the method.
        doItAllBody.insertRandomAssignment();
        if (toCall.getReturnType() instanceof RefLikeType) {
          Local ret = doItAllBody.newLocal(toCall.getReturnType());
          doItAllBody.getInvokeReturnVariables().add(ret);
          doItAllBody.insertAssignmentStatement(ret, invokeExpr, true);
        } else {
          doItAllBody.insertInvokeStatement(invokeExpr, true);
        }
      }
      if (addGuard) {
        doItAllBody.finishLoop(outerLoopStartStmt);
        doItAllBody.getJimpleBody().getUnits().add(outerIfStmt);
      }
    }
    doItAllBody.finishLoop(loop);
    // Assign the return values from all those methods only if there were
    // any return variables of type RefLikeType
    for (Local ret : doItAllBody.getInvokeReturnVariables()) {
      if (ret.getType() instanceof RefLikeType)
        doItAllBody.storeTypedLibraryPointsToField(ret, (RefLikeType) ret.getType());
    }
  }

  /**
   * Retrieve all the methods that the library could call back through reflection.
   *
   * @return
   */
  public Map<SootClass, Set<SootMethod>> getAllMethodsToCallReflectively() {
    LinkedHashSet<SootMethod> result = new LinkedHashSet<SootMethod>();
    result.addAll(Hierarchy.v().getLibrarySuperMethodsOfApplicationMethods());
    result.addAll(getTamiFlexApplicationMethodInvokes());
    result.addAll(Hierarchy.v().getAnnotatedApplicationMethods());
    // Get those methods specified in the apk resource xml files that handle
    // onClick events.
    // if (Options.v().src_prec() == Options.src_prec_apk) {
    // result.addAll(Cleanup.v().getOnClickApplicationMethods());
    // }

    Map<SootClass, Set<SootMethod>> ret = new HashMap<>();
    for (SootMethod m : result) {
      SootClass c = m.getDeclaringClass();
      if (!ret.containsKey(c)) ret.put(c, new HashSet<>());
      ret.get(c).add(m);
    }
    return ret;
  }

  /** Create objects for application classes if the library knows their name constants. */
  private void createObjectsFromApplicationClassNames() {
    SootMethod forName = Hierarchy.v().getMethod(Names.FOR_NAME_SIG);
    SootMethod newInstance = Hierarchy.v().getMethod(Names.NEW_INSTANCE_SIG);
    List<Value> args = doItAllBody.prepareActualArguments(forName);
    Local classes = doItAllBody.newLocal(Hierarchy.v().getJavaLangClass().getType());
    Local instances = doItAllBody.newLocal(Hierarchy.v().getJavaLangObject().getType());

    AssignStmt classForName =
        Jimple.v().newAssignStmt(classes, Jimple.v().newStaticInvokeExpr(forName.makeRef(), args));
    AssignStmt classNewInstance =
        Jimple.v()
            .newAssignStmt(
                instances, Jimple.v().newVirtualInvokeExpr(classes, newInstance.makeRef()));

    doItAllBody.insertAndGuardAssignStmts(classForName, classNewInstance);

    // doItAllBody.insertAssignmentStatement(classes,
    // Jimple.v().newStaticInvokeExpr(forName.makeRef(), args));
    // doItAllBody.insertAssignmentStatement(instances,
    // Jimple.v().newVirtualInvokeExpr(classes,
    // newInstance.makeRef()));
    // doItAllBody.storeLibraryPointsToField(instances);
  }

  /**
   * Throw any throwable object pointed to by the LPT. NOTE: the throw statement has to be right
   * before the return statement of the method, otherwise the method is invalid (Soot).
   */
  private void throwThrowables() {
    Local throwables =
        (Local) doItAllBody.getCompatibleValue(Hierarchy.v().getJavaLangThrowable().getType());
    doItAllBody.insertThrowStatement(throwables);
  }

  /** Create all the objects that the library could possible instantiate. */
  private void createObjects() {
    // 1. The library can point to any concrete (i.e., not an interface nor
    // abstract) library class
    for (SootClass cls : getConcreteLibraryClasses()) {
      doItAllBody.createObjectOfType(cls);
    }

    // 2. Convert any use of application class name string constants to
    // explicit instantiations.
    for (SootClass cls : Hierarchy.v().getApplicationClasses()) {
      if (!Hierarchy.isAbstractClass(cls) && !cls.isInterface()) {
        doItAllBody.createObjectOfType(cls);
      }
    }

    // 3. The library can create application objects through
    // Class.newInstance
    if (AverroesOptions.isTamiflexEnabled()) {
      for (SootClass cls : getTamiFlexApplicationClassNewInstance()) {
        doItAllBody.createObjectOfType(cls);
      }
    }

    // 4. The library can create application objects through
    // Constructor.newInstance
    if (AverroesOptions.isTamiflexEnabled()) {
      for (SootMethod init : getTamiFlexApplicationConstructorNewInstance()) {
        doItAllBody.createObjectByCallingConstructor(init);
      }
    }

    // 5. The library points to some certain objects of array types
    for (ArrayType type : getArrayTypesAccessibleToLibrary()) {
      doItAllBody.createObjectOfType(type);
    }

    // 6. The library could possibly create application objects whose class
    // names are passed to it through
    // calls to Class.forName
    if (AverroesOptions.isTamiflexEnabled()) {
      for (SootClass cls : getTamiFlexApplicationClassForName()) {
        doItAllBody.createObjectOfType(cls);
      }
    }

    // 7. Create instances of dynamic classes
    if (AverroesOptions.isDynamicClassesEnabled()) {
      try {
        for (String className : AverroesOptions.getDynamicApplicationClasses()) {
          doItAllBody.createObjectOfType(Hierarchy.v().getClass(className));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Get a set of all the array types accessible to the library.
   *
   * @return
   */
  private Set<ArrayType> getArrayTypesAccessibleToLibrary() {
    Set<ArrayType> result = new HashSet<ArrayType>();
    result.addAll(Hierarchy.v().getLibraryArrayTypeParameters());
    result.addAll(Hierarchy.v().getLibraryArrayTypeReturns());

    // Only added if reflection support is enabled
    if (AverroesOptions.isTamiflexEnabled()) {
      result.addAll(getTamiFlexApplicationArrayNewInstance());
    }

    return result;
  }

  /**
   * Get a set of all the concrete library classes. This include the original concrete library
   * classes, in addition to the concrete implementation classes generated by this code generator.
   *
   * @return
   */
  private Set<SootClass> getConcreteLibraryClasses() {
    Set<SootClass> result = new HashSet<SootClass>();
    result.addAll(Hierarchy.v().getConcreteLibraryClasses());
    result.addAll(abstractLibraryClassToConcreteImplementationClass.values());
    result.addAll(libraryInterfaceToConcreteImplementationClass.values());
    return result;
  }

  /**
   * Get a set of all the library classes. This include the original library classes, in addition to
   * the concrete implementation classes generated by this code generator.
   *
   * @return
   */
  private Set<SootClass> getLibraryClasses() {
    Set<SootClass> result = new HashSet<SootClass>();
    result.addAll(Hierarchy.v().getLibraryClasses());
    result.addAll(abstractLibraryClassToConcreteImplementationClass.values());
    result.addAll(libraryInterfaceToConcreteImplementationClass.values());
    return result;
  }

  /**
   * Find all the application methods that TamiFlex found out they could be called reflectively.
   *
   * @return
   */
  private Set<SootMethod> getTamiFlexApplicationMethodInvokes() {
    Set<SootMethod> result = new HashSet<SootMethod>();

    for (String methodSignature : TamiFlexFactsDatabase.getMethodInvoke()) {
      if (Hierarchy.v().isApplicationMethod(methodSignature)) {
        result.add(Hierarchy.v().getMethod(methodSignature));
      }
    }

    return result;
  }

  /**
   * Find all the application classes that might be reflectively created through Class.forName.
   *
   * @return
   */
  private Set<SootClass> getTamiFlexApplicationClassForName() {
    Set<SootClass> result = new HashSet<SootClass>();

    for (String className : TamiFlexFactsDatabase.getClassForName()) {
      if (Hierarchy.v().isApplicationClass(className)) {
        result.add(Hierarchy.v().getClass(className));
      }
    }

    return result;
  }

  /**
   * Get all the application array types that the library can create objects for.
   *
   * @return
   */
  private Set<ArrayType> getTamiFlexApplicationArrayNewInstance() {
    Set<ArrayType> result = new HashSet<ArrayType>();

    for (String arrayType : TamiFlexFactsDatabase.getArrayNewInstance()) {
      String baseType = Hierarchy.getBaseType(arrayType);
      if (Hierarchy.v().isApplicationClass(baseType)) {
        result.add(Hierarchy.v().getArrayType(arrayType));
      }
    }

    return result;
  }

  /**
   * Find all the application classes that could be reflectively created through Class.newInstance.
   *
   * @return
   */
  private Set<SootClass> getTamiFlexApplicationClassNewInstance() {
    Set<SootClass> result = new HashSet<SootClass>();

    for (String className : TamiFlexFactsDatabase.getClassNewInstance()) {
      if (Hierarchy.v().isApplicationClass(className)) {
        result.add(Hierarchy.v().getClass(className));
      }
    }

    return result;
  }

  /**
   * Find all the constructors that could be reflectively used to create application classes through
   * Constructor.newInstance.
   *
   * @return
   */
  private Set<SootMethod> getTamiFlexApplicationConstructorNewInstance() {
    Set<SootMethod> result = new HashSet<SootMethod>();

    for (String methodSignature : TamiFlexFactsDatabase.getConstructorNewInstance()) {
      if (Hierarchy.v().isApplicationMethod(methodSignature)) {
        result.add(Hierarchy.v().getMethod(methodSignature));
      }
    }

    return result;
  }

  /**
   * Create a class that implements the given abstract class.
   *
   * @param abstractClass
   * @return
   */
  private SootClass createConcreteLibrarySubclassFor(SootClass abstractClass) {
    SootClass cls = new SootClass(generateCraftedClassName(abstractClass));
    // Set the superclass
    cls.setSuperclass(abstractClass);

    // Add the class to the Soot scene
    // Scene.v().addClass(cls);

    // Implement all the abstract methods in the super class.
    // The bodies of these methods will be handled later.
    for (SootMethod method : abstractClass.getMethods()) {
      if (method.isAbstract()) {
        addMethodToGeneratedClass(cls, getConcreteMethod(method));
      }
    }

    // Now add a default constructor
    addDefaultConstructorToGeneratedClass(cls);

    // Set the resolving level to SIGNATURES and set this class to be a
    // library class.
    // cls.setResolvingLevel(SootClass.SIGNATURES);
    // cls.setLibraryClass();

    // Update the generated class count
    generatedClassCount++;

    return cls;
  }

  /**
   * Generate the name of any interface that GenCG creates.
   *
   * @param cls
   * @return
   */
  private String generateCraftedInterfaceName(SootClass cls) {
    // return cls.getName();
    return cls.getName().concat("_GenCG");
  }

  private boolean isCraftedInterface(SootClass cls) {
    return cls.getName().endsWith("_GenCG");
  }

  private SootClass createCraftedInterface(
      SootClass entryPointClass, List<SootMethod> entryPointMethods) throws IOException {
    SootClass iface = new SootClass(generateCraftedInterfaceName(entryPointClass));
    iface.setModifiers(Modifier.PUBLIC | Modifier.INTERFACE);
    SootMethod constructor = Hierarchy.getNewDefaultConstructor();
    iface.addMethod(constructor);
    for (SootMethod e : entryPointMethods) {
      addMethodToGeneratedClass(
          iface,
          new SootMethod(
              e.getName(),
              e.getParameterTypes(),
              e.getReturnType(),
              e.getModifiers(),
              e.getExceptions()));
    }
    entryPointClass.addInterface(iface);
    Hierarchy.v().addGeneratedInterface(entryPointClass, iface);
    return iface;
  }

  /**
   * Create a class that implements the given interface.
   *
   * @param iface
   * @return
   */
  private SootClass createLibraryClassImplementsInterface(SootClass iface) {
    SootClass cls = new SootClass(generateCraftedClassName(iface));

    // java.lang.Object is the superclass of all classes
    cls.setSuperclass(Hierarchy.v().getJavaLangObject());

    // Add the class to the Soot scene
    // Scene.v().addClass(cls);

    // Make the given Soot interface a direct superinterface of the crafted
    // class.
    cls.addInterface(iface);

    // Now we need to implement the methods in all the superinterfaces of
    // the newly crafted class.
    // The body of these methods will be created later.
    for (SootMethod method : getSuperinterfacesMethods(iface)) {
      addMethodToGeneratedClass(cls, getConcreteMethod(method));
    }

    // Now add a default constructor.
    addDefaultConstructorToGeneratedClass(cls);

    // Set the resolving level to SIGNATURES and set this class to be a
    // library class.
    // cls.setResolvingLevel(SootClass.SIGNATURES);
    // cls.setLibraryClass();

    // Update the generated class count
    generatedClassCount++;

    return cls;
  }

  /**
   * Get all the non-repeated methods in the superinterfaces (i.e., non-repeated as in don't have
   * the same subsignature.
   *
   * @param iface
   * @return
   */
  private Collection<SootMethod> getSuperinterfacesMethods(SootClass iface) {
    Map<String, SootMethod> methods = new HashMap<String, SootMethod>();

    for (SootClass superInterface : Hierarchy.v().getSuperinterfacesOfIncluding(iface)) {
      for (SootMethod method : superInterface.getMethods()) {
        if (!methods.containsKey(method.getSubSignature())) {
          methods.put(method.getSubSignature(), method);
        }
      }
    }

    return methods.values();
  }

  /**
   * Get the concrete version of an abstract method. This way Averroes will create a method body for
   * it. This is important for implementing interface methods, and extending abstract classes.
   *
   * @param method
   * @return
   */
  private SootMethod getConcreteMethod(SootMethod method) {
    return new SootMethod(
        method.getName(),
        method.getParameterTypes(),
        method.getReturnType(),
        method.getModifiers() & ~Modifier.ABSTRACT,
        method.getExceptions());
  }

  /**
   * Add a method to a generated class and update the generated method count.
   *
   * @param generatedClass
   * @param method
   */
  private void addMethodToGeneratedClass(SootClass generatedClass, SootMethod method) {
    generatedClass.addMethod(method);

    // Update the generated method count
    generatedMethodCount++;
  }

  /**
   * Add a default constructor to the given generated class.
   *
   * @param generatedClass
   */
  private void addDefaultConstructorToGeneratedClass(SootClass generatedClass) {
    if (!generatedClass.isInterface()) {
      if (Hierarchy.hasDefaultConstructor(generatedClass)) {
        Hierarchy.makePublic(generatedClass.getMethod(Names.DEFAULT_CONSTRUCTOR_SUBSIG));
      } else {
        addMethodToGeneratedClass(generatedClass, Hierarchy.getNewDefaultConstructor());
      }
    }
  }

  /**
   * Create crafted interface of each annotated entry point class
   *
   * @param entryPointClasses
   * @throws IOException
   */
  public void createCraftedInterfacesOfEntryPointClasses(
      Map<SootClass, SootClass> entryPointClasses, EntryPointConfigurationReader reader)
      throws IOException {
    this.entryPointClasses = entryPointClasses;
    AnnotationEntryPointMethodDetector mdetector = null;
    switch (AverroesOptions.getFrameworkType()) {
      case FrameworkType.SPRING:
        mdetector =
            new SpringEntryPointMethodDetector(reader.getEntryPointMethods(FrameworkType.SPRING));
        break;
      default:
        return;
    }
    for (SootClass c : entryPointClasses.keySet()) {
      EntryPointTypeTag t = (EntryPointTypeTag) c.getTag("EntryPointTypeTag");
      if (t.hasTypeOfAnnotation()) {
        List<SootMethod> eMethods = mdetector.getEntryPointMethods(c);
        SootClass iface = createCraftedInterface(c, eMethods);
        instrumentedInterfaces.put(c, iface);
        replaceInvokeToSuperclassConstructor(c, iface);
        entryPointClasses.replace(c, iface);
      }
    }
  }

  private void replaceInvokeToSuperclassConstructor(SootClass c, SootClass iface) {
    SootMethod constructor = c.getMethodUnsafe(Names.DEFAULT_CONSTRUCTOR_SUBSIG);
    if (constructor == null) return;
    UnitPatchingChain units = constructor.getActiveBody().getUnits();
    Iterator<Unit> it = units.snapshotIterator();
    List<Unit> toInsert = new ArrayList<>();
    SootMethodRef methodRef =
        Scene.v()
            .makeConstructorRef(
                iface, iface.getMethodUnsafe(Names.DEFAULT_CONSTRUCTOR_SUBSIG).getParameterTypes());
    Unit insert = Baf.v().newSpecialInvokeInst(methodRef);
    toInsert.add(insert);
    Unit toRemove = null;
    while (it.hasNext()) {
      Unit u = it.next();
      if (u instanceof SpecialInvokeInst) {

        units.insertBefore(toInsert, u);
        toRemove = u;
        break;
      }
    }
    if (toRemove != null) units.remove(toRemove);
  }

  public void replaceBeanRetrieval() {
    for (SootClass cl : Scene.v().getApplicationClasses()) {
      for (SootMethod m : cl.getMethods()) {
        if (!m.hasActiveBody()) continue;
        UnitPatchingChain units = m.getActiveBody().getUnits();
        Iterator<Unit> iter = units.snapshotIterator();
        while (iter.hasNext()) {
          Unit unit = iter.next();
          unit.apply(
              new AbstractStmtSwitch() {
                public void caseAssignStmt(AssignStmt stmt) {
                  if (stmt.containsInvokeExpr() && isBeanRetrieval(stmt)) {
                    units.remove(stmt);
                    Unit nextUnit;
                    if (iter.hasNext()) {
                      nextUnit = iter.next();
                      nextUnit.apply(
                          new AbstractStmtSwitch() {
                            public void caseAssignStmt(AssignStmt s) {
                              Value left = s.getLeftOp();
                              if (left.getType() instanceof RefLikeType) {
                                SootField instance = CodeGenerator.v().getAverroesInstanceField();
                                Value field = Jimple.v().newStaticFieldRef(instance.makeRef());
                                Local local =
                                    soot.jimple.Jimple.v()
                                        .newLocal("averroesLib", instance.getType());
                                m.getActiveBody().getLocals().add(local);
                                List<Unit> toInsert = new ArrayList();
                                toInsert.add(Jimple.v().newAssignStmt(local, field));
                                RefLikeType leftType = (RefLikeType) left.getType();
                                SootClass cls = Scene.v().getSootClassUnsafe(leftType.toString());
                                if (cls != null) {
                                  SootClass iface = instrumentedInterfaces.get(cls);
                                  if (iface == null) iface = cls;
                                  InstanceFieldRef right =
                                      Jimple.v()
                                          .newInstanceFieldRef(
                                              local,
                                              CodeGenerator.v()
                                                  .createAverroesTypedLibraryPointsToField(
                                                      (RefLikeType) iface.getType())
                                                  .makeRef());
                                  Local temp =
                                      soot.jimple.Jimple.v().newLocal("temp", right.getType());
                                  m.getActiveBody().getLocals().add(temp);
                                  toInsert.add(Jimple.v().newAssignStmt(temp, right));
                                  toInsert.add(Jimple.v().newAssignStmt(left, temp));
                                  units.insertBefore(toInsert, s);
                                }
                                units.remove(s);
                                instrumentedClasses.add(cl.getName());
                                System.out.println("Instrumenting " + cl.getName());
                              }
                            }
                          });
                    }
                  }
                }

                private boolean isBeanRetrieval(AssignStmt stmt) {
                  String name = stmt.getInvokeExpr().getMethodRef().getName();
                  return name.equals("getBean") || name.equals("getInstance");
                }
              });
        }
      }
    }
  }

  public void setObjectProviders(Map<SootClass, Set<SootMethod>> objectProviders) {
    this.objectProviders = objectProviders;
  }
}
