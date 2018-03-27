package cz.cvut.kbss.ontodriver.jena.list;

import cz.cvut.kbss.ontodriver.descriptor.SimpleListDescriptor;
import cz.cvut.kbss.ontodriver.descriptor.SimpleListDescriptorImpl;
import cz.cvut.kbss.ontodriver.jena.environment.Generator;
import cz.cvut.kbss.ontodriver.jena.exception.ListProcessingException;
import cz.cvut.kbss.ontodriver.model.Assertion;
import cz.cvut.kbss.ontodriver.model.Axiom;
import cz.cvut.kbss.ontodriver.model.NamedResource;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.jena.rdf.model.ResourceFactory.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SimpleListIteratorTest extends ListIteratorTestBase<SimpleListIterator, SimpleListDescriptor> {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp();
    }

    @Override
    SimpleListIterator iterator() {
        return new SimpleListIterator(descriptor(null), connectorMock);
    }

    @Override
    SimpleListDescriptor descriptor(String context) {
        final NamedResource owner = NamedResource.create(RESOURCE.getURI());
        final Assertion hasList = Assertion.createObjectPropertyAssertion(URI.create(HAS_LIST.getURI()), false);
        final Assertion hasNext = Assertion.createObjectPropertyAssertion(URI.create(HAS_NEXT.getURI()), false);
        final SimpleListDescriptor descriptor = new SimpleListDescriptorImpl(owner, hasList, hasNext);
        if (context != null) {
            descriptor.setContext(URI.create(context));
        }
        return descriptor;
    }

    @Override
    List<URI> generateList() {
        return testUtil.generateSimpleList();
    }

    @Test
    public void nextReturnsFirstListElement() {
        final List<URI> list = generateList();
        final AbstractListIterator iterator = iterator();
        assertTrue(iterator.hasNext());
        final Axiom<NamedResource> head = iterator.nextAxiom();
        assertNotNull(head);
        assertEquals(NamedResource.create(RESOURCE.getURI()), head.getSubject());
        assertEquals(Assertion.createObjectPropertyAssertion(URI.create(HAS_LIST.getURI()), false),
                head.getAssertion());
        assertEquals(NamedResource.create(list.get(0)), head.getValue().getValue());
    }

    @Test
    public void nextThrowsListProcessingExceptionWhenNodeIsLiteral() {
        final List<URI> list = generateList();
        final String nodeUri = list.get(list.size() - 1).toString();
        when(connectorMock.find(createResource(nodeUri), HAS_NEXT, null)).thenReturn(
                Collections.singletonList(createStatement(createResource(nodeUri), HAS_NEXT, createTypedLiteral(117))));
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        thrown.expect(ListProcessingException.class);
        thrown.expectMessage("Expected successor of node " + nodeUri + " to be a named resource.");
        while (iterator.hasNext()) {
            iterator.nextAxiom();
        }
    }

    @Test
    public void removeWithoutReconnectRemovesLastElementInList() {
        final List<URI> list = generateList();
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        while (iterator.hasNext()) {
            iterator.nextAxiom();
        }
        iterator.removeWithoutReconnect();
        verify(connectorMock).remove(createResource(list.get(list.size() - 2).toString()), HAS_NEXT,
                createResource(list.get(list.size() - 1).toString()));
    }

    @Test
    public void removeWithoutReconnectRemovesFirstElementInList() {
        final List<URI> list = generateList();
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        iterator.nextValue();
        iterator.removeWithoutReconnect();
        verify(connectorMock).remove(RESOURCE, HAS_LIST, createResource(list.get(0).toString()));
    }

    @Test
    public void removeWithoutReconnectThrowsIllegalStateExceptionWhenNextWasNotCalledBefore() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(containsString("Cannot call remove before calling nextAxiom."));
        generateList();
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        iterator.removeWithoutReconnect();
    }

    @Test
    public void removeWithoutReconnectThrowsIllegalStateExceptionWhenRemoveIsCalledTwiceOnElement() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(containsString("Cannot call remove multiple times on one element."));
        generateList();
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        iterator.nextValue();
        iterator.removeWithoutReconnect();
        iterator.removeWithoutReconnect();
    }

    @Test
    public void replaceConnectsNodeToListEnd() {
        final List<URI> list = generateList();
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        while (iterator.hasNext()) {
            iterator.nextValue();
        }
        final Resource newOne = createResource(Generator.generateUri().toString());
        iterator.replace(newOne);
        verify(connectorMock).remove(createResource(list.get(list.size() - 2).toString()), HAS_NEXT,
                createResource(list.get(list.size() - 1).toString()));
        final Statement expectedAdded = createStatement(createResource(list.get(list.size() - 2).toString()), HAS_NEXT,
                newOne);
        verify(connectorMock).add(Collections.singletonList(expectedAdded));
    }

    @Test
    public void replaceReplacesNodeInsideList() {
        final List<URI> list = generateList();
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        int index = Generator.randomInt(list.size() - 1);
        if (index == 0) {
            index = index + 1;
        }
        int i = 0;
        while (iterator.hasNext() && i <= index) {
            iterator.nextValue();
            i++;
        }
        final Resource newOne = createResource(Generator.generateUri().toString());
        iterator.replace(newOne);
        verify(connectorMock).remove(createResource(list.get(index - 1).toString()), HAS_NEXT,
                createResource(list.get(index).toString()));
        verify(connectorMock).remove(createResource(list.get(index).toString()), HAS_NEXT,
                createResource(list.get(index + 1).toString()));
        final Statement expectedAddedPrevLink = createStatement(createResource(list.get(index - 1).toString()),
                HAS_NEXT,
                newOne);
        final Statement expectedAddedNextLink = createStatement(newOne, HAS_NEXT,
                createResource(list.get(index + 1).toString()));
        verify(connectorMock).add(Arrays.asList(expectedAddedPrevLink, expectedAddedNextLink));
    }

    @Test
    public void replaceReplacesFirstNodeInList() {
        final List<URI> list = generateList();
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        iterator.nextValue();
        final Resource newOne = createResource(Generator.generateUri().toString());
        iterator.replace(newOne);
        verify(connectorMock).remove(RESOURCE, HAS_LIST, createResource(list.get(0).toString()));
        verify(connectorMock)
                .remove(createResource(list.get(0).toString()), HAS_NEXT, createResource(list.get(1).toString()));
        final Statement expectedAddedPrevLink = createStatement(RESOURCE, HAS_LIST, newOne);
        final Statement expectedAddedNextLink = createStatement(newOne, HAS_NEXT,
                createResource(list.get(1).toString()));
        verify(connectorMock).add(Arrays.asList(expectedAddedPrevLink, expectedAddedNextLink));
    }

    @Test
    public void replaceDoesNotConnectNextNodeToItselfWhenListItemIsBeingRemoved() {
        final List<URI> list = generateList();
        final SimpleListIterator iterator = new SimpleListIterator(descriptor(null), connectorMock);
        iterator.nextValue();
        iterator.nextValue();
        final Resource newOne = createResource(list.get(2).toString());
        iterator.replace(newOne);
        verify(connectorMock)
                .remove(createResource(list.get(0).toString()), HAS_NEXT, createResource(list.get(1).toString()));
        verify(connectorMock)
                .remove(createResource(list.get(1).toString()), HAS_NEXT, createResource(list.get(2).toString()));
        final Statement expectedAddedPrevLink = createStatement(createResource(list.get(0).toString()), HAS_NEXT,
                newOne);
        verify(connectorMock).add(Collections.singletonList(expectedAddedPrevLink));
    }
}