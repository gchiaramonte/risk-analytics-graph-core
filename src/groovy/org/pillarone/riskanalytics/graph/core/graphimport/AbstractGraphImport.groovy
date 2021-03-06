package org.pillarone.riskanalytics.graph.core.graphimport

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.core.wiring.ITransmitter
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.*

public abstract class AbstractGraphImport {

    protected List<GraphImportListener> importListeners = new ArrayList<GraphImportListener>();

    public abstract AbstractGraphModel importGraph(Class clazz, String comments)


    public void addGraphImportListener(GraphImportListener importListener) {
        importListeners.add(importListener);
    }

    protected abstract ComposedComponentGraphModel getComposedComponentGraph(ComposedComponent cc)

    ;

    protected HashMap<Component, ComponentNode> getComponents(List<Component> subComponents, Object componentContainer, AbstractGraphModel graph) {
        HashMap<Component, ComponentNode> components = new HashMap<Component, ComponentNode>();
        for (Component c: subComponents) {
            String name = componentContainer.properties.find {Map.Entry entry -> entry.value.is(c)}.key;
            ComponentNode n = graph.createComponentNode(PaletteService.getInstance().getComponentDefinition(c.class), name);
            if (n instanceof ComposedComponentNode) {
                n.componentGraph = getComposedComponentGraph(c);
            }
            importListeners.each {it.nodeImported(n)};
            components.put(c, n);
        }
        return components;
    }


    protected void addConnections(AbstractGraphModel graph, HashMap<Component, ComponentNode> components) {
        HashMap<String, Boolean> visited = new HashMap<String, Boolean>();

        for (Component c: components.keySet()) {
            List allTransmitter = new ArrayList();
            allTransmitter.addAll(c.getAllOutputTransmitter());
            allTransmitter.addAll(c.getAllInputTransmitter());
            for (ITransmitter t: allTransmitter) {
                String toPort = WiringUtils.getSenderChannelName(t.getReceiver(), t.getTarget());
                String fromPort = WiringUtils.getSenderChannelName(t.getSender(), t.getSource());
                ComponentNode toComp = components.get(t.getReceiver());
                ComponentNode fromComp = components.get(t.getSender());

                //if ComposedComponent port, no Component found
                if (toComp == null || fromComp == null) {
                    continue;
                }

                String key = fromComp.name + "." + fromPort + "=" + toComp.name + "." + toPort;

                Port fromP = fromComp.outPorts.find {it.name.equals(fromPort)};
                Port toP = toComp.inPorts.find {it.name.equals(toPort)};

                if (fromP != null && toP != null) {
                    if (!visited.get(key)) {
                        Connection connection = graph.createConnection(fromP, toP);
                        importListeners.each {it.connectionImported(connection)};
                        visited.put(key, true);
                    }
                }

            }
        }
    }


}

class CommentImport implements GraphImportListener {
    private HashMap<String, String> comments = new HashMap<String, String>();

    public CommentImport(String content) {
        structureComments(content);
    }

    private void structureComments(String content) {
        Pattern p = Pattern.compile('/\\*+(.*?)\\*+/', Pattern.DOTALL);
        Matcher m = p.matcher(content);
        while (m.find()) {
            String plain = m.group(1).replaceAll('\\*', '').trim();
            try {
                StringReader sr = new StringReader(plain);
                String header = sr.readLine().trim();
                String comment = sr.readLine().trim();
                comments.put(header, comment);
            } catch (Exception e) {
                continue;
            }
        }
    }

    void nodeImported(ComponentNode node) {
        node.comment = comments.get("Component:" + getComponentKey(node));
    }

    void connectionImported(Connection connection) {
        List<String> connectionHeaders = new ArrayList<String>();
        connectionHeaders.add("Connection:" + getComponentKey(connection.from.componentNode) + "." + connection.from.name +
                "->" + getComponentKey(connection.to.componentNode) + "." + connection.to.name);
        connectionHeaders.add("Replication:" + connection.from.name + "->" +
                getComponentKey(connection.to.componentNode) + "." + connection.to.name);
        connectionHeaders.add("Replication:" + getComponentKey(connection.from.componentNode) + "." + connection.from.name +
                "->" + connection.to.name);
        for (String key: connectionHeaders) {
            String s;
            if ((s = comments.get(key)) != null) {
                connection.comment = s;
                break;
            }
        }
    }

    private String getComponentKey(ComponentNode c) {
        if (c != null)
            return c.name;
        else
            return "";
    }
}
