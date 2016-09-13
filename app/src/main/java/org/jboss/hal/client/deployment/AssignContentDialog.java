/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.hal.client.deployment;

import java.util.List;
import java.util.Set;

import com.google.gwt.safehtml.shared.SafeHtml;
import elemental.dom.Element;
import elemental.html.InputElement;
import org.jboss.gwt.elemento.core.Elements;
import org.jboss.hal.ballroom.Alert;
import org.jboss.hal.ballroom.dialog.Dialog;
import org.jboss.hal.ballroom.form.SwitchBridge;
import org.jboss.hal.ballroom.table.Column;
import org.jboss.hal.ballroom.table.DataTable;
import org.jboss.hal.ballroom.table.Options;
import org.jboss.hal.ballroom.table.OptionsBuilder;
import org.jboss.hal.resources.CSS;
import org.jboss.hal.resources.Icons;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static org.jboss.gwt.elemento.core.InputType.checkbox;
import static org.jboss.hal.ballroom.table.Api.RefreshMode.RESET;

/**
 * Dialog used to assign and unassign content.
 *
 * @author Harald Pehl
 */
class AssignContentDialog {

    @FunctionalInterface
    interface AssignCallback {

        void assign(Content content, List<String> serverGroups, boolean enable);
    }


    @FunctionalInterface
    interface UnassignCallback {

        void unassign(Content content, List<String> serverGroups);
    }


    private static class ServerGroup {

        final String serverGroup;

        ServerGroup(final String serverGroup) {
            this.serverGroup = serverGroup;
        }
    }


    private static final String ENABLE_CONTAINER = "enableContainer";
    private static final String ENABLE = "enable";

    private final Content content;
    private final List<ServerGroup> serverGroups;
    private final AssignCallback assignCallback;
    private final UnassignCallback unassignCallback;
    private final Alert noServerGroupSelected;
    private final DataTable<ServerGroup> table;
    private final Element enableContainer;
    private final InputElement enable;
    private final Dialog dialog;

    AssignContentDialog(final Content content, final Set<String> unassignedServerGroups,
            final Resources resources, final AssignCallback callback) {
        this(content, unassignedServerGroups, resources, callback, null);
    }

    AssignContentDialog(final Content content, final Set<String> assignedServerGroups,
            final Resources resources, final UnassignCallback callback) {
        this(content, assignedServerGroups, resources, null, callback);
    }

    private AssignContentDialog(final Content content, final Set<String> serverGroups, final Resources resources,
            final AssignCallback assignCallback, final UnassignCallback unassignCallback) {
        this.content = content;
        //noinspection Convert2MethodRef - do not replace w/ method reference. GWT compiler will blow up
        this.serverGroups = serverGroups.stream()
                .sorted(naturalOrder())
                .map((serverGroup) -> new ServerGroup(serverGroup))
                .collect(toList());
        this.assignCallback = assignCallback;
        this.unassignCallback = unassignCallback;

        noServerGroupSelected = new Alert(Icons.ERROR, resources.messages().noServerGroupSelected());

        Options<ServerGroup> options = new OptionsBuilder<ServerGroup>()
                .checkboxColumn()
                .column(Names.SERVER_GROUP, new Column.RenderCallback<ServerGroup, String>() {
                    @Override
                    public String render(final String cell, final String type, final ServerGroup row,
                            final Column.Meta meta) {
                        return row.serverGroup;
                    }
                })
                .keys(false)
                .paging(false)
                .searching(false)
                .multiselect()
                .build();
        table = new DataTable<>(Ids.ASSIGNMENT_ADD_TABLE, options);

        SafeHtml description = assignCallback != null ? resources.messages()
                .assignContentDescription(content.getName()) : resources.messages()
                .unassignContentDescription(content.getName());
        // @formatter:off
        Elements.Builder builder = new Elements.Builder()
            .div().add(noServerGroupSelected).end()
            .p().innerHtml(description).end()
            .add(table)
            .div().rememberAs(ENABLE_CONTAINER)
                .input(checkbox).rememberAs(ENABLE).id(Ids.ASSIGNMENT_ENABLE)
                .label().css(CSS.marginLeft4)
                    .attr("for", Ids.ASSIGNMENT_ENABLE)
                    .textContent(resources.constants().enableContent())
                .end()
            .end();
        // @formatter:on
        enable = builder.referenceFor(ENABLE);
        enableContainer = builder.referenceFor(ENABLE_CONTAINER);

        String title = assignCallback != null ? resources.constants().assignContent() : resources.constants()
                .unassignContent();
        String primary = assignCallback != null ? resources.constants().assign() : resources.constants().unassign();
        dialog = new Dialog.Builder(title)
                .add(builder.elements())
                .primary(primary, this::finish)
                .cancel()
                .closeIcon(true)
                .closeOnEsc(true)
                .build();
        dialog.registerAttachable(table);
    }

    private boolean finish() {
        boolean hasSelection = table.api().hasSelection();
        Elements.setVisible(noServerGroupSelected.asElement(), !hasSelection);
        if (hasSelection) {
            List<String> serverGroups = table.api().selectedRows().stream()
                    .map(usg -> usg.serverGroup)
                    .collect(toList());
            if (assignCallback != null) {
                assignCallback.assign(content, serverGroups, SwitchBridge.Bridge.element(enable).getValue());
            } else if (unassignCallback != null) {
                unassignCallback.unassign(content, serverGroups);
            }
        }
        return hasSelection;
    }

    void show() {
        dialog.show();
        Elements.setVisible(noServerGroupSelected.asElement(), false);
        Elements.setVisible(enableContainer, assignCallback != null);
        table.api().clear().add(serverGroups).refresh(RESET);
        SwitchBridge.Bridge.element(enable).setValue(false);
    }
}
