package module.evolview.pathwaybrowser.io;

public final class PathwayExampleDirectoryTest {
    public static void main(String[] args) {
        String fallback = "/tmp";
        require(ParamsAssignerAndParser4pathwayFamBrowser.resolvePropertiesDir(null, fallback).equals(fallback),
                "null directory falls back");
        require(ParamsAssignerAndParser4pathwayFamBrowser.resolvePropertiesDir("  ", fallback).equals(fallback),
                "blank directory falls back");
        require(ParamsAssignerAndParser4pathwayFamBrowser.resolvePropertiesDir("null", fallback).equals(fallback),
                "literal null directory falls back");
        require(ParamsAssignerAndParser4pathwayFamBrowser
                        .resolvePropertiesDir("/path/that/does/not/exist", fallback).equals(fallback),
                "missing directory falls back");
        require(ParamsAssignerAndParser4pathwayFamBrowser.resolvePropertiesDir("/", fallback).equals("/"),
                "valid configured directory is preserved");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
