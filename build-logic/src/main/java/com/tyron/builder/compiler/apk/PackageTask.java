package com.tyron.builder.compiler.apk;

import android.net.Uri;

import com.android.sdklib.build.ApkBuilder;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.SealedApkException;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.Project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PackageTask extends Task {

    /**
     * List of extra dex files not including the main dex file
     */
    private final List<File> mDexFiles = new ArrayList<>();
    /**
     * List of each jar files of libraries
     */
    private final List<File> mLibraries = new ArrayList<>();
    /**
     * Main dex file
     */
    private File mDexFile;
    /**
     * The generated.apk.res file
     */
    private File mGeneratedRes;
    /**
     * The output apk file
     */
    private File mApk;

    private Project mProject;
    private ILogger mLogger;
    private BuildType mBuildType;

    @Override
    public String getName() {
        return "Package";
    }

    @Override
    public void prepare(Project project, ILogger logger, BuildType type) throws IOException {
        mProject = project;
        mLogger = logger;
        mBuildType = type;

        File mBinDir = new File(project.getBuildDirectory(), "bin");

        mApk = new File(mBinDir, "generated.apk");
        mDexFile = new File(mBinDir, "classes.dex");
        mGeneratedRes = new File(mBinDir, "generated.apk.res");
        File[] binFiles = mBinDir.listFiles();
        if (binFiles != null) {
            for (File child : binFiles) {
                if (!child.isFile()) {
                    continue;
                }
                if (!child.getName().equals("classes.dex") && child.getName().endsWith(".dex")) {
                    mDexFiles.add(child);
                }
            }
        }

        mLibraries.addAll(project.getLibraries());

        logger.debug("Packaging APK.");
    }

    @Override
    public void run() throws IOException, CompilationFailedException {

        int dexCount = 1;
        try {
            ApkBuilder builder = new ApkBuilder(
                    mApk.getAbsolutePath(),
                    mGeneratedRes.getAbsolutePath(),
                    mDexFile.getAbsolutePath(),
                    null,
                    null);

            for (File extraDex : mDexFiles) {
                dexCount++;
                builder.addFile(extraDex, Uri.parse(extraDex.getAbsolutePath()).getLastPathSegment());
            }

            for (File library : mLibraries) {
                builder.addResourcesFromJar(library);
            }

            if (mProject.getNativeLibsDirectory().exists()) {
                builder.addNativeLibraries(mProject.getNativeLibsDirectory());
            }

            if (mBuildType == BuildType.DEBUG) {
                for (File it : mProject.getLibraries()) {
                    File parent = it.getParentFile();
                    if (parent != null) {
                        File[] dexFiles = parent.listFiles(c -> c.getName().endsWith(".dex"));
                        if (dexFiles != null) {
                            for (File dexFile : dexFiles) {
                                dexCount++;
                                builder.addFile(dexFile, "classes" + dexCount + ".dex");
                            }
                        }
                    }
                }
            }

            builder.setDebugMode(true);
            builder.sealApk();
        } catch (ApkCreationException | SealedApkException | DuplicateFileException e) {
            throw new CompilationFailedException(e);
        }
    }
}
