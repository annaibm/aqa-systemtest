/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package net.adoptopenjdk.stf;

import net.adoptopenjdk.loadTest.InventoryData;
import net.adoptopenjdk.loadTest.TimeBasedLoadTest;
import net.adoptopenjdk.stf.environment.FileRef;
import net.adoptopenjdk.stf.environment.JavaVersion;
import net.adoptopenjdk.stf.extensions.core.StfCoreExtension;
import net.adoptopenjdk.stf.extensions.core.StfCoreExtension.Echo;
import net.adoptopenjdk.stf.processes.ExpectedOutcome;
import net.adoptopenjdk.stf.processes.definitions.LoadTestProcessDefinition;
import net.adoptopenjdk.stf.runner.modes.HelpTextGenerator;

public class MauveSingleThrdLoad extends TimeBasedLoadTest {
	public void help(HelpTextGenerator help) throws StfException {
		help.outputSection("MauveSingleThrdLoad");
		help.outputText("The MauveSingleThrdLoad runs a subset of tests from the GNU mauve project. "
				+ "The tests run on a single thread because some of them exercise swing and awt "
				+ "which are not thread safe");
	}

	public void execute(StfCoreExtension test) throws StfException {

		FileRef mauveJar = test.env().findPrereqFile("mauve/mauve.jar");

		if (!mauveJar.asJavaFile().exists()) {
			throw new StfException("Application jar file does not exist: " + mauveJar.getSpec());
		}
		
		JavaVersion jvm = test.env().primaryJvm();

		String inventoryFile = "/openjdk.test.load/config/inventories/mauve/mauve_all.xml";
		
		int numMauveTests = InventoryData.getNumberOfTests(test, inventoryFile);
		
		/*
		 * Three Mauve tests require these add-opens options to run because modularity Java (9+) 
		 * denies them the JCL module/package access they need to run to completion.
		 */
		String[] modularityOptions = null;
		if (jvm.getJavaVersion() >= 9) { 
			modularityOptions = new String[] {
				"--add-opens=java.base/java.util=ALL-UNNAMED",
				"--add-opens=java.base/java.lang=ALL-UNNAMED",
				"--add-opens=java.base/java.io=ALL-UNNAMED"
			}; 
		}
		
		LoadTestProcessDefinition loadTestInvocation = test.createLoadTestSpecification()
				.addJvmOption(modularityOptions)
				.addJarToClasspath(mauveJar); 
		
		if (isTimeBasedLoadTest) { 
			loadTestInvocation = loadTestInvocation.setTimeLimit(timeLimit);	// If it's a time based test, stop execution after given time duration
		}
		
		loadTestInvocation = loadTestInvocation.addSuite("mauve")
				.setSuiteInventory(inventoryFile)
				.setSuiteThreadCount(1);
				
		if (!isTimeBasedLoadTest) { 
			loadTestInvocation = loadTestInvocation.setSuiteNumTests(numMauveTests * 500); // Run each test 500 times
		}
		
		loadTestInvocation = loadTestInvocation.setSuiteSequentialSelection();
		
		test.doRunForegroundProcess("Run Mauve load test", "LT", Echo.ECHO_ON,
				ExpectedOutcome.cleanRun().within(finalTimeout), 
				loadTestInvocation);
	}
}

