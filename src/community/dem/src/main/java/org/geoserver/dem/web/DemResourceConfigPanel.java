/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.dem.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.ReorderableTablePanel;


/**
 * Resource configuration page for DEMStore
 */
public class DemResourceConfigPanel extends ResourceConfigurationPanel {

    public static final GeoServerDataProvider.Property<String> ATTR_PROP  =
        new GeoServerDataProvider.PropertyPlaceholder<>("attribute");

    private final ReorderableTablePanel<String> tablePanel;
    private List<String> sortOrderList;

    public DemResourceConfigPanel(String panelId, IModel model) {
        super(panelId, model);

        CoverageInfo resource = (CoverageInfo) this.getResourceInfo();
        String sortOrder = (String) resource.getParameters().get("SORTING");
        sortOrderList = Arrays.asList(sortOrder.split(",")).stream().map(String::trim).collect(
                Collectors.toList());

        tablePanel = new ReorderableTablePanel<String>(
                "attributesTable",
                sortOrderList,
                Collections.singletonList(ATTR_PROP)) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    GeoServerDataProvider.Property property) {
                return new Label(id, (String)itemModel.getObject());
            }

            @Override
            protected void onPopulateItem(GeoServerDataProvider.Property<String> property,
                    ListItem<GeoServerDataProvider.Property<String>> item) {
                super.onPopulateItem(property, item);
            }
        };
        tablePanel.setFilterable(false);
        tablePanel.setItemReuseStrategy(new DefaultItemReuseStrategy());
        tablePanel.setOutputMarkupId(true);
        tablePanel.setPageable(false);
        this.add(tablePanel);

    }

    @Override
    public void onSave(boolean isNew) {
        super.onSave(isNew);
        String newSortOrderString = this.getSortOrderString();
        CoverageInfo resource = (CoverageInfo) this.getResourceInfo();
        resource.getParameters().put("SORTING", newSortOrderString);
    }

    public String getSortOrderString() {
        return StringUtils.join(this.sortOrderList, ", ");
    }
}
