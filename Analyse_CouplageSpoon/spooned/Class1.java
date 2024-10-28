/* Class1.java */
public class Class1 {
    private Class2 c2;

    private Class3 c3;

    private Class3 c3bis;

    public Class1() {
        c2 = new Class2();
        c3 = new Class3();
        c3bis = new Class3();
    }

    public void method1() {
        c2.method2();
        c3.method3();
    }

    public void method1bis() {
        c3bis.method3();
    }
}