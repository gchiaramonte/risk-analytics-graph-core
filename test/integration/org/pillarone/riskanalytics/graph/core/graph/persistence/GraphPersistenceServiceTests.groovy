package org.pillarone.riskanalytics.graph.core.graph.persistence

import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.core.example.packet.TestPacket

class GraphPersistenceServiceTests extends GroovyTestCase {

    GraphPersistenceService graphPersistenceService

    void testSaveLoad() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        model.startComponents << node2

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        long id = model.id
        GraphModel persistentModel = GraphModel.get(id)
        assertNotNull persistentModel

        assertEquals "name", persistentModel.name
        assertEquals "package", persistentModel.packageName

        assertEquals 2, persistentModel.nodes.size()
        assertEquals 1, persistentModel.edges.size()

        Node name = persistentModel.nodes.find { it.name == "name" }
        assertNotNull name

        assertFalse name.startComponent
        assertEquals TestComponent.name, name.className

        NodePort name_input3 = name.ports.find { it.name == "input3" }
        assertNotNull name_input3

        Node name2 = persistentModel.nodes.find { it.name == "name2" }
        assertTrue name2.startComponent
        assertEquals TestComponent.name, name2.className

        NodePort name2_outClaims = name2.ports.find { it.name == "outClaims" }
        assertNotNull name2_outClaims

        Edge edge = persistentModel.edges.toList()[0]
        assertSame name_input3, edge.from
        assertSame name2_outClaims, edge.to


        ModelGraphModel reloaded = graphPersistenceService.load(id)

        assertEquals "name", reloaded.name
        assertEquals "package", reloaded.packageName

        assertEquals 2, reloaded.allComponentNodes.size()
        assertEquals 1, reloaded.allConnections.size()
        assertEquals 1, reloaded.startComponents.size()
    }

    void testSaveLoadComposedComponent() {
        ComposedComponentGraphModel model = new ComposedComponentGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        model.createOuterInPort(TestPacket, "inOuter")
        model.createOuterOutPort(TestPacket, "outOuter")

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        long id = model.id
        GraphModel persistentModel = GraphModel.get(id)
        assertNotNull persistentModel

        assertEquals "name", persistentModel.name
        assertEquals "package", persistentModel.packageName

        assertEquals 2, persistentModel.nodes.size()
        assertEquals 1, persistentModel.edges.size()

        Node name = persistentModel.nodes.find { it.name == "name" }
        assertNotNull name

        assertFalse name.startComponent
        assertEquals TestComponent.name, name.className

        NodePort name_input3 = name.ports.find { it.name == "input3" }
        assertNotNull name_input3

        Node name2 = persistentModel.nodes.find { it.name == "name2" }
        assertFalse name2.startComponent
        assertEquals TestComponent.name, name2.className

        NodePort name2_outClaims = name2.ports.find { it.name == "outClaims" }
        assertNotNull name2_outClaims

        Edge edge = persistentModel.edges.toList()[0]
        assertSame name_input3, edge.from
        assertSame name2_outClaims, edge.to

        assertEquals 2, persistentModel.ports.size()

        ComposedComponentGraphModel reloaded = graphPersistenceService.load(id)

        assertEquals "name", reloaded.name
        assertEquals "package", reloaded.packageName

        assertEquals 2, reloaded.allComponentNodes.size()
        assertEquals 1, reloaded.allConnections.size()
        assertEquals 1, reloaded.outerInPorts.size()
        assertEquals 1, reloaded.outerOutPorts.size()
    }

    void testDelete() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        assertNotNull model.id

        graphPersistenceService.delete(model)

        assertNull model.id

        assertEquals 0, GraphModel.count()
        assertEquals 0, Node.count()
        assertEquals 0, Edge.count()
        assertEquals 0, NodePort.count()
    }

    void testUpdate() {

        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        assertNotNull model.id
        long id = model.id

        model = new ModelGraphModel("name2", "package2")
        model.id = id
        node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "newName")
        node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "newName2")

        model.createConnection(node2.getPort("input3"), node.getPort("outClaims"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        GraphModel persistentModel = GraphModel.get(model.id)
        assertNotNull persistentModel

        assertEquals "name2", persistentModel.name
        assertEquals "package2", persistentModel.packageName

        assertEquals 2, persistentModel.nodes.size()
        assertEquals 1, persistentModel.edges.size()

        Node name = persistentModel.nodes.find { it.name == "newName" }
        assertNotNull name

        assertEquals TestComponent.name, name.className

        NodePort name_outClaims = name.ports.find { it.name == "outClaims" }
        assertNotNull name_outClaims

        Node name2 = persistentModel.nodes.find { it.name == "newName2" }
        assertEquals TestComponent.name, name2.className

        NodePort name2_input3 = name2.ports.find { it.name == "input3" }
        assertNotNull name2_input3

        Edge edge = persistentModel.edges.toList()[0]
        assertSame name2_input3, edge.from
        assertSame name_outClaims, edge.to

    }
}