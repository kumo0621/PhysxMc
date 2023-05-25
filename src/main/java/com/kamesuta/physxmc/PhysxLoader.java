package com.kamesuta.physxmc;

import sun.misc.Unsafe;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;

public class PhysxLoader {
    /** プラグインのクラスローダー */
    private static final ClassLoader pluginClassLoader = PhysxLoader.class.getClassLoader();

    /** アプリケーションクラスローダー */
    private static final ClassLoader appClassLoader = pluginClassLoader.getParent();

    /** ライブラリのバージョン */
    private static final String physxJniVersion = "2.0.6";

    static MethodHandles.Lookup lookup;
    static Unsafe unsafe;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object lookupBase = unsafe.staticFieldBase(lookupField);
            long lookupOffset = unsafe.staticFieldOffset(lookupField);
            lookup = (MethodHandles.Lookup) unsafe.getObject(lookupBase, lookupOffset);
        } catch (Throwable ignore) {
        }
    }
    
    /**
     * PhysXライブラリをアプリケーションクラスローダーにロードします。
     * プラグインローダーでロードするとプラグインのリロードに失敗するため、アプリケーションクラスローダーに直接ロードします。
     */
    public static void loadPhysxOnAppClassloader() throws Throwable {
        // 全体のクラスローダーに登録
        boolean forceCopy = "true".equalsIgnoreCase(System.getProperty("physx.forceCopyLibs", "false"));
        File libFile = copyLibsFromResources(forceCopy);
        
        Field ucp = ucp(appClassLoader.getClass());
        addURL(appClassLoader, ucp, libFile);
        
        // 全体のクラスローダーで読み込む
        Class<?> loaderClass = appClassLoader.loadClass("de.fabmax.physxjni.Loader");
        Method loadMethod = loaderClass.getMethod("load");
        loadMethod.invoke(null);
    }

    private static void addURL(ClassLoader loader, Field ucpField, File file) throws Throwable {
        if (ucpField == null) {
            throw new IllegalStateException("ucp field not found");
        }
        Object ucp = unsafe.getObject(loader, unsafe.objectFieldOffset(ucpField));
        try {
            MethodHandle methodHandle = lookup.findVirtual(ucp.getClass(), "addURL", MethodType.methodType(void.class, URL.class));
            methodHandle.invoke(ucp, file.toURI().toURL());
        } catch (NoSuchMethodError e) {
            throw new IllegalStateException("Unsupported (classloader: " + loader.getClass().getName() + ", ucp: " + ucp.getClass().getName() + ")", e);
        }
    }

    private static Field ucp(Class<?> loader) {
        try {
            return loader.getDeclaredField("ucp");
        } catch (NoSuchFieldError | NoSuchFieldException e2) {
            Class<?> superclass = loader.getSuperclass();
            if (superclass == Object.class) {
                return null;
            }
            return ucp(superclass);
        }
    }

    /**
     * リソースからライブラリをコピーします。
     * 展開先は `${java.io.tmpdir}/com.quarri6343.physx-jni/${physxJniVersion}` になります。
     */
    private static File copyLibsFromResources(boolean forceCopy) throws Exception {
        File tempLibDir = new File(System.getProperty("java.io.tmpdir"), "de.fabmax.physx-jni" + File.separator + physxJniVersion);
        if (tempLibDir.exists() && !tempLibDir.isDirectory() || !tempLibDir.exists() && !tempLibDir.mkdirs()) {
            throw new IllegalStateException("Failed creating native lib dir " + tempLibDir);
        }

        // 1st: make sure all libs are available in system temp dir
        InputStream libIn = pluginClassLoader.getResourceAsStream("libs.zip");
        if (libIn == null) {
            throw new IllegalStateException("Failed loading libs.zip from resources");
        }
        File libTmpFile = new File(tempLibDir, "libs.jar");
        if (forceCopy && libTmpFile.exists()) {
            if (!libTmpFile.delete()) {
                throw new IllegalStateException("Failed deleting existing native lib file " + libTmpFile);
            }
        }
        if (!libTmpFile.exists()) {
            Files.copy(libIn, libTmpFile.toPath());
        }
        return libTmpFile;
    }
}