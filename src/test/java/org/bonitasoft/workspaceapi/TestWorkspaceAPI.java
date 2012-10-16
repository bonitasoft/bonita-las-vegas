/**
 * Copyright (C) 2012 BonitaSoft S.A.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.model.ProcessDefinition;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.DeletingEnabledProcessException;
import org.bonitasoft.engine.exception.InvalidBusinessArchiveFormat;
import org.bonitasoft.engine.exception.InvalidSessionException;
import org.bonitasoft.engine.exception.OrganizationDeleteException;
import org.bonitasoft.engine.exception.OrganizationImportException;
import org.bonitasoft.engine.exception.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.exception.ProcessDeletionException;
import org.bonitasoft.engine.exception.ProcessDeployException;
import org.bonitasoft.engine.exception.ProcessDisablementException;
import org.bonitasoft.engine.exception.ProcessEnablementException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TestWorkspaceAPI extends CommonAPITest {

    @Test
    public void testInstallGeneratedBar() throws InvalidBusinessArchiveFormat, IOException, InvalidSessionException, ProcessDeployException, ProcessDefinitionNotFoundException, OrganizationImportException, OrganizationDeleteException, ProcessDeletionException, DeletingEnabledProcessException, ProcessDisablementException{
        File organizationFile = new File(getClass().getResource("/ACME.xml").getFile());
        Assert.assertTrue("Organization file not found",organizationFile.exists());

        String organizationContent = getFileContent(organizationFile);
        getIdentityAPI().importOrganization(organizationContent);

        Map<String,InputStream> bars = getBars();
        Assert.assertTrue("No bar found in resources",!bars.isEmpty());
        try{
            for(Entry<String,InputStream> entry : bars.entrySet()){
                BusinessArchive archive =  BusinessArchiveFactory.readBusinessArchive(entry.getValue()) ;
                try{
                    ProcessDefinition def = getProcessAPI().deploy(archive);
                    Assert.assertNotNull("Failed to deploy "+entry.getKey(),def);
                    getProcessAPI().enableProcess(def.getId());
                    getProcessAPI().disableProcess(def.getId());
                    getProcessAPI().deleteProcess(def.getId());
                }catch (ProcessDeployException e) {
                    if(e.getExceptions() != null){
                        for(BonitaException ex : e.getExceptions()){
                            ex.printStackTrace();
                        }
                    }
                } catch (ProcessEnablementException e) {
                    Assert.fail("Failed to enable "+entry.getKey());
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

    private Map<String,InputStream> getBars() throws FileNotFoundException {
        final Map<String,InputStream> bars = new HashMap<String,InputStream>();
        bars.put("Buy a MINI--6.0.bar",getClass().getResourceAsStream("/Buy a MINI--6.0.bar"));
        bars.put("Toursim demo--1.0.bar ",getClass().getResourceAsStream("/Toursim demo--1.0.bar"));
        return bars;
    }

    @Before
    public void setUp() throws BonitaException{
        login();
    }

    @After
    public void tearDown() throws BonitaException{
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
