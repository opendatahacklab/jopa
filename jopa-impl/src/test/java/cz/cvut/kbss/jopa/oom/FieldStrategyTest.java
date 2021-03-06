/**
 * Copyright (C) 2020 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.oom;

import cz.cvut.kbss.jopa.environment.OWLClassA;
import cz.cvut.kbss.jopa.environment.utils.Generators;
import cz.cvut.kbss.jopa.environment.utils.MetamodelMocks;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.model.metamodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class FieldStrategyTest {

    @Mock
    private EntityMappingHelper mapperMock;


    private MetamodelMocks metamodelMocks;

    private Descriptor descriptor = new EntityDescriptor();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.metamodelMocks = new MetamodelMocks();
        descriptor = spy(descriptor);
    }

    @Test
    void getLanguageRetrievesAttributeLanguageFromEntityDescriptor() throws Exception {
        final String lang = "cs";
        descriptor.setAttributeLanguage(OWLClassA.getStrAttField(), lang);
        final FieldStrategy<?, ?> sut = FieldStrategy.createFieldStrategy(metamodelMocks.forOwlClassA().entityType(),
                metamodelMocks.forOwlClassA().stringAttribute(), descriptor, mapperMock);
        assertEquals(lang, sut.getLanguage());
    }

    @Test
    void getAttributeContextRetrievesContextFromEntityDescriptor() throws Exception {
        final URI context = Generators.createIndividualIdentifier();
        descriptor.addAttributeContext(OWLClassA.getStrAttField(), context);
        final FieldStrategy<?, ?> sut = FieldStrategy.createFieldStrategy(metamodelMocks.forOwlClassA().entityType(),
                metamodelMocks.forOwlClassA().stringAttribute(), descriptor, mapperMock);
        assertEquals(context, sut.getAttributeContext());
        verify(descriptor).getAttributeContext(metamodelMocks.forOwlClassA().stringAttribute());
    }

    @Test
    void getLanguageReturnsNullForSimpleLiteralAttribute() {
        final FieldStrategy<?, ?> sut = FieldStrategy.createFieldStrategy(metamodelMocks.forOwlClassM().entityType(),
                metamodelMocks.forOwlClassM().simpleLiteralAttribute(), descriptor, mapperMock);
        assertNull(sut.getLanguage());
    }

    @Test
    void createFieldStrategyCreatesPluralDataPropertyStrategyForDataPropertyCollectionAttribute() {
        final EntityType et = mock(EntityType.class);
        final CollectionAttributeImpl att = mock(CollectionAttributeImpl.class);
        when(att.isCollection()).thenReturn(true);
        when(att.getCollectionType()).thenReturn(PluralAttribute.CollectionType.COLLECTION);
        when(att.isAssociation()).thenReturn(false);
        when(att.getBindableJavaType()).thenReturn(String.class);
        when(att.getElementType()).thenReturn(BasicTypeImpl.get(String.class));
        when(att.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.DATA);
        final FieldStrategy<?, ?> result = FieldStrategy.createFieldStrategy(et, att, descriptor, mapperMock);
        assertThat(result, instanceOf(PluralDataPropertyStrategy.class));
    }

    @Test
    void createFieldStrategyCreatesSimpleSetStrategyForObjectPropertyCollectionAttribute() {
        final EntityType et = mock(EntityType.class);
        final CollectionAttributeImpl att = mock(CollectionAttributeImpl.class);
        when(att.isCollection()).thenReturn(true);
        when(att.getCollectionType()).thenReturn(PluralAttribute.CollectionType.COLLECTION);
        when(att.isAssociation()).thenReturn(true);
        when(att.getBindableJavaType()).thenReturn(OWLClassA.class);
        when(att.getElementType()).thenReturn(metamodelMocks.forOwlClassA().entityType());
        when(att.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.OBJECT);
        final FieldStrategy<?, ?> result = FieldStrategy.createFieldStrategy(et, att, descriptor, mapperMock);
        assertThat(result, instanceOf(SimpleSetPropertyStrategy.class));
    }
}
