package test.api.rpython;

import api.rpython.EvolTreeManipulator;
import api.rpython.API4R;
import api.rpython.ModernTreeViewPyLauncher;
import api.rpython.RlangInterfaceEGPS;
import api.rpython.TestJFrame;

public class ExternalLanguageApiStructureTest {

    public static void main(String[] args) {
        assertClassName(API4R.class, "api.rpython.API4R");
        assertClassName(RlangInterfaceEGPS.class, "api.rpython.RlangInterfaceEGPS");
        assertClassName(ModernTreeViewPyLauncher.class, "api.rpython.ModernTreeViewPyLauncher");
        assertClassName(EvolTreeManipulator.class, "api.rpython.EvolTreeManipulator");
        assertClassName(TestJFrame.class, "api.rpython.TestJFrame");

        assertSuperclass(API4R.class, EvolTreeManipulator.class);
        assertMethodExists(API4R.class, "getNodeNames", String.class, String.class, boolean.class, boolean.class);
        assertMethodExists(API4R.class, "describe");
        assertMethodExists(API4R.class, "getString");
        assertMethodExists(RlangInterfaceEGPS.class, "launchDesktop");
        assertMethodExists(RlangInterfaceEGPS.class, "showPayloadAndReturnLength", String.class);
        assertMethodExists(RlangInterfaceEGPS.class, "openModernTreeView", String.class);
        assertMethodExists(RlangInterfaceEGPS.class, "launch");
        assertMethodExists(RlangInterfaceEGPS.class, "callTest", String.class);
        assertMethodExists(RlangInterfaceEGPS.class, "modernTreeView", String.class);
        assertMethodExists(ModernTreeViewPyLauncher.class, "launchFromConfigFile", String.class);
        assertMethodExists(EvolTreeManipulator.class, "extractNodeNames", String.class, String.class, boolean.class, boolean.class);
        assertMethodExists(EvolTreeManipulator.class, "describe");
        assertMethodExists(TestJFrame.class, "showDemoWindow", String.class);
        assertMethodExists(TestJFrame.class, "renderDemoImageAsPng", int.class, int.class);
    }

    private static void assertClassName(Class<?> clazz, String expectedName) {
        if (!expectedName.equals(clazz.getName())) {
            throw new AssertionError("Expected class name " + expectedName + " but got " + clazz.getName());
        }
    }

    private static void assertSuperclass(Class<?> clazz, Class<?> expectedSuperclass) {
        if (!expectedSuperclass.equals(clazz.getSuperclass())) {
            throw new AssertionError("Expected superclass " + expectedSuperclass.getName() + " but got "
                    + clazz.getSuperclass().getName());
        }
    }

    private static void assertMethodExists(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Missing method " + methodName + " on " + clazz.getName(), e);
        }
    }
}
