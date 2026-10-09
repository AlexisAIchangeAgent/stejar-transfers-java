package md.stejar.transfers.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectDirectories;
import org.junit.platform.suite.api.Suite;

/**
 * Runs the Gherkin features of the folder features/ at the root of the repository, the only
 * copy of the feature files. Step definitions go in this package. Run it with: mvn test -Pbdd
 */
@Suite
@IncludeEngines("cucumber")
@SelectDirectories("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "md.stejar.transfers.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class RunCucumberTest {}
