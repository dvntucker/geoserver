/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

/**
 * Displays a message suggesting the user to login or to elevate his privileges
 */
public class GeoServerErrorPage extends GeoServerBasePage {

    public GeoServerErrorPage(Throwable error) {
        IModel notice = null, errorText = new Model("");
        
        boolean trace = false;
        if (getSession().getAuthentication() != null &&
            getSession().getAuthentication().isAuthenticated()) 
        {
            try { 
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(bos);
                error.printStackTrace(ps);
                ps.close();
                bos.close();
                errorText = new Model( bos.toString() );
                notice = new ResourceModel("GeoServerErrorPage.whatIsThis");
                trace = true;
            } catch (Exception e) {
                notice = new ResourceModel("GeoServerErrorPage.failedAgain");
            }
        }

        add(new WebMarkupContainer("loggedOut").setVisible(!trace));

        WebMarkupContainer wmc = new WebMarkupContainer("trace-explanation");
        wmc.setVisible(trace);
        wmc.add(new ExternalLink(
                    "jiraLink", 
                    new Model("https://osgeo-org.atlassian.net/projects/GEOS"),
                    new ResourceModel("GeoServerErrorPage.jiraName")
                    ).setVisible(trace)
       );

        add(wmc);

        add(new Label("traceback", errorText).setVisible(trace));
    }
}
