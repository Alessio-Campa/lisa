package it.unive.lisa;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.JsonReport.JsonWarning;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.outputs.compare.JsonReportComparer.DiffReporter;
import it.unive.lisa.outputs.compare.JsonReportComparer.REPORTED_COMPONENT;
import it.unive.lisa.outputs.compare.JsonReportComparer.REPORT_TYPE;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.file.FileManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;

public abstract class AnalysisTestExecutor {

	protected static final String EXPECTED_RESULTS_DIR = "imp-testcases";
	protected static final String ACTUAL_RESULTS_DIR = "test-outputs";

	/**
	 * Performs a test, running an analysis. The test will fail if:
	 * <ul>
	 * <li>The imp file cannot be parsed (i.e. a {@link ParsingException} is
	 * thrown)</li>
	 * <li>The previous working directory using for the test execution cannot be
	 * deleted</li>
	 * <li>The analysis run terminates with an {@link AnalysisException}</li>
	 * <li>One of the json reports (either the one generated during the test
	 * execution or the one used as baseline) cannot be found or cannot be
	 * opened</li>
	 * <li>The two json reports are different</li>
	 * <li>The external files mentioned in the reports are different</li>
	 * </ul>
	 * 
	 * @param folder        the name of the sub-folder; this is used for
	 *                          searching expected results and as a working
	 *                          directory for executing tests in the test
	 *                          execution folder
	 * @param source        the name of the imp source file to be searched in
	 *                          the given folder
	 * @param configuration the configuration of the analysis to run (note that
	 *                          the workdir present into the configuration will
	 *                          be ignored, as it will be overwritten by the
	 *                          computed workdir)
	 */
	protected void perform(String folder, String source, LiSAConfiguration configuration) {
		System.out.println("Testing " + getCaller());
		performAux(folder, null, source, configuration, false);
	}

	/**
	 * Performs a test, running an analysis. The test will fail if:
	 * <ul>
	 * <li>The imp file cannot be parsed (i.e. a {@link ParsingException} is
	 * thrown)</li>
	 * <li>The previous working directory using for the test execution cannot be
	 * deleted</li>
	 * <li>The analysis run terminates with an {@link AnalysisException}</li>
	 * <li>One of the json reports (either the one generated during the test
	 * execution or the one used as baseline) cannot be found or cannot be
	 * opened</li>
	 * <li>The two json reports are different</li>
	 * <li>The external files mentioned in the reports are different</li>
	 * </ul>
	 * 
	 * @param folder        the name of the sub-folder; this is used for
	 *                          searching expected results and as a working
	 *                          directory for executing tests in the test
	 *                          execution folder
	 * @param source        the name of the imp source file to be searched in
	 *                          {@code folder}
	 * @param configuration the configuration of the analysis to run (note that
	 *                          the workdir present into the configuration will
	 *                          be ignored, as it will be overwritten by the
	 *                          computed workdir)
	 * @param forceUpdate   if {@code true}, baselines will be updated if the
	 *                          test fails
	 */
	protected void perform(String folder, String source, LiSAConfiguration configuration, boolean forceUpdate) {
		System.out.println("Testing " + getCaller());
		performAux(folder, null, source, configuration, forceUpdate);

	}

	/**
	 * Performs a test, running an analysis. The test will fail if:
	 * <ul>
	 * <li>The imp file cannot be parsed (i.e. a {@link ParsingException} is
	 * thrown)</li>
	 * <li>The previous working directory using for the test execution cannot be
	 * deleted</li>
	 * <li>The analysis run terminates with an {@link AnalysisException}</li>
	 * <li>One of the json reports (either the one generated during the test
	 * execution or the one used as baseline) cannot be found or cannot be
	 * opened</li>
	 * <li>The two json reports are different</li>
	 * <li>The external files mentioned in the reports are different</li>
	 * </ul>
	 * 
	 * @param folder        the name of the sub-folder; this is used for
	 *                          searching expected results and as a working
	 *                          directory for executing tests in the test
	 *                          execution folder
	 * @param source        the name of the imp source file to be searched in
	 *                          {@code folder}
	 * @param subfolder     an additional folder that is appended to
	 *                          {@code folder} both when computing the working
	 *                          directory and when searching for the expected
	 *                          results, but <b>not</b> for searching the source
	 *                          IMP program
	 * @param configuration the configuration of the analysis to run (note that
	 *                          the workdir present into the configuration will
	 *                          be ignored, as it will be overwritten by the
	 *                          computed workdir)
	 */
	protected void perform(String folder, String subfolder, String source, LiSAConfiguration configuration) {
		System.out.println("Testing " + getCaller());
		performAux(folder, subfolder, source, configuration, false);
	}

	/**
	 * Performs a test, running an analysis. The test will fail if:
	 * <ul>
	 * <li>The imp file cannot be parsed (i.e. a {@link ParsingException} is
	 * thrown)</li>
	 * <li>The previous working directory using for the test execution cannot be
	 * deleted</li>
	 * <li>The analysis run terminates with an {@link AnalysisException}</li>
	 * <li>One of the json reports (either the one generated during the test
	 * execution or the one used as baseline) cannot be found or cannot be
	 * opened</li>
	 * <li>The two json reports are different</li>
	 * <li>The external files mentioned in the reports are different</li>
	 * </ul>
	 * 
	 * @param folder        the name of the sub-folder; this is used for
	 *                          searching expected results and as a working
	 *                          directory for executing tests in the test
	 *                          execution folder
	 * @param source        the name of the imp source file to be searched in
	 *                          {@code folder}
	 * @param subfolder     an additional folder that is appended to
	 *                          {@code folder} both when computing the working
	 *                          directory and when searching for the expected
	 *                          results, but <b>not</b> for searching the source
	 *                          IMP program
	 * @param configuration the configuration of the analysis to run (note that
	 *                          the workdir present into the configuration will
	 *                          be ignored, as it will be overwritten by the
	 *                          computed workdir)
	 * @param forceUpdate   if {@code true}, baselines will be updated if the
	 *                          test fails
	 */
	protected void perform(String folder, String subfolder, String source, LiSAConfiguration configuration,
			boolean forceUpdate) {
		System.out.println("Testing " + getCaller());
		performAux(folder, subfolder, source, configuration, forceUpdate);
	}

