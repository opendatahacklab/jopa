/**
 * Copyright (C) 2016 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.oom;

import cz.cvut.kbss.jopa.exceptions.StorageAccessException;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.model.metamodel.EntityType;
import cz.cvut.kbss.jopa.model.metamodel.Metamodel;
import cz.cvut.kbss.jopa.oom.exceptions.EntityDeconstructionException;
import cz.cvut.kbss.jopa.oom.exceptions.EntityReconstructionException;
import cz.cvut.kbss.jopa.oom.exceptions.UnpersistedChangeException;
import cz.cvut.kbss.jopa.sessions.LoadingParameters;
import cz.cvut.kbss.jopa.sessions.UnitOfWorkImpl;
import cz.cvut.kbss.jopa.utils.Configuration;
import cz.cvut.kbss.jopa.utils.EntityPropertiesUtils;
import cz.cvut.kbss.ontodriver.Connection;
import cz.cvut.kbss.ontodriver.descriptor.AxiomDescriptor;
import cz.cvut.kbss.ontodriver.descriptor.ReferencedListDescriptor;
import cz.cvut.kbss.ontodriver.descriptor.SimpleListDescriptor;
import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import cz.cvut.kbss.ontodriver.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class ObjectOntologyMapperImpl implements ObjectOntologyMapper, EntityMappingHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectOntologyMapperImpl.class);

    private final UnitOfWorkImpl uow;
    private final Connection storageConnection;
    private final Metamodel metamodel;

    private final AxiomDescriptorFactory descriptorFactory;
    private final EntityConstructor entityBuilder;
    private final EntityDeconstructor entityBreaker;
    private final InstanceRegistry instanceRegistry;
    private final PendingChangeRegistry pendingPersists;

    public ObjectOntologyMapperImpl(UnitOfWorkImpl uow, Connection connection) {
        this.uow = Objects.requireNonNull(uow);
        this.storageConnection = Objects.requireNonNull(connection);
        this.metamodel = uow.getMetamodel();
        this.descriptorFactory = new AxiomDescriptorFactory();
        this.instanceRegistry = new InstanceRegistry();
        this.pendingPersists = new PendingChangeRegistry();
        this.entityBuilder = new EntityConstructor(this);
        this.entityBreaker = new EntityDeconstructor(this);
    }

    @Override
    public <T> boolean containsEntity(Class<T> cls, URI primaryKey, Descriptor descriptor) {
        assert cls != null;
        assert primaryKey != null;
        assert descriptor != null;

        final EntityType<T> et = getEntityType(cls);
        final NamedResource classUri = NamedResource.create(et.getIRI().toURI());
        final Axiom<NamedResource> ax = new AxiomImpl<>(NamedResource.create(primaryKey),
                Assertion.createClassAssertion(false), new Value<>(classUri));
        try {
            return storageConnection.contains(ax, descriptor.getContext());
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    @Override
    public <T> T loadEntity(LoadingParameters<T> loadingParameters) {
        assert loadingParameters != null;

        instanceRegistry.reset();
        return loadEntityInternal(loadingParameters);
    }

    private <T> T loadEntityInternal(LoadingParameters<T> loadingParameters) {
        final EntityType<T> et = getEntityType(loadingParameters.getEntityType());
        final AxiomDescriptor axiomDescriptor = descriptorFactory.createForEntityLoading(loadingParameters, et);
        try {
            final Collection<Axiom<?>> axioms = storageConnection.find(axiomDescriptor);
            if (axioms.isEmpty()) {
                return null;
            }
            return entityBuilder
                    .reconstructEntity(loadingParameters.getIdentifier(), et, loadingParameters.getDescriptor(),
                            axioms);
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new EntityReconstructionException(e);
        }
    }

    @Override
    public <T> EntityType<T> getEntityType(Class<T> cls) {
        return metamodel.entity(cls);
    }

    @Override
    public <T> void loadFieldValue(T entity, Field field, Descriptor descriptor) {
        assert entity != null;
        assert field != null;
        assert descriptor != null;

        LOG.trace("Lazily loading value of field {} of entity ", field, entity);

        final EntityType<T> et = (EntityType<T>) getEntityType(entity.getClass());
        final URI primaryKey = EntityPropertiesUtils.getPrimaryKey(entity, et);

        final AxiomDescriptor axiomDescriptor = descriptorFactory.createForFieldLoading(primaryKey,
                field, descriptor, et);
        try {
            final Collection<Axiom<?>> axioms = storageConnection.find(axiomDescriptor);
            if (axioms.isEmpty()) {
                return;
            }
            entityBuilder.setFieldValue(entity, field, axioms, et, descriptor);
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new EntityReconstructionException(e);
        }
    }

    @Override
    public <T> void persistEntity(URI primaryKey, T entity, Descriptor descriptor) {
        assert entity != null;
        assert descriptor != null;

        @SuppressWarnings("unchecked")
        final EntityType<T> et = (EntityType<T>) getEntityType(entity.getClass());
        try {
            if (primaryKey == null) {
                primaryKey = generateIdentifier(et);
                assert primaryKey != null;
                EntityPropertiesUtils.setPrimaryKey(primaryKey, entity, et);
            }
            entityBreaker.setCascadeResolver(new PersistCascadeResolver(this));
            final AxiomValueGatherer axiomBuilder = entityBreaker.mapEntityToAxioms(primaryKey,
                    entity, et, descriptor);
            axiomBuilder.persist(storageConnection);
            pendingPersists.removeInstance(primaryKey, descriptor.getContext());
        } catch (IllegalArgumentException e) {
            throw new EntityDeconstructionException("Unable to deconstruct entity " + entity, e);
        }
    }

    @Override
    public URI generateIdentifier(EntityType<?> et) {
        try {
            return storageConnection.generateIdentifier(et.getIRI().toURI());
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    @Override
    public <T> T getEntityFromCacheOrOntology(Class<T> cls, URI primaryKey, Descriptor descriptor) {
        final T orig = uow.getManagedOriginal(cls, primaryKey, descriptor);
        if (orig != null) {
            return orig;
        }
        if (uow.getLiveObjectCache().contains(cls, primaryKey, descriptor.getContext())) {
            return uow.getLiveObjectCache().get(cls, primaryKey, descriptor.getContext());
        } else if (instanceRegistry.containsInstance(primaryKey, descriptor.getContext())) {
            // This prevents endless cycles in bidirectional relationships
            return cls.cast(instanceRegistry.getInstance(primaryKey, descriptor.getContext()));
        } else {
            return loadEntityInternal(new LoadingParameters<>(cls, primaryKey, descriptor));
        }
    }

    @Override
    public <T> T getOriginalInstance(T clone) {
        assert clone != null;
        return (T) uow.getOriginal(clone);
    }

    <T> void registerInstance(URI primaryKey, T instance, URI context) {
        instanceRegistry.registerInstance(primaryKey, instance, context);
    }

    @Override
    public void checkForUnpersistedChanges() {
        try {
            final Map<URI, Map<URI, Object>> persists = pendingPersists.getInstances();
            if (!persists.isEmpty()) {
                for (URI ctx : persists.keySet()) {
                    for (Entry<URI, Object> e : persists.get(ctx).entrySet()) {
                        verifyInstanceExistInOntology(ctx, e.getKey(), e.getValue());
                    }
                }
            }
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    private void verifyInstanceExistInOntology(URI ctx, URI primaryKey, Object instance)
            throws OntoDriverException {
        boolean exists = containsEntity(instance.getClass(), primaryKey, new EntityDescriptor(ctx));
        if (!exists) {
            throw new UnpersistedChangeException(
                    "Encountered an instance that was neither persisted nor marked as cascade for persist. The instance: "
                            + instance);
        }
    }

    <T> void registerPendingPersist(URI primaryKey, T entity, URI context) {
        pendingPersists.registerInstance(primaryKey, entity, context);
    }

    @Override
    public <T> void removeEntity(URI primaryKey, Class<T> cls, Descriptor descriptor) {
        final EntityType<T> et = getEntityType(cls);
        final AxiomDescriptor axiomDescriptor = descriptorFactory.createForEntityLoading(
                new LoadingParameters<>(cls, primaryKey, descriptor, true), et);
        try {
            storageConnection.remove(axiomDescriptor);
        } catch (OntoDriverException e) {
            throw new StorageAccessException("Exception caught when removing entity.", e);
        }
    }

    @Override
    public <T> void updateFieldValue(T entity, Field field, Descriptor descriptor) {
        @SuppressWarnings("unchecked")
        final EntityType<T> et = (EntityType<T>) getEntityType(entity.getClass());
        final URI pkUri = EntityPropertiesUtils.getPrimaryKey(entity, et);

        entityBreaker.setCascadeResolver(new PersistCascadeResolver(this));
        final AxiomValueGatherer axiomBuilder = entityBreaker.mapFieldToAxioms(pkUri, entity, field,
                et, descriptor);
        axiomBuilder.update(storageConnection);
    }

    @Override
    public Collection<Axiom<NamedResource>> loadSimpleList(SimpleListDescriptor listDescriptor) {
        try {
            return storageConnection.lists().loadSimpleList(listDescriptor);
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    @Override
    public Collection<Axiom<NamedResource>> loadReferencedList(ReferencedListDescriptor listDescriptor) {
        try {
            return storageConnection.lists().loadReferencedList(listDescriptor);
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    @Override
    public Configuration getConfiguration() {
        return uow.getConfiguration();
    }
}
