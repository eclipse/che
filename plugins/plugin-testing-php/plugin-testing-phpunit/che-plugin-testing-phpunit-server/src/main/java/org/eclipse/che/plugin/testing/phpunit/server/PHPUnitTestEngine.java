/*******************************************************************************
 * Copyright (c) 2016 Rogue Wave Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Rogue Wave Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.testing.phpunit.server;

import static org.eclipse.che.plugin.testing.phpunit.server.PHPUnitTestRunner.LOG;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.io.FileUtils;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.AbstractLineConsumer;
import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.che.api.testing.server.exceptions.TestFrameworkException;
import org.eclipse.che.api.testing.shared.dto.TestResultDto;
import org.eclipse.che.api.testing.shared.dto.TestResultRootDto;
import org.eclipse.che.api.vfs.Path;
import org.eclipse.che.plugin.testing.phpunit.server.model.PHPUnitTestRoot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

/**
 * PHPUnit tests running engine.
 * 
 * @author Bartlomiej Laczkowski
 */
public class PHPUnitTestEngine {

    private final class PrinterListener implements Runnable {

        private ServerSocket serverSocket;
        private Socket socket;
        private Gson gson = new GsonBuilder().create();
        private ExecutorService threadExecutor;

        public PrinterListener() {
            threadExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    final Thread thread = new Thread(r, "PHPUnitPrinterListener");
                    thread.setDaemon(true);
                    return thread;
                }
            });
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PRINTER_PORT, 1);
                serverSocket.setSoTimeout(3000);
                serverSocket.setReuseAddress(true);
                // Release engine to perform tests
                latchReady.countDown();
                socket = serverSocket.accept();
                handleReport(socket);
            } catch (final IOException e) {
                Thread.currentThread().interrupt();
                LOG.error(e.getMessage(), e);
            } finally {
                shutdown();
            }
        }

        void shutdown() {
            try {
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (final Exception e) {
            }
            try {
                if (serverSocket != null && !serverSocket.isClosed())
                    serverSocket.close();
            } catch (final IOException e) {
            }
            threadExecutor.shutdown();
        }

        void startup() {
            threadExecutor.submit(this);
        }

        @SuppressWarnings("unchecked")
        private void handleReport(final Socket socket) {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final PHPUnitMessageParser messageParser = new PHPUnitMessageParser(phpTestsRoot);
                String line;
                Map<String, String> value = null;
                while ((line = reader.readLine()) != null) {
                    try {
                        value = gson.fromJson(line, LinkedTreeMap.class);
                        messageParser.parse(value);
                    } catch (final Throwable e) {
                        value = null;
                    }
                }
                latchDone.countDown();
                shutdown();
            } catch (final IOException e) {
                Thread.currentThread().interrupt();
                shutdown();
            }
        }
    }

    private static final String PRINTER_NAME_V5X = "PHPUnitLogger5x";
    private static final String PRINTER_NAME_V6X = "PHPUnitLogger6x";
    private static final String PRINTER_DIRECTORY = "phpunit-printer";
    private static final String PHPUNIT_GLOBAL = "phpunit";
    private static final String PHPUNIT_COMPOSER = "/vendor/bin/phpunit";
    private static final int PRINTER_PORT = 7478;

    private final ProjectManager projectManager;
    private final CountDownLatch latchReady = new CountDownLatch(1);
    private final CountDownLatch latchDone = new CountDownLatch(1);

    private PHPUnitTestRoot phpTestsRoot;
    private PHPUnitTestResultsProvider testResultsProvider;

    public PHPUnitTestEngine(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    /**
     * Executes PHP unit tests with the use of provided parameters.
     * 
     * @param testParameters
     * @return
     * @throws Exception
     */
    public TestResultRootDto executeTests(Map<String, String> testParameters) throws Exception {
        String projectPath = testParameters.get("projectPath");
        String projectAbsolutePath = testParameters.get("absoluteProjectPath");
        String testTarget = testParameters.get("testTarget");
        Path testTargetAbsolutePath;
        if (Path.of(testTarget).length() > 1)
            testTargetAbsolutePath = Path.of(projectAbsolutePath).newPath(Path.of(testTarget).subPath(1));
        else
            testTargetAbsolutePath = Path.of(projectAbsolutePath);
        String testTargetWorkingDirectory = testTargetAbsolutePath.getParent().toString();
        String testTargetName = testTargetAbsolutePath.getName();
        // Get appropriate path to executable
        String phpUnitExecutable = PHPUNIT_GLOBAL;
        if (hasComposerRunner(projectPath)) {
            phpUnitExecutable = projectAbsolutePath + PHPUNIT_COMPOSER;
        }
        // Get appropriate logger for PHP unit version
        String phpPrinterName = getPrinterName(phpUnitExecutable, testTargetWorkingDirectory);
        final File printerFile = getPrinterFile(phpPrinterName);
        final String printerDirAbsolutePath = printerFile.getParentFile().getAbsolutePath();
        PrinterListener printerListener = new PrinterListener();
        printerListener.startup();
        // Reset provider & tests root
        testResultsProvider = new PHPUnitTestResultsProvider();
        phpTestsRoot = new PHPUnitTestRoot();
        // Wait for listener thread to be started
        try {
            latchReady.await();
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
        final CommandLine cmdRunTests = new CommandLine(phpUnitExecutable, "--include-path", printerDirAbsolutePath,
                "--printer", phpPrinterName, testTargetName);
        ProcessBuilder pb = new ProcessBuilder().redirectErrorStream(true)
                .directory(new File(testTargetWorkingDirectory)).command(cmdRunTests.toShellCommand());
        pb.environment().put("ZEND_PHPUNIT_PORT", String.valueOf(PRINTER_PORT));
        Process processRunPHPUnitTests = pb.start();
        final StringBuilder stdErrOut = new StringBuilder();
        ProcessUtil.process(processRunPHPUnitTests, new AbstractLineConsumer() {
            @Override
            public void writeLine(String line) throws IOException {
                if (!line.isEmpty())
                    stdErrOut.append("\t" + line + "\n");
            }
        });
        int exitValue = processRunPHPUnitTests.waitFor();
        try {
            latchDone.await();
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
        if (exitValue != 0 && stdErrOut.length() > 0 && phpTestsRoot.getChildren() == null) {
            throw new TestFrameworkException("PHPUnit Error:\n" + stdErrOut.toString());
        }
        return testResultsProvider.getTestResultsRoot(phpTestsRoot);
    }

    /**
     * Returns test results for given result path.
     * 
     * @param testResultsPath
     * @return test results for given result path
     */
    public List<TestResultDto> getTestResults(List<String> testResultsPath) {
        return testResultsProvider.getTestResults(testResultsPath);
    }

    private String getPrinterName(String phpUnitExecutable, String testTargetWorkingDirectory) {
        final CommandLine cmdRunTests = new CommandLine(phpUnitExecutable, "--atleast-version", "6");
        Process processBuildClassPath;
        try {
            processBuildClassPath = new ProcessBuilder().redirectErrorStream(true)
                    .directory(new File(testTargetWorkingDirectory)).command(cmdRunTests.toShellCommand()).start();
            ProcessUtil.process(processBuildClassPath, LineConsumer.DEV_NULL);
            int code = processBuildClassPath.waitFor();
            if (code == 0)
                return PRINTER_NAME_V6X;
        } catch (Exception e) {
        }
        return PRINTER_NAME_V5X;
    }

    private File getPrinterFile(String phpLoggerName) {
        final String phpLoggerLocation = PRINTER_DIRECTORY + '/' + phpLoggerName + ".php";
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        final File tmpPrinterFile = new File(tmpDir, phpLoggerLocation);
        if (!tmpPrinterFile.exists()) {
            try {
                tmpPrinterFile.getParentFile().mkdir();
                tmpPrinterFile.createNewFile();
                InputStream printerFileContent = getClass().getClassLoader().getResourceAsStream(phpLoggerLocation);
                FileUtils.copyInputStreamToFile(printerFileContent, tmpPrinterFile);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            } finally {
                if (tmpPrinterFile.exists()) {
                    tmpPrinterFile.getParentFile().deleteOnExit();
                    tmpPrinterFile.deleteOnExit();
                }
            }
        }
        return tmpPrinterFile;
    }

    @SuppressWarnings("unchecked")
    private boolean hasComposerRunner(String projectPath) {
        VirtualFileEntry composerJson;
        try {
            composerJson = projectManager.getProjectsRoot().getChild(projectPath + "/composer.json");
            if (composerJson == null)
                return false;
        } catch (ServerException e) {
            return false;
        }
        try (InputStream inputStream = composerJson.getVirtualFile().getContent();
                InputStreamReader reader = new InputStreamReader(inputStream);) {
            Gson gson = new GsonBuilder().create();
            Map<String, ?> composerJsonMap = gson.fromJson(reader, LinkedTreeMap.class);
            Map<String, String> requireDev = (Map<String, String>) composerJsonMap.get("require-dev");
            if (requireDev != null && requireDev.get("phpunit/phpunit") != null)
                return true;
            Map<String, String> require = (Map<String, String>) composerJsonMap.get("require");
            if (require != null && require.get("phpunit/phpunit") != null)
                return true;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return false;
    }

}
