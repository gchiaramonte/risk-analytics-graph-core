package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.graph.core.graphimport.ComposedComponentGraphImport
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition

class ComposedComponentNode extends ComponentNode {

    ComposedComponentGraphModel componentGraph;

    public static ComposedComponentNode createInstance(ComponentDefinition definition, String name) {
        ComposedComponentNode node = new ComposedComponentNode(type: definition, name: name)
        addPorts(definition, node)
        return node
    }

    public ComposedComponentGraphModel getComponentGraph() {
        if (componentGraph == null) {
            componentGraph = new ComposedComponentGraphImport().importGraph(type.typeClass, null)
            for (Port p : componentGraph.outerInPorts) {
                p.componentNode = this
            }
            for (Port p : componentGraph.outerOutPorts) {
                p.componentNode = this
            }
        }
        return componentGraph;
    }

    public List<InPort> getReplicatedInPorts(InPort inPort) {
        if (this.inPorts.find {it.is(inPort)} == null) {
            return null;
        }
        List<InPort> replicated = new ArrayList<InPort>();

        Port outerInPort = getComponentGraph().outerInPorts.find {it.name.equals(inPort.name)};

        for (Connection c: getComponentGraph().allConnections.findAll {it.from == outerInPort}) {
            replicated.add(c.to);
        }
        return replicated;
    }

    public List<OutPort> getReplicatedOutPorts(OutPort outPort) {
        if (this.outPorts.find {it.is(outPort)} == null) {
            return null;
        }
        List<OutPort> replicated = new ArrayList<OutPort>();

        Port outerOutPort = getComponentGraph().outerOutPorts.find {it.name.equals(outPort.name)};

        for (Connection c: getComponentGraph().allConnections.findAll {it.to == outerOutPort}) {
            replicated.add(c.from);
        }
        return replicated;
    }
}
