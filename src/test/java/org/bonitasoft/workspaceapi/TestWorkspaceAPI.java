/**
 * Copyright (C) 2012-2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.workspaceapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.Context;

import junit.framework.Assert;

import org.bonitasoft.engine.api.PlatformLoginAPI;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.model.Problem;
import org.bonitasoft.engine.bpm.model.ProcessDefinition;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.IllegalProcessStateException;
import org.bonitasoft.engine.exception.InvalidBusinessArchiveFormatException;
import org.bonitasoft.engine.exception.identity.OrganizationImportException;
import org.bonitasoft.engine.exception.platform.InvalidSessionException;
import org.bonitasoft.engine.exception.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.exception.process.ProcessDeployException;
import org.bonitasoft.engine.exception.process.ProcessDisablementException;
import org.bonitasoft.engine.exception.process.ProcessEnablementException;
import org.bonitasoft.engine.session.PlatformSession;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.ObjectAlreadyExistsException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bonitasoft.engine.CommonAPISPTest;
import com.bonitasoft.engine.SPBPMTestUtil;
import com.bonitasoft.engine.api.PlatformAPI;
import com.bonitasoft.engine.api.PlatformAPIAccessor;
import com.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import com.bonitasoft.engine.bpm.model.ParameterInstance;
import com.bonitasoft.engine.exception.ParameterNotFoundException;


public class TestWorkspaceAPI extends CommonAPISPTest {

    private List<Long> definitions;
      
    static ConfigurableApplicationContext contextJNDI;
    static ConfigurableApplicationContext springContext;
    
    private static void setupSpringContext() {
        setSystemPropertyIfNotSet("sysprop.bonita.db.vendor", "h2");

        // Force these system properties
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.bonitasoft.engine.local.SimpleMemoryContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.bonitasoft.engine.local");

        springContext = new ClassPathXmlApplicationContext("datasource.xml", "jndi-setup.xml");
    }

    private static void closeSpringContext() {
        springContext.close();
    }
    
    private static void setSystemPropertyIfNotSet(String property, String value) {
        System.setProperty(property, System.getProperty(property, value));
    }
    
    @BeforeClass
    public static void beforeClass() throws BonitaException {
    	System.err.println("=================== TestWorkspaceAPI.beforeClass()");
    	setupSpringContext();

        PlatformLoginAPI platformLoginAPI = PlatformAPIAccessor.getPlatformLoginAPI();
        PlatformSession session = platformLoginAPI.login("platformAdmin", "platform");
        PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(session);
        platformAPI.createPlatform();
        platformLoginAPI.logout(session);

        System.setProperty("delete.job.frequency", "0/30 * * * * ?");
    	
        SPBPMTestUtil.createEnvironmentWithDefaultTenant();
    }

    @AfterClass
    public static void afterClass() throws BonitaException {
        SPBPMTestUtil.destroyPlatformAndTenants();
        
        System.err.println("=================== TestWorkspaceAPI.afterClass()");
        PlatformLoginAPI platformLoginAPI = PlatformAPIAccessor.getPlatformLoginAPI();
        PlatformSession session = platformLoginAPI.login("platformAdmin", "platform");
        PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(session);
        platformAPI.deletePlaftorm();
        platformLoginAPI.logout(session);

        closeSpringContext();
    }
       
    @Test
    public void installGeneratedBar() throws IOException, InvalidSessionException, ProcessDeployException, ProcessDefinitionNotFoundException, OrganizationImportException,  ProcessDisablementException, ObjectAlreadyExistsException, IllegalProcessStateException, InvalidBusinessArchiveFormatException, DeletionException{
        File organizationFile = new File(TestWorkspaceAPI.class.getResource("/ACME.xml").getFile());
        Assert.assertTrue("Organization file not found",organizationFile.exists());

        String organizationContent = getFileContent(organizationFile);
        getIdentityAPI().importOrganization(organizationContent);

        Map<String,InputStream> bars = getBars();
        Assert.assertTrue("No bar found in resources",!bars.isEmpty());

        definitions = new ArrayList<Long>();
        try{
            for(Entry<String,InputStream> entry : bars.entrySet()){
                BusinessArchive archive =  BusinessArchiveFactory.readBusinessArchive(entry.getValue()) ;
                final String entryKey = entry.getKey();
                ProcessDefinition def = null;
                try{
                    def = getProcessAPI().deploy(archive);
                    final long defId = def.getId();
					definitions.add(defId);
                    Assert.assertNotNull("Failed to deploy "+entryKey,def);
                    getProcessAPI().enableProcess(defId);
                    if(entryKey.contains("PoolWithParameters")){
                    	checkParameter(entryKey, defId, "textParameter", "some text", String.class.getName());
                    	checkParameter(entryKey, defId, "booleanParameter", "true", Boolean.class.getName());
                    	checkParameter(entryKey, defId, "integerParameter", "2", Integer.class.getName());
                    }
                    getProcessAPI().disableProcess(defId);
                    getProcessAPI().deleteProcess(defId);
                    definitions.remove(defId);
                } catch (ProcessEnablementException e) {
                	StringBuilder sb = new StringBuilder("Failed to enable "+entryKey +"\n"+e.getMessage());
                	if(def != null){
                		List<Problem> processResolutionProblems;
                		processResolutionProblems = getProcessAPI().getProcessResolutionProblems(def.getId());
                		for (Problem problem : processResolutionProblems) {
                			sb.append("\n"+problem.toString());
                		}
                	}
                    Assert.fail(sb.toString());
                } catch (DeletionException e) {
                	StringBuilder sb = new StringBuilder("Failed to delete "+entryKey +"\n"+e.getMessage());
                	if(def != null){
                		List<Problem> processResolutionProblems;
                		processResolutionProblems = getProcessAPI().getProcessResolutionProblems(def.getId());
                		for (Problem problem : processResolutionProblems) {
                			sb.append("\n"+problem.toString());
                		}
                	}
                    Assert.fail(sb.toString());
				} catch (AlreadyExistsException e) {
                	StringBuilder sb = new StringBuilder("Process already exists "+entryKey +"\n"+e.getMessage());
                	if(def != null){
                		List<Problem> processResolutionProblems;
                		processResolutionProblems = getProcessAPI().getProcessResolutionProblems(def.getId());
                		for (Problem problem : processResolutionProblems) {
                			sb.append("\n"+problem.toString());
                		}
                	}
                    Assert.fail(sb.toString());
				}
                entry.getValue().close();
            }
        }finally{
            getIdentityAPI().deleteOrganization();
            for(Entry<String,InputStream> entry : bars.entrySet()){
                if(entry.getValue() != null){
                    entry.getValue().close();
                }
            }
        }
    }

	private void checkParameter(final String entryKey, final long defId,
			final String parameterKey, final Object parameterValue,
			final String parameterType) throws InvalidSessionException,
			ProcessDefinitionNotFoundException {
		try {                 		
			ParameterInstance parameterInstance = getProcessAPI().getParameterInstance(defId, parameterKey);
			Assert.assertEquals(parameterValue, parameterInstance.getValue());
			Assert.assertEquals(parameterType, parameterInstance.getType());
		} catch (ParameterNotFoundException e) {
			Assert.fail("Parameter "+parameterKey+" not found in process "+ entryKey);
		}
	}

    private Map<String,InputStream> getBars() throws FileNotFoundException {
        final Map<String,InputStream> bars = new HashMap<String,InputStream>();
        bars.put("Buy a MINI--6.0.bar",TestWorkspaceAPI.class.getResourceAsStream("/Buy a MINI--6.0.bar"));
        bars.put("Toursim demo--1.0.bar ",TestWorkspaceAPI.class.getResourceAsStream("/Toursim demo--1.0.bar"));
        bars.put("PoolWithParameters--1.0.bar",TestWorkspaceAPI.class.getResourceAsStream("/PoolWithParameters--1.0.bar"));      
        return bars;
    }

    @Before
    public void setUp() throws BonitaException{
        login();
    }

    @After
    public void tearDown() throws BonitaException{
        if(definitions != null && definitions.isEmpty()){
            for(Long defId : definitions){
                getProcessAPI().disableProcess(defId);
                getProcessAPI().deleteProcess(defId);
            }
        }
        logout();
    }
    

    private String getFileContent(File file) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line = ""; //$NON-NLS-1$
        while((line = reader.readLine()) != null)
        {
            sb.append(line);
            sb.append('\n');
        }
        reader.close();

        return sb.toString() ;
    }
}
