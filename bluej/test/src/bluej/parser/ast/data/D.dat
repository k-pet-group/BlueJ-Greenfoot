class D {
  int f;
}
class C {
  D d=new D();
  C(int x) { this(); }
  D getD() { return d; }
  C x = this;
}
class B {
  class H1 extends C {
    H1() {
      super();
    }
    C x = super;
  }
  int getC() { return new C().getD().f; }
}
class A0 {
  void x() {
    new B().getC().f;
  }
  class subH1 extends B.H1 {
    subH1() {
      new B().super();
    }
    subH1(B enclosingInstance) {
      enclosingInstance.super();
      enclosingInstance.getC();
    }
  }
}