	private void performAux(String folder, String subfolder, String source, LiSAConfiguration configuration,
			boolean forceUpdate) {
		Path expectedPath = Paths.get(EXPECTED_RESULTS_DIR, folder);
		Path actualPath = Paths.get(ACTUAL_RESULTS_DIR, folder);
		Path target = Paths.get(expectedPath.toString(), source);

		Program program = null;
		try {
			program = IMPFrontend.processFile(target.toString(), true);
		} catch (ParsingException e) {
			e.printStackTrace(System.err);
			fail("Exception while parsing '" + target + "': " + e.getMessage());
		}

		if (subfolder != null) {
			expectedPath = Paths.get(expectedPath.toString(), subfolder);
			actualPath = Paths.get(actualPath.toString(), subfolder);
		}

		File workdir = actualPath.toFile();
		try {
			FileManager.forceDeleteFolder(workdir.toString());
		} catch (IOException e) {
			e.printStackTrace(System.err);
			fail("Cannot delete working directory '" + workdir + "': " + e.getMessage());
		}
		configuration.setWorkdir(workdir.toString());

		configuration.setJsonOutput(true);

		LiSA lisa = new LiSA(configuration);
		try {
			lisa.run(program);
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}

		File expFile = Paths.get(expectedPath.toString(), "report.json").toFile();
		File actFile = Paths.get(actualPath.toString(), "report.json").toFile();
		boolean update = "true".equals(System.getProperty("lisa.cron.update")) || forceUpdate;
		try (FileReader l = new FileReader(expFile); FileReader r = new FileReader(actFile)) {
			JsonReport expected = JsonReport.read(l);
			JsonReport actual = JsonReport.read(r);
			Accumulator acc = new Accumulator(expectedPath);
			if (!update)
				assertTrue("Results are different",
						JsonReportComparer.compare(expected, actual, expectedPath.toFile(), actualPath.toFile()));
			else if (!JsonReportComparer.compare(expected, actual, expectedPath.toFile(),
					actualPath.toFile(), acc)) {
				System.err.println("Results are different, regenerating differences");
				boolean updateReport = !acc.addedWarning.isEmpty() || !acc.removedWarning.isEmpty()
						|| !acc.addedFilePaths.isEmpty() || !acc.removedFilePaths.isEmpty()
						|| !acc.changedFileName.isEmpty();
				if (updateReport) {
					Files.copy(actFile.toPath(), expFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					System.err.println("- Updated report.json");
				}
				for (Path f : acc.removedFilePaths) {
					Files.delete(f);
					System.err.println("- Deleted " + f);
				}
				for (Path f : acc.addedFilePaths) {
					Files.copy(f, Paths.get(expectedPath.toString(), actualPath.relativize(f).toString()));
					System.err.println("- Copied (new) " + f);
				}
				for (Path f : acc.changedFileName) {
					Path fresh = Paths.get(expectedPath.toString(), f.toString());
					Files.copy(
							Paths.get(actualPath.toString(), f.toString()),
							fresh,
							StandardCopyOption.REPLACE_EXISTING);
					System.err.println("- Copied (update) " + fresh);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
			fail("Unable to find report file");
		} catch (IOException e) {
			e.printStackTrace(System.err);
			fail("Unable to compare reports");
		}

	}

	private class Accumulator implements DiffReporter {

		private final Collection<Path> changedFileName = new HashSet<>();
		private final Collection<Path> addedFilePaths = new HashSet<>();
		private final Collection<Path> removedFilePaths = new HashSet<>();
		private final Collection<JsonWarning> addedWarning = new HashSet<>();
		private final Collection<JsonWarning> removedWarning = new HashSet<>();
		private final Path exp;

		public Accumulator(Path exp) {
			this.exp = exp;
		}

		@Override
		public void report(REPORTED_COMPONENT component, REPORT_TYPE type, Collection<?> reported) {
			switch (type) {
			case ONLY_FIRST:
				switch (component) {
				case FILES:
					reported.forEach(e -> removedFilePaths.add(Paths.get((String) e)));
					break;
				case WARNINGS:
					reported.forEach(e -> removedWarning.add((JsonWarning) e));
					break;
				default:
					break;
				}
				break;
			case ONLY_SECOND:
				switch (component) {
				case FILES:
					reported.forEach(e -> addedFilePaths.add(Paths.get((String) e)));
					break;
				case WARNINGS:
					reported.forEach(e -> addedWarning.add((JsonWarning) e));
					break;
				default:
					break;
				}
				break;
			case COMMON:
			default:
				break;

			}
		}

		@Override
		public void fileDiff(String first, String second, String message) {
			Path file = Paths.get(first);
			changedFileName.add(exp.relativize(file));
		}
	}

	private String getCaller() {
		StackTraceElement[] trace = Thread.getAllStackTraces().get(Thread.currentThread());
		// 0: java.lang.Thread.dumpThreads()
		// 1: java.lang.Thread.getAllStackTraces()
		// 2: it.unive.lisa.test.AnalysisTest.getCaller()
		// 3: it.unive.lisa.test.AnalysisTest.perform()
		// 4: caller
		return trace[4].getClassName() + "::" + trace[4].getMethodName();
	}
}
