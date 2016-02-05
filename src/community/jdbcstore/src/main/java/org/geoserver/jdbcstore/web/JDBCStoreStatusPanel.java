/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jdbcstore.web;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;

public class JDBCStoreStatusPanel extends Panel {
    
    public JDBCStoreStatusPanel(String id, JDBCResourceStoreProperties config) {
        super(id);
        this.add(new Label("jdbcConfigDatasourceId", config.getDatasourceId()));
    }

}
